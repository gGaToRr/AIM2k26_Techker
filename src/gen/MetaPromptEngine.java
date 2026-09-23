package gen;

import cli.CliArgs;
import com.samskivert.mustache.Mustache;
import nlp.DomainExtractor;
import nlp.PromptProfile;
import nlp.TechStackDetector;
import nlp.TypeOfPrompt;

import java.util.*;

// Moteur Meta-Prompting d'élite orchestrant les 7 Archétypes Universels d'Intention
public class MetaPromptEngine {

    // Génère le super prompt optimisé en Markdown (version standard)
    public static String genererPromptOptimise(PromptProfile profile) {
        return genererPromptOptimise(profile, null);
    }

    // Génère le super prompt optimisé avec prise en compte des options CLI (overrides, agent, etc.)
    public static String genererPromptOptimise(PromptProfile profile, CliArgs options) {
        TypeOfPrompt type = profile.classification().primaryType();
        String subType = determinerSousType(profile, type);

        // 1. Chargement du template (avec override si -t/--template est spécifié)
        String rawTemplate = null;
        if (options != null && options.hasTemplate()) {
            rawTemplate = TemplateLoader.chargerTemplateParNom(options.template()).orElse(null);
        }
        if (rawTemplate == null) {
            rawTemplate = TemplateLoader.chargerTemplate(type, subType);
        }

        // 2. Construction du contexte pour le moteur Mustache
        Map<String, Object> context = construireContexteMustache(profile, type, options);

        // Exemples calibres : un modele leger suit mieux un format montre que decrit.
        String exemples = FewShotLibrary.selectionner(profile, type, subType);
        context.put("EXEMPLES_FEW_SHOT", exemples);
        context.put("hasFewShot", !exemples.isEmpty());

        // 3. Rendu ultra-rapide avec JMustache SANS échappement HTML
        String rendered = Mustache.compiler()
                .escapeHTML(false)
                .emptyStringIsFalse(true)
                .zeroIsFalse(true)
                .compile(rawTemplate)
                .execute(context)
                .trim();

        // 4. Adaptation spécifique selon le modèle/agent IA cible (-a/--agent)
        if (options != null && options.hasAgent()) {
            rendered = adapterPourAgent(rendered, options.agent());
        }

        return rendered;
    }

    // Adapte la structure du prompt aux conventions du modèle/agent IA cible
    public static String adapterPourAgent(String prompt, String agent) {
        if (agent == null || agent.isBlank()) return prompt;
        String a = agent.toLowerCase().trim();

        if (a.contains("claude") || a.contains("anthropic")) {
            return "<claude_system_prompt>\n"
                    + "Tu es Claude, une IA d'Anthropic conçue pour être utile, précise et transparente.\n"
                    + "<thinking_instructions>\n"
                    + "Avant de répondre, utilise un bloc <thinking> pour structurer tes réflexions, valider les contraintes et explorer les solutions optimales.\n"
                    + "</thinking_instructions>\n"
                    + "</claude_system_prompt>\n\n"
                    + prompt;
        } else if (a.contains("deepseek") || a.contains("r1")) {
            return "<system_prompt>\n"
                    + "Tu es DeepSeek-V3 / DeepSeek-R1. Analyse le problème en profondeur.\n"
                    + "Exécute un raisonnement rigoureux étape par étape dans des balises <think> avant de fournir la réponse définitive.\n"
                    + "</system_prompt>\n\n"
                    + prompt;
        } else if (a.contains("gpt") || a.contains("openai") || a.contains("chatgpt")) {
            return "# SYSTEM ROLE & GUIDELINES (OpenAI GPT-4o)\n"
                    + "Adopt an expert, authoritative, and direct tone. Apply step-by-step chain of thought internally and follow all structured output schemas strictly.\n\n"
                    + prompt;
        } else if (a.contains("gemini") || a.contains("google")) {
            return "# SYSTEM INSTRUCTIONS (Google Gemini 1.5/2.0)\n"
                    + "Format your response with rich Markdown, clear hierarchical headings, code blocks with syntax highlighting, and verified factual consistency.\n\n"
                    + prompt;
        } else if (a.contains("llama") || a.contains("meta")) {
            return "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n"
                    + "You are a helpful, respectful and honest AI assistant. Always answer as helpfully as possible.\n<|eot_id|><|start_header_id|>user<|end_header_id|>\n"
                    + prompt + "\n<|eot_id|><|start_header_id|>assistant<|end_header_id|>";
        } else if (a.contains("feynman")) {
            return "# MISSION MÉTHODE FEYNMAN EXPLICITE\n"
                    + "Règle absolue : Explique chaque concept comme à un débutant intelligent de 12 ans avec des métaphores simples, sans jargon inutile.\n\n"
                    + prompt;
        }

        return prompt;
    }

