package llm;

import util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Moteur d'exécution local utilisant le runtime natif (llama-cli / GGUF) ou fallback autonome
public class LocalLlmBackend implements LlmBackend {

    // Consigne systeme de l'amelioration. Les petits modeles (1,5 a 2B) suivent bien mieux
    // un exemple complet que des regles abstraites : la structure attendue y est montree.
    // Les titres de section restent en francais : PromptStructure les reconnait et les
    // traduit dans la langue du prompt au moment du rendu.
    public static final String CONSIGNE_AMELIORATION = """
            Tu es un expert en prompt engineering. Tu reçois le prompt brut d'un utilisateur et une analyse \
            automatique de ce prompt. Ta mission : le réécrire pour qu'une IA le comprenne parfaitement. \
            Tu ne réponds JAMAIS au prompt et tu ne réalises pas la tâche demandée.
            Règles :
            1. Garde l'intention exacte et toutes les informations du prompt brut. N'invente aucun fait \
            (nom, chiffre, technologie, public) qui n'y figure pas.
            2. Quand une information importante manque, écris-la entre crochets pour que l'utilisateur la \
            complète, par exemple [préciser le public visé].
            3. Réponds uniquement avec ces 5 sections, dans cet ordre, chaque titre seul sur sa ligne :
            Rôle :
            Contexte :
            Tâche :
            Contraintes :
            Format de sortie attendu :
            4. Le Rôle est UNE phrase qui commence par "Tu es" et décrit l'expert idéal pour la demande.
            5. La Tâche reformule précisément ce que demande le prompt brut, avec un verbe d'action.
            6. Les Contraintes sont une liste de tirets, sans doublon. Elles viennent du prompt brut ; \
            une contrainte absente mais utile s'écrit entre crochets.
            7. Le Format de sortie attendu décrit le résultat que l'IA doit rendre pour CETTE demande.

            Exemple (sujet différent : n'en recopie pas le contenu, seulement la structure)
            Prompt brut : aide moi a ecrire un mail pour demander une augmentation
            Prompt amélioré :
            Rôle :
            Tu es un expert en communication professionnelle et en négociation salariale.
            Contexte :
            Je souhaite demander une augmentation à mon responsable. Mon poste : [préciser le poste]. \
            Mon ancienneté : [préciser].
            Tâche :
            Rédige un e-mail qui demande une augmentation de salaire de façon claire et convaincante.
            Contraintes :
            - Ton professionnel et respectueux
            - Mettre en avant mes réalisations : [préciser 2 ou 3 réalisations]
            - 150 mots maximum
            Format de sortie attendu :
            Un e-mail avec un objet, une formule d'appel, le corps du message et une formule de politesse.""";

    // llama-completion (llama.cpp recent) fait une generation unique et non interactive :
    // il passe avant llama-cli, qui ouvre une session de chat.
    private static final String[] RUNNER_CANDIDATES = {
            "llama-completion",
            "llama-cli",
            "bin/llama-cli",
            "./llama-cli",
            "/usr/local/bin/llama-cli",
            "/usr/bin/llama-cli"
    };

    @Override
    public boolean isAvailable(ModelType model, String modelsDir) {
        return ModelInstaller.isModelInstalled(model, modelsDir);
    }

