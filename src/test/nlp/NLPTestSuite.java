package test.nlp;

import test.framework.TestRunner;

// Suite de tests complète pour le module NLP
public class NLPTestSuite {

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.registerTestClass(SanitizerTest.class);
        runner.registerTestClass(LemmatizerTest.class);
        runner.registerTestClass(TechStackDetectorTest.class);
        runner.registerTestClass(DomainExtractorTest.class);
        runner.registerTestClass(QuestionDecomposerTest.class);
        runner.registerTestClass(TokenCounterTest.class);
        runner.registerTestClass(PromptQualityScorerTest.class);
        runner.registerTestClass(PromptClassifierTest.class);
        runner.registerTestClass(AvertissementPromptTest.class);
        runner.registerTestClass(NlpCorpusReelTest.class);
        runner.registerTestClass(ThemeClassifierTest.class);
        runner.registerTestClass(SafetyAdvisorTest.class);

        boolean success = runner.runAll();
        if (!success) {
            System.exit(1);
        }
    }
}
