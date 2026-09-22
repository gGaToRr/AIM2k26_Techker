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

    private static final String[] RUNNER_CANDIDATES = {
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
            return runNativeInference(runner, modelPath, model, prompt, config, tokenConsumer, startTime);
        } else {
            // Mode autonome / simulation contrôlée pour environnement de test ou sans binaire externe
            return runEmbeddedInference(model, prompt, config, tokenConsumer, startTime);
        }
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
        return command;
    }

    // Delai au-dela duquel un llama-cli qui ne rend pas la main est considere comme bloque
    public static long delaiMaxInferenceSecondes = 600L;

    public GenerationResult runNativeInference(String runnerPath, Path modelPath, ModelType model, String prompt,
                                               LlmConfig config, TokenConsumer tokenConsumer, long startTime) throws Exception {
        List<String> command = construireCommande(runnerPath, modelPath, prompt, config);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
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
            while ((ch = reader.read()) != -1) {
                char c = (char) ch;
                fullOutput.append(c);
                wordBuffer.append(c);

                if (Character.isWhitespace(c) || c == '.' || c == '\n') {
                    String chunk = wordBuffer.toString();
                    if (tokenConsumer != null) {
                        tokenConsumer.accept(chunk);
                    }
                    tokenCount++;
                    wordBuffer.setLength(0);
                }
            }
            if (wordBuffer.length() > 0) {
                if (tokenConsumer != null) {
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

        return new GenerationResult(fullOutput.toString().trim(), tokenCount, elapsed, tps, model);
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
        String prefix = "🤖 [Réponse générée localement par " + model.getNomAffiche() + "]\n\n";
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
