package test.cli;

import cli.CliArgs;
import cli.CliParser;
import gen.MetaPromptEngine;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

// Tests d'intégration des surcharges sémantiques (Templates, Domaines, Langues, Agents IA) (Issue #33)
public class CliSemanticOverridesTest {

    // Test du forçage de templates spécifiques sur différents archétypes
    public void testTemplateOverrideAcrossArchetypes() {
        PromptProfile profile = Lemmatizer.analyser("Bonjour");

        // 1. Template Apprentissage Débutant
        CliArgs opt1 = CliParser.parse(new String[]{"-t", "guide_debutant"});
        String p1 = MetaPromptEngine.genererPromptOptimise(profile, opt1);
        Assert.assertContains(p1, "directives_pedagogiques", "Doit charger le template guide_debutant");
        Assert.assertContains(p1, "Débutant", "Doit mentionner le niveau Débutant");

        // 2. Template Aide à la décision
        CliArgs opt2 = CliParser.parse(new String[]{"-t", "aide_decision"});
        String p2 = MetaPromptEngine.genererPromptOptimise(profile, opt2);
        Assert.assertContains(p2, "directives_decisionnelles", "Doit charger le template aide_decision");

        // 3. Template Audit & Revue de code
        CliArgs opt3 = CliParser.parse(new String[]{"-t", "audit_review"});
        String p3 = MetaPromptEngine.genererPromptOptimise(profile, opt3);
        Assert.assertContains(p3, "Auditeur de Code", "Doit charger le template audit_review");

        // 4. Template Storytelling
        CliArgs opt4 = CliParser.parse(new String[]{"-t", "storytelling"});
        String p4 = MetaPromptEngine.genererPromptOptimise(profile, opt4);
        Assert.assertContains(p4, "regles_narratives", "Doit charger le template storytelling");
    }

    // Test du forçage de persona et domaine métier
    public void testDomainOverrideWithKnownAndCustomDomains() {
        PromptProfile profile = Lemmatizer.analyser("Explique les mécanismes");

        // Domaine connu : Gastronomie
        CliArgs optGastro = CliParser.parse(new String[]{"-d", "gastronomie"});
        String pGastro = MetaPromptEngine.genererPromptOptimise(profile, optGastro);
        Assert.assertContains(pGastro, "Chef Étoilé", "Doit injecter le persona Chef Étoilé");

        // Domaine connu : Médecine
        CliArgs optMed = CliParser.parse(new String[]{"-d", "medecine"});
        String pMed = MetaPromptEngine.genererPromptOptimise(profile, optMed);
        Assert.assertContains(pMed, "Médecin et Chercheur", "Doit injecter le persona Médical");

        // Domaine sur-mesure
        CliArgs optCustom = CliParser.parse(new String[]{"-d", "Blockchain & Cryptographie Post-Quantique"});
        String pCustom = MetaPromptEngine.genererPromptOptimise(profile, optCustom);
        Assert.assertContains(pCustom, "Blockchain & Cryptographie Post-Quantique", "Doit injecter le domaine sur-mesure");
    }

    // Test de la spécification de langues cibles (FR, EN, ES, DE)
    public void testLanguageOverrideTargetLanguages() {
        PromptProfile profile = Lemmatizer.analyser("Bonjour");

        CliArgs optFr = CliParser.parse(new String[]{"-l", "fr"});
        String pFr = MetaPromptEngine.genererPromptOptimise(profile, optFr);
        Assert.assertContains(pFr, "Français", "Doit définir Français");

        CliArgs optEn = CliParser.parse(new String[]{"-l", "en"});
        String pEn = MetaPromptEngine.genererPromptOptimise(profile, optEn);
        Assert.assertContains(pEn, "English", "Doit définir English");

        CliArgs optEs = CliParser.parse(new String[]{"-l", "es"});
        String pEs = MetaPromptEngine.genererPromptOptimise(profile, optEs);
        Assert.assertContains(pEs, "Español", "Doit définir Español");

        CliArgs optDe = CliParser.parse(new String[]{"-l", "de"});
        String pDe = MetaPromptEngine.genererPromptOptimise(profile, optDe);
        Assert.assertContains(pDe, "Deutsch", "Doit définir Deutsch");
    }

    // Test de l'adaptation du prompt pour l'intégralité des 6 modèles IA cibles
    public void testTargetAgentFormattingAllModels() {
        PromptProfile profile = Lemmatizer.analyser("Rédiger un script de sauvegarde automatique");

        // 1. Claude
        String pClaude = MetaPromptEngine.genererPromptOptimise(profile, CliParser.parse(new String[]{"-a", "claude"}));
        Assert.assertContains(pClaude, "<claude_system_prompt>", "Claude prompt tag");
        Assert.assertContains(pClaude, "<thinking_instructions>", "Claude thinking tag");

        // 2. DeepSeek
        String pDeepSeek = MetaPromptEngine.genererPromptOptimise(profile, CliParser.parse(new String[]{"-a", "deepseek"}));
        Assert.assertContains(pDeepSeek, "DeepSeek-V3", "DeepSeek prompt header");
        Assert.assertContains(pDeepSeek, "<think>", "DeepSeek think tag");

        // 3. OpenAI GPT-4o
        String pGpt = MetaPromptEngine.genererPromptOptimise(profile, CliParser.parse(new String[]{"-a", "gpt-4o"}));
        Assert.assertContains(pGpt, "OpenAI GPT-4o", "OpenAI GPT prompt header");

        // 4. Google Gemini
        String pGemini = MetaPromptEngine.genererPromptOptimise(profile, CliParser.parse(new String[]{"-a", "gemini"}));
        Assert.assertContains(pGemini, "Google Gemini", "Gemini prompt header");

        // 5. Meta LLaMA-3
        String pLlama = MetaPromptEngine.genererPromptOptimise(profile, CliParser.parse(new String[]{"-a", "llama3"}));
        Assert.assertContains(pLlama, "<|start_header_id|>system<|end_header_id|>", "LLaMA-3 system header");
        Assert.assertContains(pLlama, "<|start_header_id|>user<|end_header_id|>", "LLaMA-3 user header");

        // 6. Feynman
        String pFeynman = MetaPromptEngine.genererPromptOptimise(profile, CliParser.parse(new String[]{"-a", "feynman"}));
        Assert.assertContains(pFeynman, "MISSION MÉTHODE FEYNMAN EXPLICITE", "Feynman header");
    }
}
