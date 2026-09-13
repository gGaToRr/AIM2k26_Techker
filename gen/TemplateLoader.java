package gen;

import nlp.TypeOfPrompt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Charge et met en cache les templates Markdown du dossier genPrompt pour les 7 Archétypes Universels
public class TemplateLoader {

    private static final Map<String, String> CACHE_TEMPLATES = new HashMap<>();

    // Charge un template à partir de l'Archétype et du sous-type
    public static String chargerTemplate(TypeOfPrompt type, String subType) {
        String dossier = switch (type) {
            case APPRENTISSAGE_TUTORIEL -> "learning";
            case CONCEPTION_ARCHITECTURE -> "architecture";
            case DEPANNAGE_DIAGNOSTIC -> "troubleshooting";
            case CREATION_REDACTION -> "creation";
            case PROTOCOLE_RECETTE -> "protocol";
            case COMPARAISON_DECISION -> "comparison";
            case CONCEPT_VULGARISATION -> "concept";
        };

        String cleCache = dossier + "/" + subType;
        if (CACHE_TEMPLATES.containsKey(cleCache)) {
            return CACHE_TEMPLATES.get(cleCache);
        }

        Path cheminFichier = Path.of("genPrompt", dossier, subType + ".md");

        try {
            if (Files.exists(cheminFichier)) {
                String contenu = Files.readString(cheminFichier);
                CACHE_TEMPLATES.put(cleCache, contenu);
                return contenu;
            }
        } catch (IOException ignored) {
            // En cas d'erreur de lecture, on bascule sur le fallback
        }

        return fallbackTemplate(type);
    }

    // Charge un template spécifique par son nom ou son chemin (utilisé pour le flag -t/--template)
    public static java.util.Optional<String> chargerTemplateParNom(String nom) {
        if (nom == null || nom.isBlank()) return java.util.Optional.empty();
        String clean = nom.trim().replace(".md", "");

        // 1. Recherche par chemin direct
        Path directPath = Path.of(nom.endsWith(".md") ? nom : nom + ".md");
        if (Files.exists(directPath) && Files.isRegularFile(directPath)) {
            try {
                return java.util.Optional.of(Files.readString(directPath));
            } catch (IOException ignored) {}
        }

        // 2. Recherche dans genPrompt/*/<clean>.md
        String[] dossiers = {"learning", "architecture", "troubleshooting", "creation", "protocol", "comparison", "concept"};
        for (String dossier : dossiers) {
            Path p = Path.of("genPrompt", dossier, clean + ".md");
            if (Files.exists(p)) {
                try {
                    return java.util.Optional.of(Files.readString(p));
                } catch (IOException ignored) {}
            }
        }

        // 3. Recherche par correspondance souple (ex: "feynman" -> "feynman_learning.md" ou "vulgarisation_feynman.md")
        for (String dossier : dossiers) {
            Path dir = Path.of("genPrompt", dossier);
            if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    for (Path file : stream.toList()) {
                        String fname = file.getFileName().toString().replace(".md", "");
                        if (fname.equalsIgnoreCase(clean) || fname.toLowerCase().contains(clean.toLowerCase())) {
                            return java.util.Optional.of(Files.readString(file));
                        }
                    }
                } catch (IOException ignored) {}
            }
        }

        return java.util.Optional.empty();
    }

    // Template de secours universel au cas où le fichier n'est pas trouvé
    private static String fallbackTemplate(TypeOfPrompt type) {
        return """
                # RÔLE & EXPERTISE
                {{#hasDomainExpertise}}
                {{domainPersona}}
                {{/hasDomainExpertise}}
                {{^hasDomainExpertise}}
                Tu es un Assistant IA Expert et Conseiller Stratégique de rang mondial.
                {{/hasDomainExpertise}}

                <contexte>
                - Archétype : {{type}}
                {{#hasDomainExpertise}}
                - Domaine : {{domainName}} (Sujet : {{domainTopic}})
                {{/hasDomainExpertise}}
                - Langue : {{language}}
                </contexte>

                <instruction_principale>
                {{cleanedMission}}
                </instruction_principale>

                {{#hasMultipleObjectives}}
                <objectifs_specifiques>
                {{#objectives}}
                - {{.}}
                {{/objectives}}
                </objectifs_specifiques>
                {{/hasMultipleObjectives}}

                <directives>
                - Répondre avec rigueur, précision, clarté et pédagogie.
                - Découper la réponse en étapes logiques et structurées.
                {{#hasAutoConstraints}}
                {{#autoConstraints}}
                - {{.}}
                {{/autoConstraints}}
                {{/hasAutoConstraints}}
                </directives>

                <format_de_sortie>
                - Rédiger la réponse en Markdown clair et soigné.
                </format_de_sortie>
                """;
    }
}
