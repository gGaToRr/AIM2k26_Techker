package test.cli;

import test.framework.TestRunner;

// Suite de tests dédiée à l'ensemble des fonctionnalités et arguments CLI
public class CLITestSuite {

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();

        runner.registerTestClass(CliParserTest.class);
        runner.registerTestClass(CliSyntaxParsingTest.class);
        runner.registerTestClass(CliOutputAndExportTest.class);
        runner.registerTestClass(CliSemanticOverridesTest.class);
        runner.registerTestClass(CliRobustnessAndEdgeCasesTest.class);
        runner.registerTestClass(CliClipboardCacheTest.class);

        boolean success = runner.runAll();
        if (!success) {
            System.exit(1);
        }
    }
}
