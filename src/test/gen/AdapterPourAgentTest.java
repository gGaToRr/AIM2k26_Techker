package test.gen;

import gen.MetaPromptEngine;
import test.framework.Assert;
import test.framework.Test;

// Tests de l'adaptation du prompt aux conventions de chaque agent cible (Issue #57)
public class AdapterPourAgentTest {

    private static final String PROMPT = "# ROLE\nTu es un expert.\n<instruction_principale>\nTrie ce tableau.\n</instruction_principale>";

    // --- Invariant commun a tous les agents ---

    // Adapter ne doit jamais perdre le prompt : l'enveloppe s'ajoute, elle ne remplace pas
    @Test
    public void testLePromptOriginalEstToujoursPreserve() {
        String[] agents = {"claude", "deepseek", "gpt", "gemini", "llama", "feynman", "inconnu", ""};
        for (String agent : agents) {
            String adapte = MetaPromptEngine.adapterPourAgent(PROMPT, agent);
            Assert.assertContains(adapte, "Trie ce tableau.",
                    "Le contenu du prompt doit survivre a l'adaptation pour " + agent);
            Assert.assertTrue(adapte.length() >= PROMPT.length(),
                    "L'adaptation ajoute une enveloppe, elle ne tronque jamais pour " + agent);
        }
    }

    // --- Agents reconnus et leurs alias ---

    @Test
    public void testClaudeEtSonAliasAnthropic() {
        for (String alias : new String[]{"claude", "anthropic", "Claude", "  CLAUDE  ", "claude-opus"}) {
            String adapte = MetaPromptEngine.adapterPourAgent(PROMPT, alias);
            Assert.assertContains(adapte, "<claude_system_prompt>", "Balise Claude pour l'alias " + alias);
            Assert.assertContains(adapte, "<thinking_instructions>", "Instructions de reflexion pour " + alias);
        }
    }

    @Test
    public void testDeepseekEtSonAliasR1() {
        for (String alias : new String[]{"deepseek", "r1", "DeepSeek-R1"}) {
            String adapte = MetaPromptEngine.adapterPourAgent(PROMPT, alias);
            Assert.assertContains(adapte, "<think>", "Balises de raisonnement pour " + alias);
        }
    }

    @Test
    public void testGptEtSesAlias() {
        for (String alias : new String[]{"gpt", "openai", "chatgpt", "GPT-4o"}) {
            String adapte = MetaPromptEngine.adapterPourAgent(PROMPT, alias);
            Assert.assertContains(adapte, "SYSTEM ROLE", "En-tete systeme pour " + alias);
        }
    }

    @Test
    public void testGeminiEtSonAliasGoogle() {
        for (String alias : new String[]{"gemini", "google"}) {
            String adapte = MetaPromptEngine.adapterPourAgent(PROMPT, alias);
            Assert.assertContains(adapte, "SYSTEM INSTRUCTIONS", "En-tete systeme pour " + alias);
            Assert.assertContains(adapte, "Markdown", "Consigne de formatage pour " + alias);
        }
    }

    // Llama utilise des jetons speciaux qui doivent encadrer le prompt, pas seulement le preceder
    @Test
    public void testLlamaEncadreLePromptAvecSesJetons() {
        String adapte = MetaPromptEngine.adapterPourAgent(PROMPT, "llama");

        Assert.assertTrue(adapte.startsWith("<|begin_of_text|>"), "Jeton d'ouverture en tete");
        Assert.assertTrue(adapte.endsWith("<|start_header_id|>assistant<|end_header_id|>"),
                "Le prompt se termine en cedant la parole a l'assistant");
        Assert.assertContains(adapte, "<|eot_id|>", "Jetons de fin de tour");

        int positionPrompt = adapte.indexOf("Trie ce tableau.");
        int positionEnTeteAssistant = adapte.indexOf("<|start_header_id|>assistant");
        Assert.assertTrue(positionPrompt < positionEnTeteAssistant,
                "Le prompt doit se situer dans le tour utilisateur, avant la main donnee a l'assistant");
    }

