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
