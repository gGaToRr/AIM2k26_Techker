package gen;

import com.samskivert.mustache.Mustache;
import nlp.PromptProfile;
import nlp.TechStackDetector;
import nlp.TypeOfPrompt;

import java.util.*;

// Moteur Meta-Prompting d'élite appliquant les 5 règles d'ingénierie de prompt
public class MetaPromptEngine {

    // Génère le super prompt optimisé en Markdown
    public static String genererPromptOptimise(PromptProfile profile) {
        TypeOfPrompt type = profile.classification().primaryType();
        String subType = determinerSousType(profile, type);

        // 1. Chargement du template Markdown
        String rawTemplate = TemplateLoader.chargerTemplate(type, subType);

        // 2. Construction du contexte pour le moteur Mustache
        Map<String, Object> context = construireContexteMustache(profile, type);

        // 3. Rendu ultra-rapide avec JMustache SANS échappement HTML
        return Mustache.compiler()
                .escapeHTML(false)
                .emptyStringIsFalse(true)
                .zeroIsFalse(true)
                .compile(rawTemplate)
                .execute(context)
                .trim();
    }

    // Détermine le sous-template le plus adapté à l'intention fine
    private static String determinerSousType(PromptProfile profile, TypeOfPrompt type) {
        String lower = profile.rawText().toLowerCase();

        return switch (type) {
            case CODE -> {
                if (lower.contains("architecture") || lower.contains("arborescence") || lower.contains("structure de dossier") || lower.contains("structure des dossier") || lower.contains("organisation des fichier")) {
                    yield "architecture";
                } else if (lower.contains("debug") || lower.contains("bug") || lower.contains("erreur") || lower.contains("exception") || lower.contains("crash")) {
                    yield "debug";
                } else if (lower.contains("review") || lower.contains("audit") || lower.contains("securite")) {
                    yield "review";
                } else if (lower.contains("refactor") || lower.contains("amelior") || lower.contains("optimis") || lower.contains("clean")) {
                    yield "refactor";
                }
                yield "generation";
            }
            case TRANSLATE -> "technical";
            case CORRECTANSWERS -> lower.contains("reformul") ? "rewrite" : "proofreading";
            case CREATION -> lower.contains("brainstorm") || lower.contains("idee") ? "brainstorming" : "storytelling";
            case FACTUALQUESTIONS -> (!profile.detectedTechnologies().isEmpty() || profile.hasCode()) ? "deep_technical" : "feynman";
        };
    }

    // Prépare les variables injectées (application des 5 règles d'optimisation)
    private static Map<String, Object> construireContexteMustache(PromptProfile profile, TypeOfPrompt type) {
        Map<String, Object> ctx = new HashMap<>();

        String cleanMission = extraireMissionPure(profile.rawText());

        ctx.put("rawPrompt", profile.rawText().trim());
        ctx.put("cleanedMission", cleanMission);
        ctx.put("sanitizedPrompt", profile.sanitizedText());
        ctx.put("type", type.name());
        ctx.put("language", profile.language().equals("FR") ? "Français" : "English");
        ctx.put("confidence", profile.classification().confidenceLevel().name() + " (" + String.format("%.1f%%", profile.classification().primaryProbability()) + ")");

        // Règle 2 : Injection de stack & persona
        boolean hasTech = !profile.detectedTechnologies().isEmpty();
        ctx.put("hasTechStack", hasTech);
        ctx.put("techStackSummary", String.join(", ", profile.detectedTechnologies()));

        // Contexte académique / spécifique
        Optional<String> specialCtx = TechStackDetector.detecterContexteSpecial(profile.rawText());
        specialCtx.ifPresentOrElse(
                c -> {
                    ctx.put("hasAcademicContext", true);
                    ctx.put("academicContext", c);
                },
                () -> ctx.put("hasAcademicContext", false)
        );

        // Langue cible pour la traduction
        profile.targetTranslationLanguage().ifPresentOrElse(
                target -> {
                    ctx.put("hasTargetLanguage", true);
                    ctx.put("targetLanguage", target);
                },
                () -> ctx.put("hasTargetLanguage", false)
        );

        // Règle 5 : Auto-correction des faiblesses du prompt
        List<String> autoConstraints = genererContraintesAutomatiques(profile);
        ctx.put("hasAutoConstraints", !autoConstraints.isEmpty());
        ctx.put("autoConstraints", autoConstraints);

        return ctx;
    }

    // Nettoie le bruit conversationnel ("salut je suis...", "et c tout") pour extraire l'essence
    private static String extraireMissionPure(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String clean = raw.trim();

        // Supprime les salutations et intros inutiles
        clean = clean.replaceAll("(?i)^(salut|bonjour|hello|hey|bonsoir)[,\\s]+", "");
        clean = clean.replaceAll("(?i)(et c est tout|et c tout|c est tout|merci|merci d avance)[.!\\s]*$", "");

        return clean.trim();
    }

    // Transforme les diagnostics de qualité en contraintes explicites pour le LLM
    private static List<String> genererContraintesAutomatiques(PromptProfile profile) {
        List<String> contraintes = new ArrayList<>();

        if (profile.qualityDiagnostic().scoreContraintes() < 20) {
            contraintes.add("Structure de réponse claire et aérée avec titres Markdown et listes à puces.");
        }

        if (profile.rawText().split("\\s+").length < 10) {
            contraintes.add("Couvrir les cas limites essentiels même s'ils n'étaient pas explicités.");
        }

        contraintes.add("Ne pas inventer de faits non vérifiés ; expliciter clairement les hypothèses si nécessaire.");

        return contraintes;
    }
}