    @Test
    public void testAliasMetaEquivautALlama() {
        Assert.assertEquals(MetaPromptEngine.adapterPourAgent(PROMPT, "llama"),
                MetaPromptEngine.adapterPourAgent(PROMPT, "meta"),
                "meta et llama produisent la meme enveloppe");
    }

    @Test
    public void testFeynman() {
        String adapte = MetaPromptEngine.adapterPourAgent(PROMPT, "feynman");
        Assert.assertContains(adapte, "MÉTHODE FEYNMAN", "En-tete de la methode");
        Assert.assertContains(adapte, "12 ans", "Consigne de vulgarisation");
    }

    // --- Cas neutres : aucune adaptation ---

    @Test
    public void testAgentInconnuRendLePromptInchange() {
        Assert.assertEquals(PROMPT, MetaPromptEngine.adapterPourAgent(PROMPT, "mistral"),
                "Un agent non gere ne doit rien modifier");
        Assert.assertEquals(PROMPT, MetaPromptEngine.adapterPourAgent(PROMPT, "agent_imaginaire_42"),
                "Aucune enveloppe par defaut");
    }

    @Test
    public void testAgentNulOuVideRendLePromptInchange() {
        Assert.assertEquals(PROMPT, MetaPromptEngine.adapterPourAgent(PROMPT, null), "Agent nul");
        Assert.assertEquals(PROMPT, MetaPromptEngine.adapterPourAgent(PROMPT, ""), "Agent vide");
        Assert.assertEquals(PROMPT, MetaPromptEngine.adapterPourAgent(PROMPT, "    "), "Agent en blanc");
    }

    // --- Non-idempotence : appliquer deux fois empile deux enveloppes ---

    // Documente le comportement reel : adapterPourAgent n'est pas idempotent.
    // Le pipeline ne l'appelle qu'une fois ; ce test fige cette contrainte.
    @Test
    public void testAdaptationAppliqueeDeuxFoisEmpileLesEnveloppes() {
        String uneFois = MetaPromptEngine.adapterPourAgent(PROMPT, "claude");
        String deuxFois = MetaPromptEngine.adapterPourAgent(uneFois, "claude");

        Assert.assertEquals(2, compterOccurrences(deuxFois, "<claude_system_prompt>"),
                "Deux appels produisent deux enveloppes : la methode n'est pas idempotente");
    }

    // --- Agents distincts, enveloppes distinctes ---

    @Test
    public void testChaqueAgentProduitUneEnveloppeDistincte() {
        String claude = MetaPromptEngine.adapterPourAgent(PROMPT, "claude");
        String gpt = MetaPromptEngine.adapterPourAgent(PROMPT, "gpt");
        String gemini = MetaPromptEngine.adapterPourAgent(PROMPT, "gemini");
        String llama = MetaPromptEngine.adapterPourAgent(PROMPT, "llama");

        Assert.assertNotEquals(claude, gpt, "Claude et GPT different");
        Assert.assertNotEquals(gpt, gemini, "GPT et Gemini different");
        Assert.assertNotEquals(gemini, llama, "Gemini et Llama different");
        Assert.assertNotContains(gpt, "<claude_system_prompt>", "Pas de fuite d'une enveloppe a l'autre");
        Assert.assertNotContains(claude, "SYSTEM ROLE", "Pas de fuite d'une enveloppe a l'autre");
    }

    // Un prompt vide reste gerable sans exception
    @Test
    public void testPromptVideNeLevePasDException() {
        String adapte = MetaPromptEngine.adapterPourAgent("", "claude");
        Assert.assertContains(adapte, "<claude_system_prompt>", "L'enveloppe est posee meme sur un prompt vide");
    }

    private static int compterOccurrences(String texte, String motif) {
        int total = 0;
        int index = texte.indexOf(motif);
        while (index >= 0) {
            total++;
            index = texte.indexOf(motif, index + motif.length());
        }
        return total;
    }
}
