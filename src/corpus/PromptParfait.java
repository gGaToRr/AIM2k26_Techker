package corpus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// Le prompt "parfait" : le vrai prompt de la base le plus proche de celui de l'utilisateur,
// qui sert de reference au modele local pour l'ameliorer.
//
//   prompt de l'utilisateur
//     -> NLP : langue, themes proches, type de demande (aiguillage)
//     -> base : seuls les fichiers de ces themes (corpus/themes/<langue>/<theme>.json)
//     -> classement selon la richesse lexicale du prompt (RechercheThematique)
//     -> le premier qui contient la moitie des mots precis de l'utilisateur (3 au plus)
//
// Sans base installee (java Main --corpus-install), aucune reference : l'amelioration se
// fait sans, comme avant.
public final class PromptParfait {

    // Une reference doit etre un prompt lisible par un petit modele : ni une ligne de trois
    // mots, ni 7 000 caracteres de code colle
    static final int LONGUEUR_MIN = 40;
    static final int LONGUEUR_MAX = 1500;
    // Mots precis de l'utilisateur que la reference doit contenir : la moitie, plafonnee a
    // MOTS_COMMUNS_MAX. Un prompt riche (8 mots precis) a rarement un jumeau qui en reprend 4 ;
    // 3 mots communs suffisent a designer le meme sujet.
    static final double COUVERTURE_MIN = 0.5;
    static final int MOTS_COMMUNS_MAX = 3;

    // Nombre de mots communs exige selon la richesse lexicale du prompt de l'utilisateur
    public static int motsCommunsExiges(int richesse) {
        return Math.max(1, Math.min(MOTS_COMMUNS_MAX, (int) Math.ceil(COUVERTURE_MIN * richesse)));
    }
    // Candidats examines, dans l'ordre du classement
    static final int CANDIDATS = 20;

    public record Reference(ConstruireIndex.Entree entree, List<String> themes, int richesse, double couverture,
                            List<RechercheProgressive.Etape> etapes, long millisecondes) {
        public String texte() {
            return entree.prompt().texte();
        }
    }

    private static volatile RechercheThematique recherche;

    private PromptParfait() {}

    // Base presente et non desactivee (les tests la desactivent : -Daim.corpus.desactive=true)
    public static boolean disponible() {
        return !Boolean.getBoolean("aim.corpus.desactive")
                && Files.isRegularFile(Corpus.DOSSIER.resolve(ConstruireIndex.SOMMAIRE));
    }

    public static Optional<Reference> trouver(String promptUtilisateur) {
        if (!disponible()) return Optional.empty();
        if (recherche == null) {
            synchronized (PromptParfait.class) {
                if (recherche == null) recherche = new RechercheThematique(Corpus.DOSSIER);
            }
        }
        return trouver(promptUtilisateur, recherche);
    }

    public static Optional<Reference> trouver(String promptUtilisateur, RechercheThematique recherche) {
        RechercheThematique.Resultat r = recherche.rechercher(promptUtilisateur, CANDIDATS, e -> {
            int longueur = e.prompt().texte().length();
            return longueur >= LONGUEUR_MIN && longueur <= LONGUEUR_MAX;
        });
        int exiges = motsCommunsExiges(r.richesse());
        for (int k = 0; k < r.meilleurs().size(); k++) {
            if (Math.round(r.couvertures().get(k) * r.richesse()) >= exiges) {
                return Optional.of(new Reference(r.meilleurs().get(k), r.themes(), r.richesse(), r.couvertures().get(k),
                        r.etapes(), r.millisecondes()));
            }
        }
        return Optional.empty();
    }

    // Pour les tests : base situee ailleurs
    public static Optional<Reference> trouverDans(Path dossier, String promptUtilisateur) {
        return trouver(promptUtilisateur, new RechercheThematique(dossier));
    }
}
