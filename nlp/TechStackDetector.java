package nlp;

import java.util.*;
import java.util.regex.Pattern;

// Détecte les langages de dev, frameworks, moteurs de jeu et langues de traduction
public class TechStackDetector {

    // Liste des langages et frameworks avec détection contextuelle.
    // LinkedHashMap (et non Map.of/Map.ofEntries) afin de garantir un ordre d'itération stable et
    // déterministe entre deux exécutions : Map.of randomise son ordre d'itération à chaque lancement de JVM,
    // ce qui rendait l'ordre de la liste de technologies détectées imprévisible d'une exécution à l'autre.
    private static final Map<String, List<String>> LANGAGES_DEV = new LinkedHashMap<>();

    // Langues pour la traduction (ordre stable : le premier match trouvé dans le texte doit être déterministe)
    private static final Map<String, String> LANGUES_TRADUCTION = new LinkedHashMap<>();

    // Contextes académiques ou spécialisés (ordre stable pour la même raison)
    private static final Map<String, String> CONTEXTES_SPECIAUX = new LinkedHashMap<>();

    // Patterns précompilés par technologie, construits une seule fois au chargement de la classe
    // (évite de recompiler l'ensemble des regex à chaque appel de detecterTechnologies)
    private static final Map<String, List<Pattern>> LANGAGES_DEV_PATTERNS;

    static {
        LANGAGES_DEV.put("HTML / CSS / UI Design", List.of("css", "html", "tailwind", "sass", "bootstrap", "frontend", "ui", "ux", "responsive", "site web", "page web", "web design", "design du site", "redesign"));
        LANGAGES_DEV.put("Unity / Game Dev", List.of("unity", "unreal", "unreal engine", "godot", "sfml", "raylib", "game engine", "moteur de jeu"));
        LANGAGES_DEV.put("C++", List.of("c++", "cpp", "cmake", "gcc", "clang", "g++"));
        LANGAGES_DEV.put("C#", List.of("c#", "csharp", ".net", "dotnet"));
        LANGAGES_DEV.put("C", List.of("langage c", "norme c", "pointeur"));
        LANGAGES_DEV.put("Java", List.of("java", "jvm", "spring", "spring boot", "maven", "gradle", "hibernate", "quarkus"));
        LANGAGES_DEV.put("Python", List.of("python", "django", "flask", "fastapi", "pandas", "numpy", "pytorch", "tensorflow"));
        LANGAGES_DEV.put("JavaScript / TypeScript", List.of("javascript", "typescript", "js", "ts", "node", "nodejs", "react", "vue", "angular", "nextjs", "express"));
        LANGAGES_DEV.put("Rust", List.of("rust", "cargo", "tokio", "actix"));
        LANGAGES_DEV.put("Go", List.of("golang", "goroutine", "gin"));
        LANGAGES_DEV.put("SQL", List.of("sql", "postgresql", "postgres", "mysql", "mongodb", "redis", "sqlite"));
        LANGAGES_DEV.put("DevOps / Cloud", List.of("docker", "kubernetes", "k8s", "aws", "gcp", "azure", "terraform"));

        LANGUES_TRADUCTION.put("anglais", "Anglais (EN)");
        LANGUES_TRADUCTION.put("english", "Anglais (EN)");
        LANGUES_TRADUCTION.put("francais", "Français (FR)");
        LANGUES_TRADUCTION.put("french", "Français (FR)");
        LANGUES_TRADUCTION.put("espagnol", "Espagnol (ES)");
        LANGUES_TRADUCTION.put("spanish", "Espagnol (ES)");
        LANGUES_TRADUCTION.put("allemand", "Allemand (DE)");
        LANGUES_TRADUCTION.put("german", "Allemand (DE)");
        LANGUES_TRADUCTION.put("italien", "Italien (IT)");
        LANGUES_TRADUCTION.put("italian", "Italien (IT)");
        LANGUES_TRADUCTION.put("japonais", "Japonais (JA)");
        LANGUES_TRADUCTION.put("chinois", "Chinois (ZH)");

        CONTEXTES_SPECIAUX.put("epitech", "Projet Académique Epitech (Exigence Clean Code & Architecture)");
        CONTEXTES_SPECIAUX.put("42", "Projet Académique École 42 (Norme stricte)");
        CONTEXTES_SPECIAUX.put("etudiant", "Projet Étudiant (Pédagogie & Bonnes pratiques)");
        CONTEXTES_SPECIAUX.put("universite", "Projet Universitaire & Académique");
        CONTEXTES_SPECIAUX.put("these", "Projet Universitaire de Recherche & Thèse");
        CONTEXTES_SPECIAUX.put("doctorat", "Projet Universitaire de Recherche & Thèse");

        Map<String, List<Pattern>> compiled = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : LANGAGES_DEV.entrySet()) {
            List<Pattern> patterns = new ArrayList<>(entry.getValue().size());
            for (String keyword : entry.getValue()) {
                patterns.add(Pattern.compile("(?i)(?<![a-zA-Z0-9])" + Pattern.quote(keyword) + "(?![a-zA-Z0-9])"));
            }
            compiled.put(entry.getKey(), Collections.unmodifiableList(patterns));
        }
        LANGAGES_DEV_PATTERNS = Collections.unmodifiableMap(compiled);
    }

    // Repère les technos mentionnées dans le texte avec gestion précise des symboles (C++, C#)
    public static List<String> detecterTechnologies(String text) {
        if (text == null || text.isBlank()) return List.of();

        String lower = " " + text.toLowerCase().replace("'", " ").replace("’", " ") + " ";
        Set<String> technologiesTrouvees = new LinkedHashSet<>();

        for (Map.Entry<String, List<Pattern>> entry : LANGAGES_DEV_PATTERNS.entrySet()) {
            for (Pattern p : entry.getValue()) {
                if (p.matcher(lower).find()) {
                    technologiesTrouvees.add(entry.getKey());
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
