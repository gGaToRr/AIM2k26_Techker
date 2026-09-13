package test;

import test.framework.TestRunner;
import test.gen.MetaPromptEngineTest;
import test.gen.TemplateLoaderTest;
import test.gen.TemplateSyntaxValidationTest;
import test.integration.EndToEndIntegrationTest;
import test.menu.MenuTest;
import test.nlp.*;

// Suite globale exécutant l'intégralité des tests TDD du projet
public class AllTestSuite {

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();

        // 1. Tests NLP
        runner.registerTestClass(SanitizerTest.class);
        runner.registerTestClass(LemmatizerTest.class);
        runner.registerTestClass(TechStackDetectorTest.class);
        runner.registerTestClass(DomainExtractorTest.class);
        runner.registerTestClass(QuestionDecomposerTest.class);
        runner.registerTestClass(TokenCounterTest.class);
        runner.registerTestClass(PromptQualityScorerTest.class);
        runner.registerTestClass(PromptClassifierTest.class);

        // 2. Tests GenPrompt
        runner.registerTestClass(TemplateLoaderTest.class);
        runner.registerTestClass(MetaPromptEngineTest.class);
        runner.registerTestClass(TemplateSyntaxValidationTest.class);

        // 3. Tests Menu CLI & Intégration End-to-End
        runner.registerTestClass(MenuTest.class);
        runner.registerTestClass(EndToEndIntegrationTest.class);

        boolean success = runner.runAll();
        if (!success) {
            System.exit(1);
        }
    }
}
