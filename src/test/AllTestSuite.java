package test;

import test.framework.TestRunner;
import test.gen.MetaPromptEngineTest;
import test.gen.TemplateLoaderTest;
import test.gen.TemplateSyntaxValidationTest;
import test.integration.EndToEndIntegrationTest;
import test.llm.*;
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
        runner.registerTestClass(SafetyAdvisorTest.class);
        runner.registerTestClass(PromptClassifierTest.class);

        // 2. Tests GenPrompt
        runner.registerTestClass(TemplateLoaderTest.class);
        runner.registerTestClass(MetaPromptEngineTest.class);
        runner.registerTestClass(TemplateSyntaxValidationTest.class);

        // 3. Tests Menu & Arguments CLI
        runner.registerTestClass(MenuTest.class);
        runner.registerTestClass(test.cli.CliParserTest.class);
        runner.registerTestClass(test.cli.CliSyntaxParsingTest.class);
        runner.registerTestClass(test.cli.CliOutputAndExportTest.class);
        runner.registerTestClass(test.cli.CliSemanticOverridesTest.class);
        runner.registerTestClass(test.cli.CliRobustnessAndEdgeCasesTest.class);

        // 4. Tests Inférence Locale & Routage LLM
        runner.registerTestClass(ModelTypeTest.class);
        runner.registerTestClass(ModelRouterTest.class);
        runner.registerTestClass(LlmConfigTest.class);
        runner.registerTestClass(ModelInstallerTest.class);
        runner.registerTestClass(RuntimeInstallerTest.class);
        runner.registerTestClass(LlmEngineTest.class);

        // 5. Tests d'Intégration End-to-End
        runner.registerTestClass(EndToEndIntegrationTest.class);

        boolean success = runner.runAll();
        if (!success) {
            System.exit(1);
        }
    }
}
