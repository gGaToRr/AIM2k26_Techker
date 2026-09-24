package test.llm;

import test.framework.TestRunner;

// Suite de tests dédiée au module d'inférence et de routage LLM
public class LlmTestSuite {

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();

        runner.registerTestClass(ModelTypeTest.class);
        runner.registerTestClass(ModelRouterTest.class);
        runner.registerTestClass(LlmConfigTest.class);
        runner.registerTestClass(ModelInstallerTest.class);
        runner.registerTestClass(LlmEngineTest.class);
        runner.registerTestClass(LocalLlmBackendTest.class);
        runner.registerTestClass(ModelManagerTest.class);
        runner.registerTestClass(RuntimeInstallerTest.class);

        boolean success = runner.runAll();
        if (!success) {
            System.exit(1);
        }
    }
}
