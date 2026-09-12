package gen;

import nlp.TypeOfPrompt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Charge et met en cache les templates Markdown du dossier genPrompt
public class TemplateLoader {

    private static final Map<String, String> CACHE_TEMPLATES = new HashMap<>();

    // Charge un template à partir de la catégorie et du nom de fichier
    public static String chargerTemplate(TypeOfPrompt type, String subType) {
        String dossier = switch (type) {
            case CODE -> "code";
            case TRANSLATE -> "translate";
            case CORRECTANSWERS -> "correction";
            case CREATION -> "creation";
            case FACTUALQUESTIONS -> "questions";
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
                Tu es un assistant IA expert de haut niveau.

                <contexte>
                - Catégorie : {{type}}
                - Langue : {{language}}
                </contexte>

                <instruction_principale>
                {{rawPrompt}}
                </instruction_principale>

                <directives>
                - Répondre avec rigueur, précision et clarté.
                - Découper la réponse en étapes logiques.
                {{#hasAutoConstraints}}
                {{#autoConstraints}}
                - {{.}}
                {{/autoConstraints}}
                {{/hasAutoConstraints}}
                </directives>

                <format_de_sortie>
                - Rédiger la réponse en Markdown clair et structuré.
                </format_de_sortie>
                """;
    }
}
