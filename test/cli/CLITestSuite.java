package test.cli;

import test.framework.TestRunner;

// Suite de tests dédiée aux composants CLI (Parser, Options, Bannières)
public class CLITestSuite {

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.registerTestClass(CliParserTest.class);

        boolean success = runner.runAll();
        if (!success) {
            System.exit(1);
        }
    }
}
