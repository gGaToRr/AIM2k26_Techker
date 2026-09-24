package llm;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Mise au meme format du prompt ameliore par un modele local.
//
// Chaque modele presente les sections a sa facon ("Role:", "**Rôle :**", "## Tâche",
// "Section : Role"...) et ajoute parfois un preambule ("## Prompt amélioré :"). Les
// sections sont reconnues quelle que soit leur presentation, puis rendues toujours de la
// meme maniere : cinq titres Markdown dans un ordre fixe, dans la langue du prompt.
public final class PromptStructure {

    public enum Section {
        ROLE("r[oô]le(?:\\s*(?:&|et|and)\\s*expertise)?|persona"),
        CONTEXTE("contexte|context"),
        TACHE("t[aâ]che|task|objectif|mission"),
        CONTRAINTES("contraintes?|constraints?|exigences|requirements"),
        FORMAT("format(?:\\s+de\\s+(?:sortie|r[ée]ponse))?(?:\\s+attendu)?|output\\s+format"
                + "|expected\\s+output(?:\\s+format)?|livrables?");

        private final String libelles;

        Section(String libelles) {
            this.libelles = libelles;
        }
    }

    // En dessous, la reponse n'est pas un prompt structure (mots-cles en vrac, reponse a
    // la tache...) : mieux vaut le repli NLP qu'un gabarit presque vide
    public static final int SECTIONS_MINIMUM = 3;

    // Une ligne de titre : "## Rôle", "**Tâche :**", "- Contexte :", "Section : Role"...
    // suivie eventuellement du debut du contenu sur la meme ligne
    private static final Pattern TITRE;

