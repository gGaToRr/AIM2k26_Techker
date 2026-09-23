package test.gen;

import test.framework.TestRunner;

// Suite de tests complète pour le module GenPrompt
public class GenPromptTestSuite {

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.registerTestClass(TemplateLoaderTest.class);
        runner.registerTestClass(MetaPromptEngineTest.class);
        runner.registerTestClass(AdapterPourAgentTest.class);
        runner.registerTestClass(TemplateSyntaxValidationTest.class);

        boolean success = runner.runAll();
        if (!success) {
            System.exit(1);
        }
    }
}
