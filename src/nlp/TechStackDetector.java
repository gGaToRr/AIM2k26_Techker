package nlp;

import java.util.*;
import java.util.regex.Pattern;

// Détecte les langages de dev, frameworks, moteurs de jeu et langues de traduction
public class TechStackDetector {

    // Liste des langages et frameworks avec détection contextuelle
    private static final Map<String, List<String>> LANGAGES_DEV = Map.ofEntries(
            Map.entry("HTML / CSS / UI Design", List.of("css", "html", "tailwind", "sass", "bootstrap", "frontend", "ui", "ux", "responsive", "site web", "page web", "web design", "design du site", "redesign")),
            Map.entry("Unity / Game Dev", List.of("unity", "unreal", "unreal engine", "godot", "sfml", "raylib", "game engine", "moteur de jeu")),
            Map.entry("C++", List.of("c++", "cpp", "cmake", "gcc", "clang", "g++")),
            Map.entry("C#", List.of("c#", "csharp", ".net", "dotnet")),
            Map.entry("C", List.of("langage c", "norme c", "pointeur")),
            Map.entry("Java", List.of("java", "jvm", "spring", "spring boot", "maven", "gradle", "hibernate", "quarkus")),
            Map.entry("Python", List.of("python", "django", "flask", "fastapi", "pandas", "numpy", "pytorch", "tensorflow")),
            Map.entry("JavaScript / TypeScript", List.of("javascript", "typescript", "js", "ts", "node", "nodejs", "react", "vue", "angular", "nextjs", "express")),
            Map.entry("Rust", List.of("rust", "cargo", "tokio", "actix")),
            Map.entry("Go", List.of("golang", "goroutine", "gin")),
            Map.entry("SQL", List.of("sql", "postgresql", "postgres", "mysql", "mongodb", "redis", "sqlite")),
            Map.entry("DevOps / Cloud", List.of("docker", "kubernetes", "k8s", "aws", "gcp", "azure", "terraform"))
    );

    // Mots-cles qui sont aussi des mots courants ou d'autres usages : "unreal engine" comme
    // style d'image (prompts Midjourney), "react" et "express" verbes anglais, "spring"
    // printemps, "rust" rouille, "vue" en francais... Comptes seulement dans un contexte de
    // developpement. Sur 470 000 vrais prompts, "unreal" seul classait 10 % d'entre eux en Game Dev.
    static final Set<String> MOTS_AMBIGUS = Set.of(
            "unity", "unreal", "unreal engine", "react", "express", "vue", "angular", "node", "ts", "spring",
            "rust", "cargo", "gin", "azure", "responsive", "ui", "ux");

    static final Pattern CONTEXTE_DEV = Pattern.compile("(?<![a-z0-9])(code|coder|coding|script|scripts|programm\\w*"
            + "|develop\\w*|developp\\w*|function|fonction|api|bug|debug\\w*|error|erreur|framework|compil\\w*|library"
            + "|librairie|biblioth\\w*|app|application|component|composant|server|serveur|backend|frontend|database"
            + "|git|npm|deploy\\w*|logiciel|software|blueprint|class|classe|import|gamedev|game dev\\w*|jeu video"
            + "|css|html|javascript|typescript|python|java|c\\+\\+|c#|sql|site web|website|web app|webapp)(?![a-z0-9])"
            + "|[{}]|=>|\\(\\);");

    // Motif de chaque mot-cle, compile une fois : l'analyse passe sur chaque prompt
    private static final Map<String, Pattern> MOTIFS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Pattern motif(String motCle) {
        return MOTIFS.computeIfAbsent(motCle,
                kw -> Pattern.compile("(?i)(?<![a-zA-Z0-9])" + Pattern.quote(kw) + "(?![a-zA-Z0-9])"));
    }

    // Langues pour la traduction
    private static final Map<String, String> LANGUES_TRADUCTION = Map.ofEntries(
            Map.entry("anglais", "Anglais (EN)"),
            Map.entry("english", "Anglais (EN)"),
            Map.entry("francais", "Français (FR)"),
            Map.entry("french", "Français (FR)"),
            Map.entry("espagnol", "Espagnol (ES)"),
            Map.entry("spanish", "Espagnol (ES)"),
            Map.entry("allemand", "Allemand (DE)"),
            Map.entry("german", "Allemand (DE)"),
            Map.entry("italien", "Italien (IT)"),
            Map.entry("italian", "Italien (IT)"),
            Map.entry("japonais", "Japonais (JA)"),
            Map.entry("chinois", "Chinois (ZH)")
    );

    // Contextes académiques ou spécialisés
    private static final Map<String, String> CONTEXTES_SPECIAUX = Map.of(
            "epitech", "Projet Académique Epitech (Exigence Clean Code & Architecture)",
            "42", "Projet Académique École 42 (Norme stricte)",
            "etudiant", "Projet Étudiant (Pédagogie & Bonnes pratiques)",
            "universite", "Projet Universitaire & Académique",
            "these", "Projet Universitaire de Recherche & Thèse",
            "doctorat", "Projet Universitaire de Recherche & Thèse"
    );

    // Repère les technos mentionnées dans le texte avec gestion précise des symboles (C++, C#)
    public static List<String> detecterTechnologies(String text) {
        if (text == null || text.isBlank()) return List.of();

        String lower = " " + text.toLowerCase().replace("'", " ").replace("’", " ") + " ";
        Set<String> technologiesTrouvees = new LinkedHashSet<>();

        boolean contexteDev = CONTEXTE_DEV.matcher(Sanitzer.supprimerAccents(lower)).find();
        for (Map.Entry<String, List<String>> entry : LANGAGES_DEV.entrySet()) {
            String techNom = entry.getKey();
            for (String keyword : entry.getValue()) {
                if (MOTS_AMBIGUS.contains(keyword) && !contexteDev) continue;
                if (motif(keyword).matcher(lower).find()) {
                    technologiesTrouvees.add(techNom);
                    break;
                }
            }
        }

        return new ArrayList<>(technologiesTrouvees);
    }

    // Détecte si un contexte spécial (école, type de projet) est mentionné
    public static Optional<String> detecterContexteSpecial(String text) {
        if (text == null || text.isBlank()) return Optional.empty();

        String lower = text.toLowerCase();
        for (Map.Entry<String, String> entry : CONTEXTES_SPECIAUX.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    // Repère si une langue cible est demandée (ex: "en anglais", "vers l'espagnol", "vers le japonais")
    public static Optional<String> detecterLangueCibleTraduction(String text) {
        if (text == null || text.isBlank()) return Optional.empty();

        String lower = " " + text.toLowerCase().replace("'", " ").replace("’", " ") + " ";
        for (Map.Entry<String, String> entry : LANGUES_TRADUCTION.entrySet()) {
            String l = entry.getKey();
            if (lower.contains(" en " + l) || lower.contains(" vers " + l) || lower.contains(" vers l " + l) || lower.contains(" vers le " + l) || lower.contains(" vers la " + l) || lower.contains(" in " + l) || lower.contains(" to " + l) || lower.contains(" into " + l)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
