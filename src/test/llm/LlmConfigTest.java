package test.llm;

import llm.LlmConfig;
import test.framework.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

// Tests unitaires pour la gestion de configuration et persistance LlmConfig
public class LlmConfigTest {

    public void testDefaultConfigValues() {
        LlmConfig config = new LlmConfig();
        Assert.assertFalse(config.isPermissionAccordee(), "Permission non accordée par défaut");
        Assert.assertEquals("auto", config.getModeleParDefaut(), "Modèle par défaut");
        Assert.assertEquals("models", config.getRepertoireModeles(), "Dossier modèles");
        Assert.assertEquals("runtime", config.getRepertoireRuntime(), "Dossier runtime natif");
        Assert.assertTrue(Math.abs(0.7 - config.getTemperature()) < 0.001, "Température");
        Assert.assertEquals(2048, config.getMaxTokens(), "Max tokens");
        Assert.assertTrue(config.isStreamingActive(), "Streaming actif par défaut");
    }

    public void testJsonSerializationAndDeserialization() {
        LlmConfig original = new LlmConfig(true, "qwen-coder", "custom_models", 0.5, 1024, false);
        String json = original.toJson();

        Assert.assertContains(json, "\"permissionAccordee\": true", "JSON permission");
        Assert.assertContains(json, "\"modeleParDefaut\": \"qwen-coder\"", "JSON modèle");
        Assert.assertContains(json, "\"repertoireModeles\": \"custom_models\"", "JSON répertoire");

        LlmConfig parsed = LlmConfig.fromJson(json);
        Assert.assertTrue(parsed.isPermissionAccordee(), "Parsé permission");
        Assert.assertEquals("qwen-coder", parsed.getModeleParDefaut(), "Parsé modèle");
        Assert.assertEquals("custom_models", parsed.getRepertoireModeles(), "Parsé dossier");
        Assert.assertTrue(Math.abs(0.5 - parsed.getTemperature()) < 0.001, "Parsé température");
        Assert.assertEquals(1024, parsed.getMaxTokens(), "Parsé maxTokens");
        Assert.assertFalse(parsed.isStreamingActive(), "Parsé streaming");
    }

    public void testSaveAndLoadFromFile() throws Exception {
        Path tempFile = Files.createTempFile("llm_config_test", ".json");
        try {
            LlmConfig config = new LlmConfig(true, "deepseek-reasoning", "models_dir", 0.8, 4096, true);
            config.sauvegarder(tempFile);

            Assert.assertTrue(Files.exists(tempFile), "Fichier créé");

            LlmConfig loaded = LlmConfig.charger(tempFile);
            Assert.assertTrue(loaded.isPermissionAccordee(), "Loaded permission");
            Assert.assertEquals("deepseek-reasoning", loaded.getModeleParDefaut(), "Loaded modèle");
            Assert.assertEquals("models_dir", loaded.getRepertoireModeles(), "Loaded dossier");
            Assert.assertTrue(Math.abs(0.8 - loaded.getTemperature()) < 0.001, "Loaded temp");
            Assert.assertEquals(4096, loaded.getMaxTokens(), "Loaded tokens");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testRepertoireRuntimePersisteEnJson() {
        LlmConfig original = new LlmConfig();
        original.setRepertoireRuntime("custom_runtime");
        String json = original.toJson();

        Assert.assertContains(json, "\"repertoireRuntime\": \"custom_runtime\"", "JSON dossier runtime");

        LlmConfig parsed = LlmConfig.fromJson(json);
        Assert.assertEquals("custom_runtime", parsed.getRepertoireRuntime(), "Parsé dossier runtime");
    }

    public void testCorruptedOrEmptyJsonResilience() {
        LlmConfig empty = LlmConfig.fromJson("");
        Assert.assertNotNull(empty, "JSON vide renvoie une config non nulle");

        LlmConfig corrupted = LlmConfig.fromJson("{invalid json non parseable}");
        Assert.assertNotNull(corrupted, "JSON corrompu renvoie une config par défaut sans crasher");

        LlmConfig fromNull = LlmConfig.charger(null);
        Assert.assertNotNull(fromNull, "Fichier null renvoie config par défaut");
    }
}
