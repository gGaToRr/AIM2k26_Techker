package test.llm;

import llm.ModelType;
import nlp.TypeOfPrompt;
import test.framework.Assert;

import java.util.Optional;

// Tests unitaires pour le registre de modèles ModelType
public class ModelTypeTest {

    public void testModelConstantsIntegrity() {
        ModelType qwen = ModelType.QWEN_CODER;
        Assert.assertEquals("qwen-coder", qwen.getId(), "ID de Qwen");
        Assert.assertContains(qwen.getNomAffiche(), "Qwen", "Nom affiché");
        Assert.assertNotNull(qwen.getUrlTelechargement(), "URL non nulle");
        Assert.assertTrue(qwen.getNomFichier().endsWith(".gguf"), "Fichier GGUF");
        Assert.assertTrue(qwen.getArchetypesCibles().contains(TypeOfPrompt.CONCEPTION_ARCHITECTURE), "Cible Architecture");

        ModelType gemma = ModelType.GEMMA_GENERAL;
        Assert.assertEquals("gemma-general", gemma.getId(), "ID de Gemma");
        Assert.assertTrue(gemma.getArchetypesCibles().contains(TypeOfPrompt.APPRENTISSAGE_TUTORIEL), "Cible Tutoriel");

        ModelType deepseek = ModelType.DEEPSEEK_REASONING;
        Assert.assertEquals("deepseek-reasoning", deepseek.getId(), "ID de DeepSeek");
        Assert.assertTrue(deepseek.getArchetypesCibles().contains(TypeOfPrompt.COMPARAISON_DECISION), "Cible Décision");

        ModelType smol = ModelType.SMOLLM_FAST;
        Assert.assertEquals("smollm-fast", smol.getId(), "ID de SmolLM");
    }

    public void testAliasResolution() {
        Assert.assertEquals(ModelType.QWEN_CODER, ModelType.fromAlias("qwen").orElseThrow(), "Alias qwen");
        Assert.assertEquals(ModelType.QWEN_CODER, ModelType.fromAlias("coder").orElseThrow(), "Alias coder");
        Assert.assertEquals(ModelType.QWEN_CODER, ModelType.fromAlias("code").orElseThrow(), "Alias code");

        Assert.assertEquals(ModelType.GEMMA_GENERAL, ModelType.fromAlias("gemma").orElseThrow(), "Alias gemma");
        Assert.assertEquals(ModelType.GEMMA_GENERAL, ModelType.fromAlias("general").orElseThrow(), "Alias general");
        Assert.assertEquals(ModelType.GEMMA_GENERAL, ModelType.fromAlias("redaction").orElseThrow(), "Alias redaction");

        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, ModelType.fromAlias("deepseek").orElseThrow(), "Alias deepseek");
        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, ModelType.fromAlias("r1").orElseThrow(), "Alias r1");
        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, ModelType.fromAlias("reasoning").orElseThrow(), "Alias reasoning");

        Assert.assertEquals(ModelType.SMOLLM_FAST, ModelType.fromAlias("smollm").orElseThrow(), "Alias smollm");
        Assert.assertEquals(ModelType.SMOLLM_FAST, ModelType.fromAlias("fast").orElseThrow(), "Alias fast");
        Assert.assertEquals(ModelType.SMOLLM_FAST, ModelType.fromAlias("rapide").orElseThrow(), "Alias rapide");
    }

    public void testUnknownAndNullAliases() {
        Assert.assertTrue(ModelType.fromAlias(null).isEmpty(), "Null alias doit renvoyer empty");
        Assert.assertTrue(ModelType.fromAlias("   ").isEmpty(), "Blank alias doit renvoyer empty");
        Assert.assertTrue(ModelType.fromAlias("modele_inconnu_xyz_123").isEmpty(), "Inconnu doit renvoyer empty");
    }

    public void testGetAllAvailable() {
        Assert.assertEquals(4, ModelType.getAllAvailable().size(), "Exactement 4 modèles légers configurés");
    }
}
