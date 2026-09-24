package llm;

import nlp.PromptProfile;
import java.io.PrintStream;
import java.util.Scanner;

// Orchestrateur de haut niveau pour l'exécution des LLMs locaux avec routage et onboarding
public class LlmEngine {

    private final LlmBackend backend;
    private final LlmConfig config;
    private final PrintStream out;
    private final Scanner in;

    public LlmEngine() {
        this(new LocalLlmBackend(), LlmConfig.chargerParDefaut(), System.out, new Scanner(System.in));
    }

    public LlmEngine(LlmBackend backend, LlmConfig config, PrintStream out, Scanner in) {
        this.backend = backend != null ? backend : new LocalLlmBackend();
        this.config = config != null ? config : new LlmConfig();
        this.out = out != null ? out : System.out;
        this.in = in != null ? in : new Scanner(System.in);
    }

    // Message envoyé au modèle local : le prompt brut et les indices de l'analyse NLP.
    // Le méta-prompt complet est trop long pour un modèle de 1.5B.
    public static String construireDemandeAmelioration(PromptProfile profil) {
        return construireDemandeAmelioration(profil, null);
    }

    // langueCible : langue imposee au prompt ameliore (null = celle du prompt brut)
    public static String construireDemandeAmelioration(PromptProfile profil, String langueCible) {
        StringBuilder sb = new StringBuilder();
        sb.append("Prompt brut à améliorer :\n\"\"\"\n").append(profil.rawText().trim()).append("\n\"\"\"\n\n");

        // L'analyse NLP guide le modele : il sait de quel genre de demande il s'agit, ce qu'un
        // bon prompt de ce genre doit preciser, et ce qui manque au prompt brut
        sb.append("Analyse automatique du prompt brut :\n");
        nlp.TypeOfPrompt type = profil.classification().primaryType();
        sb.append("- Type de demande : ").append(libelleType(type)).append('\n');
        sb.append("- Un bon prompt de ce type précise : ").append(attendusType(type)).append('\n');
        if (!profil.detectedTechnologies().isEmpty()) {
            sb.append("- Technologies mentionnées : ").append(String.join(", ", profil.detectedTechnologies())).append('\n');
        }
        if (profil.decomposition() != null && profil.decomposition().hasMultipleObjectives()) {
            sb.append("- Objectifs distincts à conserver : ")
                    .append(String.join(" ; ", profil.decomposition().objectives())).append('\n');
        }
        java.util.List<String> manques = profil.qualityDiagnostic().pistesAmelioration();
        if (!manques.isEmpty()) {
            sb.append("- Ce qui manque au prompt brut (à signaler entre crochets) : ")
                    .append(String.join(" ", manques)).append('\n');
        }

        String langue = langueCible != null && !langueCible.isBlank() ? langueCible : profil.language();
        sb.append("\nRédige le contenu des sections en ").append(gen.MetaPromptEngine.libelleLangue(langue))
                .append(", même si le prompt brut est dans une autre langue.");
        // Rappel en fin de message : un petit modele suit surtout la derniere consigne lue
        sb.append("\nRAPPEL : ne réalise PAS la tâche. Aucun code, aucune solution, aucune explication. "
                + "Écris uniquement les 5 sections du prompt amélioré (Rôle, Contexte, Tâche, Contraintes, "
                + "Format de sortie attendu), puis arrête-toi.");
        return sb.toString();
    }

    public static String libelleType(nlp.TypeOfPrompt type) {
        if (type == null) return "demande générale";
        return switch (type) {
            case APPRENTISSAGE_TUTORIEL -> "apprendre une notion ou un savoir-faire";
            case CONCEPTION_ARCHITECTURE -> "concevoir ou créer un projet (logiciel, site, système)";
            case DEPANNAGE_DIAGNOSTIC -> "résoudre un problème ou une erreur";
            case CREATION_REDACTION -> "rédiger ou créer un contenu";
            case PROTOCOLE_RECETTE -> "obtenir une procédure ou une recette pas à pas";
            case COMPARAISON_DECISION -> "comparer des options pour prendre une décision";
            case CONCEPT_VULGARISATION -> "comprendre un concept";
        };
    }

    public static String attendusType(nlp.TypeOfPrompt type) {
        if (type == null) return "le contexte, l'objectif et le résultat attendu";
        return switch (type) {
            case APPRENTISSAGE_TUTORIEL -> "le niveau actuel de l'apprenant, l'objectif visé, le temps disponible";
            case CONCEPTION_ARCHITECTURE -> "les fonctionnalités, les utilisateurs visés, les technologies imposées, "
                    + "les contraintes (budget, délais, sécurité), le livrable attendu";
            case DEPANNAGE_DIAGNOSTIC -> "les symptômes, le message d'erreur exact, l'environnement, "
                    + "ce qui a déjà été essayé";
            case CREATION_REDACTION -> "le public visé, le ton, la longueur, le support de publication";
            case PROTOCOLE_RECETTE -> "le matériel ou les ingrédients disponibles, le niveau, le temps, "
                    + "les contraintes à respecter";
            case COMPARAISON_DECISION -> "les options comparées, les critères de choix, le contexte d'usage";
            case CONCEPT_VULGARISATION -> "le niveau de connaissance du lecteur, la profondeur souhaitée, "
                    + "le besoin d'exemples";
        };
    }