    // Génère un export JSON structuré complet du profil NLP et du prompt optimisé
    public static String genererExportJson(PromptProfile profile, String promptOptimise, CliArgs options) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"project\": \"Prompting tool 4 a better work from AI (AIM2k26)\",\n");
        sb.append("  \"version\": \"1.0.0\",\n");
        sb.append("  \"prompt_profile\": {\n");
        sb.append("    \"raw_text\": \"").append(echapperJson(profile.rawText())).append("\",\n");
        sb.append("    \"language\": \"").append(profile.language()).append("\",\n");
        sb.append("    \"is_command\": ").append(profile.isCommand()).append(",\n");
        sb.append("    \"is_question\": ").append(profile.isQuestion()).append(",\n");
        sb.append("    \"code_density\": ").append(String.format(Locale.US, "%.3f", profile.codeDensity())).append(",\n");
        sb.append("    \"archetype\": \"").append(profile.classification().primaryType().name()).append("\",\n");
        sb.append("    \"archetype_label\": \"").append(profile.classification().primaryType().getLabel()).append("\",\n");
        sb.append("    \"confidence\": \"").append(profile.classification().confidenceLevel().name()).append("\",\n");
        sb.append("    \"quality_score\": ").append(profile.qualityDiagnostic().scoreGlobal()).append(",\n");
        sb.append("    \"estimated_tokens\": ").append(profile.tokenMetrics().estimatedTokens()).append("\n");
        sb.append("  },\n");
        sb.append("  \"meta_prompt\": \"").append(echapperJson(promptOptimise)).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    private static String echapperJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Détermine le sous-template le plus adapté à l'intention fine parmi les 7 Archétypes
    // Delegue la selection du sous-type aux regles externalisees (voir SubTypeRules)
    private static String determinerSousType(PromptProfile profile, TypeOfPrompt type) {
        return SubTypeRules.resoudre(type, profile);
    }

    // Prépare les variables injectées (application des 5 règles d'optimisation)
    private static Map<String, Object> construireContexteMustache(PromptProfile profile, TypeOfPrompt type, CliArgs options) {
        Map<String, Object> ctx = new HashMap<>();

        String cleanMission = extraireMissionPure(profile.rawText());

        ctx.put("rawPrompt", profile.rawText().trim());
        ctx.put("cleanedMission", cleanMission);
        ctx.put("sanitizedPrompt", profile.sanitizedText());
        ctx.put("type", type.name());
        ctx.put("typeLabel", type.getLabel());

        // Langue (avec override si -l/--language)
        if (options != null && options.hasLanguage()) {
            String l = options.language().trim();
            String langLabel = switch (l.toLowerCase()) {
                case "fr", "francais", "français" -> "Français";
                case "en", "english", "anglais" -> "English";
                case "es", "espagnol", "spanish" -> "Español";
                case "de", "allemand", "deutsch", "german" -> "Deutsch";
                default -> l;
            };
            ctx.put("language", langLabel);
            ctx.put("hasTargetLanguage", true);
            ctx.put("targetLanguage", langLabel);
        } else {
            ctx.put("language", profile.language().equals("FR") ? "Français" : "English");
            profile.targetTranslationLanguage().ifPresentOrElse(
                    target -> {
                        ctx.put("hasTargetLanguage", true);
                        ctx.put("targetLanguage", target);
                    },
                    () -> ctx.put("hasTargetLanguage", false)
            );
        }

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

        // Injection de Domaine & Persona Expert (avec override si -d/--domain)
        if (options != null && options.hasDomain()) {
            DomainExtractor.DomainInfo forcedDomain = DomainExtractor.creerDomaineForce(options.domain(), cleanMission);
            ctx.put("hasDomainExpertise", true);
            ctx.put("domainName", forcedDomain.domainName());
            ctx.put("domainTopic", forcedDomain.extractedTopic());
            ctx.put("domainPersona", forcedDomain.expertPersona());
        } else {
            boolean hasDomain = profile.domainInfo() != null && profile.domainInfo().isDomainIdentified();
            ctx.put("hasDomainExpertise", hasDomain);
            if (hasDomain) {
                ctx.put("domainName", profile.domainInfo().domainName());
                ctx.put("domainTopic", profile.domainInfo().extractedTopic());
                ctx.put("domainPersona", profile.domainInfo().expertPersona());
            }
        }

        // Décomposition des sous-objectifs (Issue #3)
        boolean hasMultiObjectives = profile.decomposition() != null && profile.decomposition().hasMultipleObjectives();
        ctx.put("hasMultipleObjectives", hasMultiObjectives);
        if (hasMultiObjectives) {
            ctx.put("objectives", profile.decomposition().objectives());
            ctx.put("formattedObjectivesXml", profile.decomposition().xmlFormattedList());
        }

        // Règle 5 : Auto-correction des faiblesses du prompt
        List<String> autoConstraints = genererContraintesAutomatiques(profile);
        ctx.put("hasAutoConstraints", !autoConstraints.isEmpty());
        ctx.put("autoConstraints", autoConstraints);

        return ctx;
    }

    // Nettoie le bruit conversationnel pour extraire l'essence
    private static String extraireMissionPure(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String clean = raw.trim();

        clean = clean.replaceAll("(?i)^(salut|bonjour|hello|hey|bonsoir|alors)[,\\s]+", "");
        clean = clean.replaceAll("(?i)(et c est tout|et c tout|c est tout|merci beaucoup|merci d avance|merci|xd|stp|s il te plait|s'il te plaît|s'il te plait)[.!\\s]*$", "");

        clean = nlp.Sanitzer.nettoyerEtFormaterTexte(clean);
        return clean.trim();
    }

    // Transforme les diagnostics de qualité en contraintes explicites pour le LLM
    private static List<String> genererContraintesAutomatiques(PromptProfile profile) {
        List<String> contraintes = new ArrayList<>();

        if (profile.qualityDiagnostic().scoreContraintes() < 20) {
            contraintes.add("Structure de réponse claire et aérée avec titres Markdown et listes à puces.");
        }

        if (profile.rawText().split("\\s+").length < 10) {
            contraintes.add("Couvrir les aspects fondamentaux, historiques et pratiques de manière exhaustive.");
        }

        contraintes.add("Ne pas inventer de faits non vérifiés ; expliciter clairement les hypothèses si nécessaire.");

        return contraintes;
    }
}