    static {
        StringBuilder alternatives = new StringBuilder();
        for (Section section : Section.values()) {
            if (alternatives.length() > 0) alternatives.append('|');
            alternatives.append("(?<").append(section.name()).append('>').append(section.libelles).append(')');
        }
        TITRE = Pattern.compile("^\\s*(?<diese>#{1,6}\\s*)?(?<puce>[-*]\\s+)?(?<gras>\\*\\*|__)?\\s*(?:section\\s*:?\\s*)?"
                + "(?:" + alternatives + ")\\s*(?:\\*\\*|__)?\\s*(?<deuxpoints>[:：])?\\s*(?:\\*\\*|__)?\\s*(?<suite>.*)$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static final Pattern PUCE = Pattern.compile("(?m)^(\\s*)[*•]\\s+");

    private PromptStructure() {}

    // Sections trouvees dans la reponse du modele, dans l'ordre fixe. Le texte avant la
    // premiere section (preambule) est ignore ; une section repetee est completee.
    public static Map<Section, String> extraire(String reponse) {
        Map<Section, StringBuilder> contenus = new EnumMap<>(Section.class);
        Section courante = null;
        for (String ligne : (reponse == null ? "" : reponse).split("\\R")) {
            Matcher m = TITRE.matcher(ligne);
            Section titre = m.matches() ? sectionDe(m) : null;
            // Sans deux-points ni mise en forme de titre, "Contexte technique ..." est du contenu.
            // Une puce n'est un titre qu'en gras ("* **Rôle :**") : "- Format : PDF" est une contrainte.
            boolean estTitre = titre != null && (m.group("puce") == null || m.group("gras") != null)
                    && (m.group("deuxpoints") != null || m.group("diese") != null
                    || m.group("gras") != null || m.group("suite").isBlank());
            if (estTitre) {
                courante = titre;
                StringBuilder contenu = contenus.computeIfAbsent(titre, s -> new StringBuilder());
                ajouterLigne(contenu, nettoyer(m.group("suite")));
            } else if (courante != null) {
                ajouterLigne(contenus.get(courante), ligne);
            }
        }
        Map<Section, String> sections = new EnumMap<>(Section.class);
        contenus.forEach((section, contenu) -> {
            String texte = normaliser(contenu.toString());
            if (section == Section.ROLE) texte = premierePhrase(texte);
            if (!texte.isEmpty()) sections.put(section, texte);
        });
        return sections;
    }

    // Contenu propre a l'exemple de la consigne (LocalLlmBackend.CONSIGNE_AMELIORATION) :
    // les petits modeles le recopient parfois dans un prompt qui n'a rien a voir
    private static final java.util.List<String> MARQUEURS_EXEMPLE = java.util.List.of(
            "mon poste", "ancienneté", "augmentation", "négociation salariale", "150 mots",
            "formule d'appel", "formule de politesse", "mes réalisations");

    // Part minimale des mots-cles du prompt brut a retrouver dans le prompt ameliore :
    // en dessous, le modele a change de sujet
    public static final double COUVERTURE_MINIMALE = 0.3;

    public static Optional<String> structurer(String reponse, String langue) {
        return structurer(reponse, langue, "");
    }

    // Prompt final au format commun, ou vide si la reponse n'est pas assez structuree.
    // promptBrut : les lignes empruntees a l'exemple n'y figurant pas sont retirees.
    public static Optional<String> structurer(String reponse, String langue, String promptBrut) {
        Map<Section, String> sections = extraire(reponse);
        String brut = promptBrut == null ? "" : promptBrut.toLowerCase(Locale.ROOT);
        sections.replaceAll((section, texte) -> retirerEmpruntsExemple(texte, brut));
        sections.values().removeIf(String::isEmpty);
        if (sections.size() < SECTIONS_MINIMUM) return Optional.empty();
        return Optional.of(rendre(sections, langue));
    }

    // Le prompt ameliore parle-t-il encore du sujet ? Accents et casse ignores ; les mots
    // tres courts ("pc", "web") sont trop ambigus pour compter.
    public static boolean resteSurLeSujet(String prompt, java.util.List<String> motsCles) {
        java.util.List<String> retenus = motsCles.stream().map(PromptStructure::sansAccents)
                .filter(mot -> mot.length() >= 4).distinct().toList();
        if (retenus.size() < 3) return true;
        String texte = sansAccents(prompt);
        long trouves = retenus.stream().filter(texte::contains).count();
        return (double) trouves / retenus.size() >= COUVERTURE_MINIMALE;
    }

    private static String sansAccents(String texte) {
        return java.text.Normalizer.normalize(texte == null ? "" : texte, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String retirerEmpruntsExemple(String texte, String promptBrut) {
        StringBuilder garde = new StringBuilder();
        for (String ligne : texte.split("\n")) {
            String minuscule = ligne.toLowerCase(Locale.ROOT);
            boolean emprunt = MARQUEURS_EXEMPLE.stream()
                    .anyMatch(marqueur -> minuscule.contains(marqueur) && !promptBrut.contains(marqueur));
            if (emprunt) continue;
            if (garde.length() > 0) garde.append('\n');
            garde.append(ligne);
        }
        return garde.toString().strip();
    }

    // Les sections absentes restent visibles, a completer : le prompt garde toujours la
    // meme forme, quel que soit le modele
    public static String rendre(Map<Section, String> sections, String langue) {
        String[] titres = titres(langue);
        StringBuilder sb = new StringBuilder();
        for (Section section : Section.values()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("## ").append(titres[section.ordinal()]).append('\n');
            sb.append(sections.getOrDefault(section, aCompleter(langue)));
        }
        return sb.toString();
    }

    private static Section sectionDe(Matcher m) {
        for (Section section : Section.values()) {
            if (m.group(section.name()) != null) return section;
        }
        return null;
    }

    private static void ajouterLigne(StringBuilder contenu, String ligne) {
        if (contenu.length() > 0) contenu.append('\n');
        contenu.append(ligne);
    }

    // Reste de mise en forme autour d'un titre en ligne ("**Rôle :** Tu es...")
    private static String nettoyer(String suite) {
        return suite.replaceAll("^(?:\\*\\*|__)\\s*", "").strip();
    }

    // Puces uniformes (-), sans doublon, pas de lignes vides en trop, pas de gras orphelin
    private static String normaliser(String texte) {
        String propre = PUCE.matcher(texte).replaceAll("$1- ");
        propre = propre.replaceAll("(?m)^(\\s*)-\\s{2,}", "$1- ");
        java.util.Set<String> puces = new java.util.HashSet<>();
        StringBuilder sansDoublon = new StringBuilder();
        for (String ligne : propre.split("\n", -1)) {
            boolean estPuce = ligne.stripLeading().startsWith("- ");
            if (estPuce && !puces.add(ligne.strip().toLowerCase(Locale.ROOT))) continue;
            if (sansDoublon.length() > 0) sansDoublon.append('\n');
            sansDoublon.append(ligne.stripTrailing());
        }
        propre = sansDoublon.toString().replaceAll("\\n{3,}", "\n\n").strip();
        return propre.equals("**") || propre.equals("__") ? "" : propre;
    }

    // Le role tient en une phrase ("Tu es un expert en...") : les modeles de raisonnement
    // y deroulent parfois toute la solution
    private static String premierePhrase(String texte) {
        Matcher fin = Pattern.compile("[.!?](\\s|$)").matcher(texte);
        return fin.find() ? texte.substring(0, fin.start() + 1).strip() : texte;
    }

    // Titres dans la langue du prompt : code de l'option -l, ou langue detectee (FR, EN)
    static String[] titres(String langue) {
        return switch (langue == null ? "" : langue.trim().toLowerCase(Locale.ROOT)) {
            case "en", "english", "anglais" ->
                    new String[] {"Role", "Context", "Task", "Constraints", "Expected output format"};
            case "es", "espagnol", "spanish" ->
                    new String[] {"Rol", "Contexto", "Tarea", "Restricciones", "Formato de salida esperado"};
            case "de", "allemand", "deutsch", "german" ->
                    new String[] {"Rolle", "Kontext", "Aufgabe", "Einschränkungen", "Erwartetes Ausgabeformat"};
            default -> new String[] {"Rôle", "Contexte", "Tâche", "Contraintes", "Format de sortie attendu"};
        };
    }

    private static String aCompleter(String langue) {
        return switch (langue == null ? "" : langue.trim().toLowerCase(Locale.ROOT)) {
            case "en", "english", "anglais" -> "[to be completed]";
            case "es", "espagnol", "spanish" -> "[por completar]";
            case "de", "allemand", "deutsch", "german" -> "[zu ergänzen]";
            default -> "[à compléter]";
        };
    }
}
