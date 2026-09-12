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
            "etudiant", "Projet Étudiant (Pédagogie & Bonnes pratiques)"
    );

    // Repère les technos mentionnées dans le texte avec gestion précise des symboles (C++, C#)
    public static List<String> detecterTechnologies(String text) {
        if (text == null || text.isBlank()) return List.of();

        String lower = " " + text.toLowerCase().replace("'", " ").replace("’", " ") + " ";
        Set<String> technologiesTrouvees = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : LANGAGES_DEV.entrySet()) {
            String techNom = entry.getKey();
            for (String keyword : entry.getValue()) {
                String regex = "(?i)(?<![a-zA-Z0-9])" + Pattern.quote(keyword) + "(?![a-zA-Z0-9])";
                Pattern p = Pattern.compile(regex);
                if (p.matcher(lower).find()) {
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

    // Repère si une langue cible est demandée (ex: "en anglais")
    public static Optional<String> detecterLangueCibleTraduction(String text) {
        if (text == null || text.isBlank()) return Optional.empty();

        String lower = text.toLowerCase();
        for (Map.Entry<String, String> entry : LANGUES_TRADUCTION.entrySet()) {
            if (lower.contains("en " + entry.getKey()) || lower.contains("vers " + entry.getKey()) || lower.contains("in " + entry.getKey()) || lower.contains("to " + entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