    public static String detectRunnerBinary() {
        // La suite de tests doit rester deterministe, que llama.cpp soit installe ou non
        if (Boolean.getBoolean("aim.llama.desactive")) {
            return null;
        }
        // Binaire precompile depose dans llama/<version>/ a la racine du projet
        File[] versions = new File("llama").listFiles(File::isDirectory);
        if (versions != null) {
            java.util.Arrays.sort(versions);
            for (int i = versions.length - 1; i >= 0; i--) {
                // .exe : moteur installe sous Windows (RuntimeInstaller)
                for (String nom : new String[] {"llama-completion", "llama-completion.exe"}) {
                    File f = new File(versions[i], nom);
                    if (f.exists() && f.canExecute()) {
                        return f.getAbsolutePath();
                    }
                }
            }
        }
        for (String candidate : RUNNER_CANDIDATES) {
            File f = new File(candidate);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        // Vérifie si la commande est dans le PATH
        try {
            Process p = new ProcessBuilder("which", "llama-cli").start();
            if (p.waitFor() == 0) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = r.readLine();
                    if (line != null && !line.isBlank()) return line.trim();
                }
            }
        } catch (Exception e) {
            Log.exceptionIgnoree("Detection de llama-cli via which", e);
        }
        return null;
    }

    @Override
    public GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer tokenConsumer) throws Exception {
        long startTime = System.currentTimeMillis();
        Path modelPath = ModelInstaller.getModelPath(model, config.getRepertoireModeles());

        String runner = detectRunnerBinary();

        if (doitUtiliserLeRuntimeNatif(runner, modelPath)) {
            return runNativeInference(runner, modelPath, model, prompt, configPour(model, config), tokenConsumer,
                    startTime);
        } else {
            // Mode autonome / simulation contrôlée pour environnement de test ou sans binaire externe
            return runEmbeddedInference(model, prompt, config, tokenConsumer, startTime);
        }
    }

    // Tokens accordes a la reflexion d'un modele de raisonnement, en plus de la reponse
    public static final int MARGE_RAISONNEMENT = 1536;

    // La limite de tokens (test de sante : 16, reglage "Longueur" de l'extension) vaut pour
    // la reponse. Un modele de raisonnement reflechit d'abord : sans marge, il serait coupe
    // en pleine reflexion et ne produirait aucune reponse.
    public static LlmConfig configPour(ModelType model, LlmConfig config) {
        if (!model.raisonne()) return config;
        return new LlmConfig(config.isPermissionAccordee(), config.getModeleParDefaut(),
                config.getRepertoireModeles(), config.getTemperature(),
                config.getMaxTokens() + MARGE_RAISONNEMENT, config.isStreamingActive());
    }

    // L'inference reelle exige les deux : le moteur ET les poids. Il manque l'un des
    // deux -> mode autonome.
    public static boolean doitUtiliserLeRuntimeNatif(String runnerPath, Path modelPath) {
        return runnerPath != null && modelPath != null && Files.exists(modelPath);
    }

    // Construction de la ligne de commande passee a llama-cli
    public static List<String> construireCommande(String runnerPath, Path modelPath, String prompt, LlmConfig config) {
        List<String> command = new ArrayList<>();
        command.add(runnerPath);
        command.add("-m");
        command.add(modelPath.toAbsolutePath().toString());
        command.add("-p");
        command.add(prompt);
        command.add("-n");
        command.add(String.valueOf(config.getMaxTokens()));
        command.add("--temp");
        command.add(String.valueOf(config.getTemperature()));
        command.add("--no-display-prompt");
        // Un seul tour de conversation : le modele de chat du GGUF est applique,
        // puis le processus rend la main apres la reponse.
        command.add("-sys");
        command.add(CONSIGNE_AMELIORATION);
        command.add("-cnv");
        command.add("-st");
        return command;
    }

    // Delai au-dela duquel un llama-cli qui ne rend pas la main est considere comme bloque
    public static long delaiMaxInferenceSecondes = 600L;

    public GenerationResult runNativeInference(String runnerPath, Path modelPath, ModelType model, String prompt,
                                               LlmConfig config, TokenConsumer tokenConsumer, long startTime) throws Exception {
        List<String> command = construireCommande(runnerPath, modelPath, prompt, config);

        ProcessBuilder pb = new ProcessBuilder(command);
        // Les journaux de chargement de llama.cpp partent sur stderr : seule la reponse est gardee
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        // Sans entree, le mode conversation ne peut pas attendre une saisie
        pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
        // Les bibliotheques partagees (.so) sont livrees a cote du binaire precompile
        File dossierRunner = new File(runnerPath).getAbsoluteFile().getParentFile();
        if (dossierRunner != null) {
            String existant = System.getenv("LD_LIBRARY_PATH");
            pb.environment().put("LD_LIBRARY_PATH",
                    dossierRunner + (existant == null || existant.isBlank() ? "" : File.pathSeparator + existant));
        }
        Process process = pb.start();

        // Le blocage reel ne se produit pas sur waitFor mais sur la lecture du flux :
        // un moteur muet qui ne rend jamais la main laisserait le read suspendu indefiniment.
        // Un chien de garde tue donc le processus a l'echeance, ce qui debloque la lecture.
        java.util.concurrent.atomic.AtomicBoolean expire = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread chienDeGarde = new Thread(() -> {
            try {
                if (!process.waitFor(delaiMaxInferenceSecondes, TimeUnit.SECONDS)) {
                    expire.set(true);
                    // Les descendants gardent le tube de sortie ouvert : tuer le seul
                    // processus pere laisserait la lecture bloquee jusqu'a leur fin.
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                }
            } catch (InterruptedException interrompu) {
                Thread.currentThread().interrupt();
            }
        });
        chienDeGarde.setDaemon(true);
        chienDeGarde.start();

        StringBuilder fullOutput = new StringBuilder();
        int tokenCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            int ch;
            StringBuilder wordBuffer = new StringBuilder();
            // llama.cpp termine par le marqueur "[end of text]" : rien n'est diffuse a partir de lui
            boolean finDeTexte = false;
            while ((ch = reader.read()) != -1) {
                char c = (char) ch;
                fullOutput.append(c);
                wordBuffer.append(c);

                if (Character.isWhitespace(c) || c == '.' || c == '\n') {
                    String chunk = wordBuffer.toString();
                    finDeTexte = finDeTexte || chunk.trim().equals("[end");
                    if (tokenConsumer != null && !finDeTexte) {
                        tokenConsumer.accept(chunk);
                    }
                    tokenCount++;
                    wordBuffer.setLength(0);
                }
            }
            if (wordBuffer.length() > 0) {
                if (tokenConsumer != null && !finDeTexte) {
                    tokenConsumer.accept(wordBuffer.toString());
                }
                tokenCount++;
            }
        }

        process.waitFor();
        chienDeGarde.interrupt();

        // Le depassement de delai se diagnostique avant le code retour : un processus tue
        // par le chien de garde sort forcement avec un code non nul, qui masquerait la cause.
        if (expire.get()) {
            throw new IllegalStateException("Le moteur d'inference n'a pas rendu la main apres "
                    + delaiMaxInferenceSecondes + " s, processus interrompu.");
        }

        // Un code retour non nul ne doit pas etre presente comme une reponse du modele :
        // la sortie contiendrait un message d'erreur de llama-cli, affiche tel quel a l'utilisateur.
        int codeRetour = process.exitValue();
        if (codeRetour != 0) {
            throw new IllegalStateException("Le moteur d'inference a echoue (code " + codeRetour + ") : "
                    + resumerSortie(fullOutput.toString()));
        }

        long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
        double tps = (tokenCount * 1000.0) / elapsed;

        String texte = retirerRaisonnement(fullOutput.toString().replace("[end of text]", "")).trim();
        return new GenerationResult(texte, tokenCount, elapsed, tps, model);
    }

    // Les modeles de raisonnement (DeepSeek-R1) reflechissent a voix haute dans un bloc
    // <think>...</think> avant de repondre : seule la reponse qui suit est le resultat.
    // Un bloc jamais ferme (limite de tokens atteinte en pleine reflexion) ne contient
    // aucune reponse : le resultat est vide, et l'appelant se replie sur le NLP.
    public static String retirerRaisonnement(String sortie) {
        if (sortie == null) return "";
        int fin = sortie.lastIndexOf("</think>");
        if (fin >= 0) return sortie.substring(fin + "</think>".length());
        return sortie.stripLeading().startsWith("<think>") ? "" : sortie;
    }

    // Reduit la sortie du process a un extrait exploitable dans un message d'erreur
    private static String resumerSortie(String sortie) {
        String nettoyee = sortie == null ? "" : sortie.trim();
        if (nettoyee.isEmpty()) {
            return "aucune sortie";
        }
        return nettoyee.length() <= 200 ? nettoyee : nettoyee.substring(0, 200) + "...";
    }

    private GenerationResult runEmbeddedInference(ModelType model, String prompt, LlmConfig config,
                                                  TokenConsumer tokenConsumer, long startTime) {
        // Réponse structurée générée localement par le moteur selon le modèle
        String prefix = "🤖 [Simulation " + model.getNomAffiche()
                + " : moteur llama.cpp introuvable, placez-le dans llama/<version>/]\n\n";
        String body = switch (model) {
            case QWEN_CODER -> "Voici l'analyse et la solution technique optimisée :\n```java\n// Solution générée par Qwen 2.5 Coder\npublic class Solution {\n    // Code propre, modulaire et performant\n}\n```";
            case DEEPSEEK_REASONING -> "<think>\nAnalyse pas-à-pas des contraintes et des cas limites...\n</think>\n\nConclusion et recommandations étayées :";
            case GEMMA_GENERAL -> "Voici une explication claire et détaillée pour répondre à votre besoin :";
            case SMOLLM_FAST -> "Synthèse rapide :";
        };

        String fullText = prefix + body;
        String[] tokens = fullText.split("(?<=\\s)|(?<=[.,;!?])");

        int count = 0;
        for (String tok : tokens) {
            if (tokenConsumer != null) {
                tokenConsumer.accept(tok);
            }
            count++;
        }

        long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
        double tps = (count * 1000.0) / elapsed;

        return new GenerationResult(fullText, count, elapsed, tps, model);
    }
}
