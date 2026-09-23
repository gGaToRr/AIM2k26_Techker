package gen;

import nlp.PromptProfile;
import nlp.TypeOfPrompt;
import util.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

// Bibliotheque d'exemples calibres (few-shot) injectes dans les meta-prompts.
//
// Les petits modeles locaux suivent bien mieux un format qu'on leur montre qu'un
// format qu'on leur decrit. Un exemple pertinent vaut plusieurs lignes de consigne ;
// un exemple hors sujet, lui, degrade la reponse. D'ou une selection par pertinence
// plutot qu'un bloc fixe.
public final class FewShotLibrary {

    public static final String DOSSIER = "fewshots";

    // Budget de contexte : les modeles vises tiennent dans quelques milliers de tokens.
    // Au-dela, les exemples chassent la vraie demande hors de la fenetre.
    public static final int MAX_EXEMPLES = 2;
    public static final int MAX_CARACTERES = 2400;

    private static final Set<String> MOTS_VIDES = Set.of(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "a", "au", "aux",
            "en", "dans", "sur", "pour", "par", "avec", "sans", "que", "qui", "quoi", "ce",
            "cette", "ces", "je", "tu", "il", "elle", "on", "nous", "vous", "ils", "mon",
            "ma", "mes", "son", "sa", "ses", "est", "sont", "etre", "avoir", "fait", "faire",
            "plus", "moins", "tres", "the", "and", "for", "with", "this", "that", "from");

    // Un exemple : la demande type et la reponse ideale correspondante
    public record Exemple(String entree, String sortie) {

        public String rendu() {
            return "<exemple>\n<demande>\n" + entree.trim() + "\n</demande>\n"
                    + "<reponse_ideale>\n" + sortie.trim() + "\n</reponse_ideale>\n</exemple>";
        }

        public int taille() {
            return entree.length() + sortie.length();
        }
    }

    // Les marqueurs sont ancres en debut de ligne : un fichier qui les cite dans
    // son en-tete explicative ne doit pas etre lu comme un exemple.
    private static final java.util.regex.Pattern SEPARATEUR_EXEMPLE =
            java.util.regex.Pattern.compile("(?m)^##\\s+EXEMPLE\\s*$");
    private static final java.util.regex.Pattern MARQUEUR_DEMANDE =
            java.util.regex.Pattern.compile("(?m)^###\\s+DEMANDE\\s*$");
    private static final java.util.regex.Pattern MARQUEUR_REPONSE =
            java.util.regex.Pattern.compile("(?m)^###\\s+REPONSE\\s*$");

    private FewShotLibrary() {}

    // Bloc pret a injecter dans le template, ou chaine vide si aucun exemple pertinent
    public static String selectionner(PromptProfile profile, TypeOfPrompt type, String sousType) {
        List<Exemple> disponibles = chargerExemples(type, sousType);
        if (disponibles.isEmpty()) {
            return "";
        }

        List<Exemple> retenus = classerParPertinence(disponibles, profile.rawText());
        StringBuilder bloc = new StringBuilder();
        int cumul = 0;
        int poses = 0;

        for (Exemple exemple : retenus) {
            if (poses >= MAX_EXEMPLES || cumul + exemple.taille() > MAX_CARACTERES) {
                break;
            }
            if (poses > 0) {
                bloc.append('\n');
            }
            bloc.append(exemple.rendu());
            cumul += exemple.taille();
            poses++;
        }
        return bloc.toString();
    }

    // Charge les exemples du sous-type, a defaut ceux de l'archetype
    public static List<Exemple> chargerExemples(TypeOfPrompt type, String sousType) {
        if (sousType != null && !sousType.isBlank()) {
            List<Exemple> duSousType = lireFichier(DOSSIER + "/" + sousType + ".md");
            if (!duSousType.isEmpty()) {
                return duSousType;
            }
        }
        return lireFichier(DOSSIER + "/" + type.name().toLowerCase(Locale.ROOT) + ".md");
    }

    private static List<Exemple> lireFichier(String chemin) {
        Optional<Path> resolu = TemplateLoader.resoudreCheminTemplate(chemin);
        if (resolu.isEmpty()) {
            return List.of();
        }
        try {
            return analyser(Files.readString(resolu.get()));
        } catch (Exception e) {
            Log.exceptionIgnoree("Lecture des exemples few-shot " + chemin, e);
            return List.of();
        }
    }

    // Format attendu, volontairement lisible a la main :
    //   ## EXEMPLE
    //   ### DEMANDE
    //   ...
    //   ### REPONSE
    //   ...
    public static List<Exemple> analyser(String contenu) {
        List<Exemple> exemples = new ArrayList<>();
        if (contenu == null || contenu.isBlank()) {
            return exemples;
        }

        for (String bloc : SEPARATEUR_EXEMPLE.split(contenu)) {
            java.util.regex.Matcher demande = MARQUEUR_DEMANDE.matcher(bloc);
            java.util.regex.Matcher reponse = MARQUEUR_REPONSE.matcher(bloc);
            if (!demande.find() || !reponse.find(demande.end())) {
                continue;
            }
            String entree = bloc.substring(demande.end(), reponse.start()).trim();
            String sortie = bloc.substring(reponse.end()).trim();
            if (!entree.isEmpty() && !sortie.isEmpty()) {
                exemples.add(new Exemple(entree, sortie));
            }
        }
        return exemples;
    }

    // Trie par recouvrement lexical avec la demande de l'utilisateur.
    // Le tri est stable : a score egal, l'ordre du fichier fait foi, ce qui rend
    // la selection reproductible.
    public static List<Exemple> classerParPertinence(List<Exemple> exemples, String demande) {
        Set<String> motsDemande = motsSignifiants(demande);
        List<Exemple> copie = new ArrayList<>(exemples);
        copie.sort((a, b) -> Double.compare(score(b, motsDemande), score(a, motsDemande)));
        return copie;
    }

    public static double score(Exemple exemple, Set<String> motsDemande) {
        if (motsDemande.isEmpty()) {
            return 0.0;
        }
        Set<String> motsExemple = motsSignifiants(exemple.entree());
        if (motsExemple.isEmpty()) {
            return 0.0;
        }
        long communs = motsExemple.stream().filter(motsDemande::contains).count();
        // Normalise par l'union : un exemple tres long ne doit pas gagner par accumulation
        Set<String> union = new HashSet<>(motsExemple);
        union.addAll(motsDemande);
        return (double) communs / union.size();
    }

    private static Set<String> motsSignifiants(String texte) {
        Set<String> mots = new HashSet<>();
        if (texte == null) {
            return mots;
        }
        for (String brut : texte.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            String mot = normaliser(brut);
            if (mot.length() >= 3 && !MOTS_VIDES.contains(mot)) {
                mots.add(mot);
            }
        }
        return mots;
    }

    private static String normaliser(String mot) {
        return java.text.Normalizer.normalize(mot, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