    public LlmBackend.GenerationResult execute(String metaPrompt, PromptProfile profile, String modelOverride, boolean interactive) {
        return execute(metaPrompt, profile, modelOverride, interactive, null);
    }

    // miseEnForme : si fournie, la reponse brute n'est pas diffusee au fil de l'eau ; seul
    // le texte mis en forme est affiche (prompt ameliore au format commun, PromptStructure)
    public LlmBackend.GenerationResult execute(String metaPrompt, PromptProfile profile, String modelOverride,
                                               boolean interactive, java.util.function.UnaryOperator<String> miseEnForme) {
        // 1. Routage intelligent vers le modèle expert
        ModelRouter.RoutingDecision routing = ModelRouter.resolve(modelOverride, profile);
        ModelType targetModel = routing.selectedModel();

        out.println("\n--- [ROUTAGE DU MODELE EXPERT] ---");
        out.println("Modele selectionne : " + targetModel.getNomAffiche());
        out.println("Justification       : " + routing.rationale());
        out.println("Confiance           : " + (int)(routing.confidenceScore() * 100) + "%\n");

        // 2. Vérification de l'installation et onboarding si premier lancement
        boolean isInstalled = ModelInstaller.isModelInstalled(targetModel, config.getRepertoireModeles());

        // Sans modèle forcé, un modèle déjà installé vaut mieux qu'un téléchargement
        // silencieux de plus d'1 Go pour le spécialiste du routage.
        boolean modeleForce = modelOverride != null && !modelOverride.isBlank() && !"auto".equalsIgnoreCase(modelOverride);
        if (!isInstalled && !modeleForce) {
            for (ModelType candidat : ModelType.getAllAvailable()) {
                if (ModelInstaller.isModelInstalled(candidat, config.getRepertoireModeles())) {
                    out.println("[*] " + targetModel.getNomAffiche() + " n'est pas installe : utilisation de "
                            + candidat.getNomAffiche() + ".");
                    targetModel = candidat;
                    isInstalled = true;
                    break;
                }
            }
        }

        if (!isInstalled && !config.isPermissionAccordee()) {
            if (interactive) {
                ModelType accepted = ModelInstaller.demanderPermissionUtilisateur(in, out, targetModel, config);
                if (accepted == null) {
                    return null;
                }
                targetModel = accepted;
                isInstalled = ModelInstaller.isModelInstalled(targetModel, config.getRepertoireModeles());
            } else {
                out.println("[!] Avertissement : Le modele " + targetModel.getNomAffiche() + " n'est pas encore installe localement.");
                out.println("    Installez-le avec : --models-install " + targetModel.getId() + "  (ou lancez l'outil sans argument)");
                return null;
            }
        }

        // Si le modèle n'est pas sur le disque mais la permission est acquise
        if (!isInstalled && config.isPermissionAccordee()) {
            boolean success = ModelInstaller.telechargerModele(targetModel, config.getRepertoireModeles(), out, null);
            if (!success) {
                out.println("[!] Impossible de telecharger le modele. Poursuite sans execution locale.");
                return null;
            }
        }

        // 3. Exécution de l'inférence locale
        out.println("\n--- [PROMPT AMELIORE PAR LE MODELE LOCAL (" + targetModel.getNomAffiche() + ")] ---\n");

        try {
            boolean diffusion = config.isStreamingActive() && miseEnForme == null;
            if (miseEnForme != null) {
                out.println("Generation en cours...\n");
            }
            LlmBackend.GenerationResult result = backend.generate(targetModel, metaPrompt, config, token -> {
                if (diffusion) {
                    out.print(token);
                    out.flush();
                }
            });

            if (miseEnForme != null) {
                out.println(miseEnForme.apply(result.fullText()));
            } else if (!config.isStreamingActive()) {
                out.println(result.fullText());
            }

            out.println("\n\n--------------------------------------------");
            out.printf("Statistiques : %d tokens generes en %d ms (%.1f tokens/sec)%n",
                    result.totalTokens(), result.elapsedMs(), result.tokensPerSecond());
            out.println("--------------------------------------------\n");

            return result;

        } catch (Exception e) {
            out.println("\n[!] Erreur pendant l'inference locale : " + e.getMessage());
            return null;
        }
    }

    public LlmConfig getConfig() {
        return config;
    }

    public LlmBackend getBackend() {
        return backend;
    }
}
