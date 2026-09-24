package test.llm;

import llm.AmeliorationCommand;
import llm.LlmBackend;
import llm.LlmConfig;
import llm.ModelType;
import llm.ModelsCommand;
import test.framework.AfterEach;
import test.framework.Assert;
import test.framework.BeforeEach;
import test.framework.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

// Amelioration appelee par l'extension a chaque envoi de prompt (--improve-json)
public class AmeliorationCommandTest {

    private Path repertoire;
    private ByteArrayOutputStream tampon;
    private PrintStream sortie;

    // Reponse type d'un modele : sections presentees a sa facon (titres sans accents, gras)
    private static final String REPONSE_STRUCTUREE = "Voici le prompt :\nRole : Tu es un expert Python.\n"
            + "Contexte : Besoin d'une fonction de tri.\n**Tache :** Ecris une fonction Python de tri.\n"
            + "Contraintes :\n* Python 3\nFormat de sortie attendu : Le code de la fonction.";

    // Backend espion : repond un texte fixe et compte les appels
    private static final class BackendEspion implements LlmBackend {
        int appels = 0;
        String promptRecu;
        @Override public boolean isAvailable(ModelType model, String modelsDir) { return true; }
        @Override public GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer c) {
            appels++;
            promptRecu = prompt;
            return new GenerationResult(REPONSE_STRUCTUREE, 5, 10, 1.0, model);
        }
    }

    @BeforeEach
    public void preparer() throws Exception {
        repertoire = Files.createTempDirectory("amelioration_test");
        tampon = new ByteArrayOutputStream();
        sortie = new PrintStream(tampon, true, StandardCharsets.UTF_8);
    }

    @AfterEach
    public void nettoyer() throws Exception {
        try (var flux = Files.walk(repertoire)) {
            flux.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        }
    }

    private LlmConfig config() {
        LlmConfig config = new LlmConfig();
        config.setRepertoireModeles(repertoire.toString());
        config.setPermissionAccordee(true);
        return config;
    }

    private int executer(String message, BackendEspion backend, boolean moteur) {
        return AmeliorationCommand.executer(new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)),
                sortie, config(), backend, moteur);
    }

    @Test
    public void testPromptAmelioreRenvoyeEnJson() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        BackendEspion backend = new BackendEspion();

        int code = executer("{\"action\":\"ameliorer\",\"prompt\":\"fais une fonction python de tri\"}", backend, true);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Succes");
        Assert.assertEquals(1, backend.appels, "Une generation");
        Assert.assertContains(backend.promptRecu, "fais une fonction python de tri", "Le prompt brut atteint le modele");
        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "\"ok\":true", "Succes signale");
        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "\"source\":\"modele\"", "Source modele");
        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8),
                "\"ameliore\":\"## Rôle\\nTu es un expert Python.\\n\\n## Contexte\\nBesoin d'une fonction de tri.",
                "Texte ameliore au format commun, echappe");
    }

    // Appelee a chaque envoi : ne doit jamais declencher un telechargement de plus d'1 Go.
    // Sans modele, repli sur le meta-prompt NLP, signale comme tel.
    @Test
    public void testSansModeleInstalleRepliNlp() {
        BackendEspion backend = new BackendEspion();
        int code = executer("{\"prompt\":\"fais une fonction python de tri\"}", backend, true);
        String json = tampon.toString(StandardCharsets.UTF_8);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Succes grace au repli");
        Assert.assertEquals(0, backend.appels, "Aucune generation");
        Assert.assertContains(json, "\"source\":\"nlp\"", "Source NLP annoncee");
        Assert.assertContains(json, "\"raison\":\"aucun modele installe\"", "Raison du repli");
        Assert.assertContains(json, "fais une fonction python de tri", "Le meta-prompt reprend la demande");
    }

    // Sans moteur, le backend repondrait un texte simule : repli NLP plutot que fausse amelioration
    @Test
    public void testSansMoteurRepliNlpSansReponseSimulee() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        BackendEspion backend = new BackendEspion();
        int code = executer("{\"prompt\":\"bonjour\"}", backend, false);
        String json = tampon.toString(StandardCharsets.UTF_8);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Succes grace au repli");
        Assert.assertEquals(0, backend.appels, "Aucune generation");
        Assert.assertContains(json, "\"source\":\"nlp\"", "Source NLP annoncee");
        Assert.assertContains(json, "moteur llama.cpp introuvable", "Raison du repli");
    }

    @Test
    public void testPromptVideRefuse() {
        BackendEspion backend = new BackendEspion();
        int code = executer("{\"prompt\":\"   \"}", backend, true);

        Assert.assertEquals(ModelsCommand.ECHEC, code, "Echec");
        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "prompt vide", "Cause explicite");
    }

    // Reglages de l'extension : IA cible et format appliques au texte du modele
    @Test
    public void testCibleEtFormatAppliquesAuTexteDuModele() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        BackendEspion backend = new BackendEspion();

        executer("{\"prompt\":\"fais une fonction python de tri\",\"format\":\"txt\",\"cible\":\"claude\",\"langue\":\"auto\"}",
                backend, true);
        String json = tampon.toString(StandardCharsets.UTF_8);

        Assert.assertContains(json, "<claude_system_prompt>", "Adapte a Claude");
        Assert.assertContains(json, "Rôle\\nTu es un expert Python.", "Texte du modele conserve, sans balisage");
        Assert.assertContains(backend.promptRecu, "sections en Français", "Langue auto : celle du prompt brut");
    }

    @Test
    public void testLangueImposeeAuModele() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        BackendEspion backend = new BackendEspion();

        executer("{\"prompt\":\"fais une fonction python de tri\",\"langue\":\"es\"}", backend, true);

        Assert.assertContains(backend.promptRecu, "sections en Español", "Consigne de langue");
        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "## Rol\\n", "Titres dans la langue imposee");
    }

    @Test
    public void testFormatJsonEnveloppeLePrompt() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        executer("{\"prompt\":\"fais une fonction python de tri\",\"format\":\"json\"}", new BackendEspion(), true);

        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "\\\"meta_prompt\\\":", "Export JSON dans le champ ameliore");
    }

    // Repli NLP : le meta-prompt suit aussi les reglages
    @Test
    public void testRepliNlpSuitLesReglages() {
        executer("{\"prompt\":\"fais une fonction python de tri\",\"cible\":\"gpt\",\"langue\":\"en\"}", new BackendEspion(), true);
        String json = tampon.toString(StandardCharsets.UTF_8);

        Assert.assertContains(json, "\"source\":\"nlp\"", "Repli NLP");
        Assert.assertContains(json, "SYSTEM ROLE", "Adapte a GPT");
    }

    // Backend espion qui note le modele employe
    private static final class BackendModele implements LlmBackend {
        ModelType modeleRecu;
        @Override public boolean isAvailable(ModelType model, String modelsDir) { return true; }
        @Override public GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer c) {
            modeleRecu = model;
            return new GenerationResult("Role : expert", 2, 10, 1.0, model);
        }
    }

    // Reglage "Modele utilise" : le modele choisi est employe s'il est installe
    @Test
    public void testModeleChoisiEmploye() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        Files.createFile(repertoire.resolve(ModelType.SMOLLM_FAST.getNomFichier()));
        BackendModele backend = new BackendModele();

        AmeliorationCommand.executer(new ByteArrayInputStream(
                "{\"prompt\":\"fais une fonction python de tri\",\"modele\":\"smollm\"}".getBytes(StandardCharsets.UTF_8)),
                sortie, config(), backend, true);

        Assert.assertEquals(ModelType.SMOLLM_FAST, backend.modeleRecu, "Modele impose respecte");
    }

    // Modele choisi mais absent : jamais de telechargement, un modele installe prend le relais
    @Test
    public void testModeleChoisiAbsentSansTelechargement() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        BackendModele backend = new BackendModele();

        AmeliorationCommand.executer(new ByteArrayInputStream(
                "{\"prompt\":\"fais une fonction python de tri\",\"modele\":\"gemma\"}".getBytes(StandardCharsets.UTF_8)),
                sortie, config(), backend, true);

        Assert.assertEquals(ModelType.QWEN_CODER, backend.modeleRecu, "Repli sur le modele installe");
        Assert.assertFalse(Files.exists(repertoire.resolve(ModelType.GEMMA_GENERAL.getNomFichier())), "Rien telecharge");
    }

    // Backend espion : garde la configuration recue par la generation
    private static final class BackendConfig implements LlmBackend {
        double temperature;
        int maxTokens;
        @Override public boolean isAvailable(ModelType model, String modelsDir) { return true; }
        @Override public GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer c) {
            temperature = config.getTemperature();
            maxTokens = config.getMaxTokens();
            return new GenerationResult("Role : expert", 5, 10, 1.0, model);
        }
    }

    // Reglages "Creativite" et "Longueur maximale" : transmis au modele local
    @Test
    public void testCreativiteEtLongueurTransmises() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        BackendConfig backend = new BackendConfig();

        AmeliorationCommand.executer(new ByteArrayInputStream(
                "{\"prompt\":\"fais une fonction python de tri\",\"creativite\":\"0.2\",\"longueurMax\":\"512\"}"
                        .getBytes(StandardCharsets.UTF_8)), sortie, config(), backend, true);

        Assert.assertEquals(0.2, backend.temperature, "Temperature appliquee");
        Assert.assertEquals(512, backend.maxTokens, "Limite de tokens appliquee");
    }

    // Valeurs hors bornes ou illisibles : la configuration par defaut est gardee
    @Test
    public void testReglagesModeleInvalidesIgnores() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        BackendConfig backend = new BackendConfig();

        AmeliorationCommand.executer(new ByteArrayInputStream(
                "{\"prompt\":\"fais une fonction python de tri\",\"creativite\":\"5\",\"longueurMax\":\"beaucoup\"}"
                        .getBytes(StandardCharsets.UTF_8)), sortie, config(), backend, true);

        Assert.assertEquals(0.7, backend.temperature, "Temperature par defaut");
        Assert.assertEquals(2048, backend.maxTokens, "Limite par defaut");
    }

    // Backend qui repond un texte fixe
    private static LlmBackend repondant(String texte) {
        return new LlmBackend() {
            @Override public boolean isAvailable(ModelType model, String modelsDir) { return true; }
            @Override public GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer c) {
                return new GenerationResult(texte, 5, 10, 1.0, model);
            }
        };
    }

    // Mots-cles en vrac (SmolLM) : pas un prompt structure, repli NLP
    @Test
    public void testReponseNonStructureeRepliNlp() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        AmeliorationCommand.executer(new ByteArrayInputStream(
                "{\"prompt\":\"fais une fonction python de tri\"}".getBytes(StandardCharsets.UTF_8)),
                sortie, config(), repondant("Fonction python, tri, liste, rapide."), true);
        String json = tampon.toString(StandardCharsets.UTF_8);

        Assert.assertContains(json, "\"source\":\"nlp\"", "Repli NLP");
        Assert.assertContains(json, "reponse du modele non structuree", "Raison du repli");
    }

    // Prompt bien presente mais sur un autre sujet : pire que le repli NLP
    @Test
    public void testReponseHorsSujetRepliNlp() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        String horsSujet = "Rôle :\nTu es un expert en jardinage.\nContexte :\nUn potager au printemps.\n"
                + "Tâche :\nPlanifie les semis de tomates.\nContraintes :\n- Sol argileux";
        AmeliorationCommand.executer(new ByteArrayInputStream(("{\"prompt\":\"mon script python qui lit un fichier "
                + "csv plante avec MemoryError, j'utilise pandas read_csv\"}").getBytes(StandardCharsets.UTF_8)),
                sortie, config(), repondant(horsSujet), true);

        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "reponse du modele hors sujet", "Repli NLP");
    }

    // Prompt initial de moins de 100 caracteres : avertissement transmis a l'extension
    @Test
    public void testAvertissementPromptCourt() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        executer("{\"prompt\":\"fais une fonction python de tri\"}", new BackendEspion(), true);

        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8),
                "\"avertissement\":\"Attention votre prompt initial contient trop peu d'information.\"", "Avertissement");
    }

    @Test
    public void testPasDAvertissementPourUnPromptDetaille() throws Exception {
        Files.createFile(repertoire.resolve(ModelType.QWEN_CODER.getNomFichier()));
        executer("{\"prompt\":\"fais une fonction python de tri qui trie une liste de dictionnaires par date "
                + "puis par nom, sans bibliotheque externe\"}", new BackendEspion(), true);

        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "\"avertissement\":null", "Pas d'avertissement");
    }

    // Le prompt de reference trouve dans la base est transmis au modele, qui s'en inspire
    @Test
    public void testReferenceTransmiseAuModele() {
        nlp.PromptProfile profil = nlp.Lemmatizer.analyser("recette de lasagnes");
        String demande = llm.LlmEngine.construireDemandeAmelioration(profil, null,
                "Donne-moi une recette de lasagnes végétariennes pour 6 personnes");
        Assert.assertContains(demande, "Prompt de référence", "Section de reference");
        Assert.assertContains(demande, "lasagnes végétariennes pour 6 personnes", "Texte de la reference");
        Assert.assertContains(demande, "N'en recopie pas les faits propres", "Consigne d'inspiration");
        Assert.assertNotContains(llm.LlmEngine.construireDemandeAmelioration(profil, null, null), "Prompt de référence",
                "Sans base : pas de section");
    }
}
