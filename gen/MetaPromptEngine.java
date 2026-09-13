package gen;

import com.samskivert.mustache.Mustache;
import nlp.PromptProfile;
import nlp.TechStackDetector;
import nlp.TypeOfPrompt;

import java.util.*;

// Moteur Meta-Prompting d'élite orchestrant les 7 Archétypes Universels d'Intention
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

    // Détermine le sous-template le plus adapté à l'intention fine parmi les 7 Archétypes
    private static String determinerSousType(PromptProfile profile, TypeOfPrompt type) {
        String lower = profile.rawText().toLowerCase();
        int nbMots = profile.rawText().trim().split("\\s+").length;

        return switch (type) {
            case APPRENTISSAGE_TUTORIEL -> {
                if (lower.contains("debutant") || lower.contains("initiation") || lower.contains("premier") || lower.contains("bases") || lower.contains("cours")) {
                    yield "guide_debutant";
                }
                yield "feynman_learning";
            }
            case CONCEPTION_ARCHITECTURE -> {
                boolean isWebUI = lower.contains("site") || lower.contains("css") || lower.contains("html") || lower.contains("tailwind") || lower.contains("frontend") || lower.contains("web") || lower.contains("responsive");
                if (isWebUI && (lower.contains("design") || lower.contains("bouton") || lower.contains("carte") || lower.contains("ombre") || lower.contains("layout"))) {
                    yield "frontend_ui";
                } else if (lower.contains("architecture") || lower.contains("arborescence") || lower.contains("structure de dossier") || lower.contains("structure des dossier") || lower.contains("organisation")) {
                    yield "architecture_systeme";
                }
                yield "code_generation";
            }
            case DEPANNAGE_DIAGNOSTIC -> {
                if (lower.contains("review") || lower.contains("audit") || lower.contains("securite") || lower.contains("conformite")) {
                    yield "audit_review";
                } else if (lower.contains("refactor") || lower.contains("clean") || lower.contains("amelior") || lower.contains("optimis")) {
                    yield "refactor_clean";
                }
                yield "root_cause_debug";
            }
            case CREATION_REDACTION -> {
                if (profile.targetTranslationLanguage().isPresent() || lower.contains("tradui") || lower.contains("translate")) {
                    yield "technical_translation";
                } else if (lower.contains("corrig") || lower.contains("orthographe") || lower.contains("grammaire") || lower.contains("relectur")) {
                    yield "proofreading";
                } else if (lower.contains("brainstorm") || lower.contains("idee") || lower.contains("concept")) {
                    yield "brainstorming";
                }
                yield "storytelling";
            }
            case PROTOCOLE_RECETTE -> {
                if (lower.contains("recette") || lower.contains("cuisin") || lower.contains("cuisson") || lower.contains("gateau") || lower.contains("tarte") || lower.contains("plat") || lower.contains("ingredient")) {
                    yield "recette_culinaire";
                }
                yield "protocole_technique";
            }
            case COMPARAISON_DECISION -> {
                if (lower.contains("choisir") || lower.contains("choix") || lower.contains("arbitrage") || lower.contains("decision") || lower.contains("lequel")) {
                    yield "aide_decision";
                }
                yield "matrice_comparative";
            }
            case CONCEPT_VULGARISATION -> {
                if (nbMots <= 3 && !profile.rawText().contains("?")) {
                    yield "concept_encyclopedique";
                }
                yield "vulgarisation_feynman";
            }
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
        ctx.put("typeLabel", type.getLabel());
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

        // Injection de Domaine & Persona Expert (Issue #2)
        boolean hasDomain = profile.domainInfo() != null && profile.domainInfo().isDomainIdentified();
        ctx.put("hasDomainExpertise", hasDomain);
        if (hasDomain) {
            ctx.put("domainName", profile.domainInfo().domainName());
            ctx.put("domainTopic", profile.domainInfo().extractedTopic());
            ctx.put("domainPersona", profile.domainInfo().expertPersona());
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
