package llm;

import cli.CliArgs;
import cli.FormatSortie;
import gen.MetaPromptEngine;
import nlp.AvertissementPrompt;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import util.Json;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

// Amelioration d'un prompt pour l'extension (--improve-json).
//
// Lit sur l'entree standard le message JSON de l'extension
// ({"prompt": "...", "format": "txt|md|json", "cible": "gpt|claude|...", "langue": "fr|en|...",
//   "modele": "qwen|gemma|deepseek|smollm", "creativite": "0.7", "longueurMax": "2048"}) et ecrit
// une seule ligne JSON : {"ok":true,"ameliore":"...","source":"modele"|"nlp","avertissement":...}
// ou {"ok":false,"message":"..."} (message illisible ou prompt vide).
// Le prompt transite en JSON et non en argument : il peut contenir n'importe quel caractere.
// Les reglages absents ou "auto" laissent le comportement par defaut (Markdown, sans
// adaptation a une IA, langue du prompt brut).
public final class AmeliorationCommand {

    private AmeliorationCommand() {}

    public static int executer(InputStream entree, PrintStream out, LlmConfig config, LlmBackend backend,
                               boolean moteurDisponible) {
        String prompt;
        CliArgs options;
        try {
            String message = new String(entree.readAllBytes(), StandardCharsets.UTF_8);
            prompt = Json.extraireChaine(message, "prompt");
            // Memes options que la ligne de commande (-o, -a, -l) : meme rendu que le CLI
            options = CliArgs.builder()
                    .output(reglage(message, "format"))
                    .agent(reglage(message, "cible"))
                    .language(reglage(message, "langue"))
                    .model(reglage(message, "modele"))
                    .build();
            // Reglages du modele local : temperature et nombre maximal de tokens produits
            appliquerReglagesModele(message, config);
        } catch (Exception e) {
            return echec(out, "message illisible : " + e.getMessage());
        }
        if (prompt == null || prompt.isBlank()) {
            return echec(out, "prompt vide");
        }

        PromptProfile profil = Lemmatizer.analyser(prompt);
        long debut = System.currentTimeMillis();

        // Garde-fous : ici, jamais de telechargement silencieux (appele a chaque envoi de
        // prompt) ni de reponse simulee presentee comme une amelioration. Sans modele
        // utilisable, repli sur le meta-prompt du moteur NLP, moins fiable : l'extension
        // le signale a l'utilisateur.
        boolean unModeleInstalle = ModelManager.inventaire(config.getRepertoireModeles()).stream()
                .anyMatch(ModelManager.ModelStatus::installe);
        if (!unModeleInstalle) {
            return repliNlp(out, profil, options, "aucun modele installe", debut);
        }
        if (!moteurDisponible) {
            return repliNlp(out, profil, options, "moteur llama.cpp introuvable", debut);
        }

        // Prompt parfait : le vrai prompt de la base le plus proche, guide par le NLP (themes)
        // et classe selon la richesse lexicale du prompt ; le modele l'ameliore a partir de lui
        java.util.Optional<corpus.PromptParfait.Reference> reference = corpus.PromptParfait.trouver(prompt);

        // Le moteur ecrit routage et statistiques : sortie ecartee, seul le JSON final compte
        PrintStream silencieux = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        LlmEngine moteur = new LlmEngine(backend, config, silencieux, new Scanner(InputStream.nullInputStream()));

        LlmBackend.GenerationResult resultat =
                moteur.execute(LlmEngine.construireDemandeAmelioration(profil, options.language(),
                                reference.map(corpus.PromptParfait.Reference::texte).orElse(null)), profil,
                        modeleDemande(options, config), false);
        if (resultat == null || resultat.fullText() == null || resultat.fullText().isBlank()) {
            return repliNlp(out, profil, options, "le modele n'a rien produit", debut);
        }
        // Meme format quel que soit le modele : sections reconnues puis rendues a l'identique.
        // Une reponse sans structure (mots-cles en vrac, reponse a la tache) ne vaut pas le NLP.
        java.util.Optional<String> structure = PromptStructure.structurer(resultat.fullText(),
                langueRendu(profil, options), prompt);
        if (structure.isEmpty()) {
            return repliNlp(out, profil, options, "reponse du modele non structuree", debut);
        }
        // Un prompt bien presente mais sur un autre sujet serait pire que le repli NLP
        if (!PromptStructure.resteSurLeSujet(structure.get(), profil.tokens())) {
            return repliNlp(out, profil, options, "reponse du modele hors sujet", debut);
        }
        String ameliore = structure.get();
        if (options.hasAgent()) {
            ameliore = MetaPromptEngine.adapterPourAgent(ameliore, options.agent());
        }
        return reussite(out, profil, mettreEnForme(profil, ameliore, options), "modele",
                resultat.modelUsed().getNomAffiche(), null, reference.orElse(null), debut);
    }

    // Le meta-prompt NLP applique deja langue (-l) et adaptation a l'IA cible (-a)
    private static int repliNlp(PrintStream out, PromptProfile profil, CliArgs options, String raison, long debut) {
        String metaPrompt = MetaPromptEngine.genererPromptOptimise(profil, options).trim();
        return reussite(out, profil, mettreEnForme(profil, metaPrompt, options), "nlp", null, raison, null, debut);
    }

    // Langue imposee (-l) ou detectee (FR, EN) : les titres des sections la suivent
    private static String langueRendu(PromptProfile profil, CliArgs options) {
        return options.hasLanguage() ? options.language() : profil.language();
    }

    // Modele choisi dans l'extension, s'il est installe. Un modele impose mais absent serait
    // telecharge par le moteur (plus d'1 Go, a chaque envoi) : le routage automatique
    // choisit alors parmi les modeles installes.
    private static String modeleDemande(CliArgs options, LlmConfig config) {
        if (!options.hasModel()) return "auto";
        return ModelType.fromAlias(options.model())
                .filter(modele -> ModelInstaller.isModelInstalled(modele, config.getRepertoireModeles()))
                .map(ModelType::getId)
                .orElse("auto");
    }

    // Valeurs hors bornes ou illisibles : celles de la configuration sont gardees
    static void appliquerReglagesModele(String message, LlmConfig config) {
        String creativite = Json.extraireChaine(message, "creativite");
        if (creativite != null) {
            try {
                double temperature = Double.parseDouble(creativite.trim());
                if (temperature >= 0.0 && temperature <= 1.0) config.setTemperature(temperature);
            } catch (NumberFormatException ignoree) {
                // reglage illisible : temperature de la configuration
            }
        }
        String longueur = Json.extraireChaine(message, "longueurMax");
        if (longueur != null) {
            try {
                int maxTokens = Integer.parseInt(longueur.trim());
                if (maxTokens >= 64 && maxTokens <= 8192) config.setMaxTokens(maxTokens);
            } catch (NumberFormatException ignoree) {
                // reglage illisible : limite de la configuration
            }
        }
    }

    private static String mettreEnForme(PromptProfile profil, String prompt, CliArgs options) {
        return FormatSortie.depuis(options).rendre(profil, prompt, options);
    }

    // Valeur d'un reglage de l'extension ; "auto" (ou absent) = pas de forcage
    private static String reglage(String message, String cle) {
        String valeur = Json.extraireChaine(message, cle);
        return valeur == null || valeur.isBlank() || valeur.equalsIgnoreCase("auto") ? null : valeur.trim();
    }

    // source : "modele" (reecrit par le LLM local) ou "nlp" (meta-prompt, repli moins fiable)
    // reference : prompt parfait de la base dont le modele s'est inspire (null sans base ou sans
    // prompt assez proche)
    private static int reussite(PrintStream out, PromptProfile profil, String ameliore, String source,
                                String modele, String raison, corpus.PromptParfait.Reference reference, long debut) {
        out.println("{\"ok\":true"
                + ",\"ameliore\":" + Json.chaine(ameliore)
                + ",\"source\":" + Json.chaine(source)
                + ",\"modele\":" + Json.chaine(modele)
                + ",\"raison\":" + Json.chaine(raison)
                + ",\"avertissement\":" + Json.chaine(AvertissementPrompt.verifier(profil.rawText()).orElse(null))
                + ",\"themes\":" + Json.chaine(String.join(",", profil.themes()))
                + ",\"reference\":" + referenceEnJson(reference)
                + ",\"type\":" + Json.chaine(String.valueOf(profil.classification().primaryType()))
                + ",\"score\":" + profil.qualityDiagnostic().scoreGlobal()
                + ",\"secondes\":" + String.format(java.util.Locale.ROOT, "%.1f",
                        (System.currentTimeMillis() - debut) / 1000.0)
                + "}");
        return ModelsCommand.SUCCES;
    }

    private static String referenceEnJson(corpus.PromptParfait.Reference reference) {
        if (reference == null) return "null";
        return "{\"id\":" + Json.chaine(reference.entree().prompt().id())
                + ",\"texte\":" + Json.chaine(reference.texte())
                + ",\"themes\":" + Json.chaine(String.join(",", reference.themes()))
                + ",\"richesse\":" + reference.richesse()
                + ",\"couverture\":" + String.format(java.util.Locale.ROOT, "%.2f", reference.couverture()) + "}";
    }

    private static int echec(PrintStream out, String message) {
        out.println("{\"ok\":false,\"message\":" + Json.chaine(message) + "}");
        return ModelsCommand.ECHEC;
    }
}
