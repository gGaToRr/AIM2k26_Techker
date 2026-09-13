package test.menu;

import menu.Menu;
import test.framework.Assert;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class MenuTest {

    public void testSaisieMultiligneAvecValidationDone() {
        String input = "Ligne 1 : Présentation du projet\nLigne 2 : Détails techniques\n:done\n";
        Menu menu = new Menu(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        String result = menu.demanderPrompt();
        Assert.assertContains(result, "Ligne 1 : Présentation du projet", "Doit contenir la ligne 1");
        Assert.assertContains(result, "Ligne 2 : Détails techniques", "Doit contenir la ligne 2");
        Assert.assertFalse(result.contains(":done"), "Le mot-clé :done ne doit pas faire partie du prompt");
    }

    public void testSaisieMultiligneAvecValidationEnd() {
        String input = "Premiere partie\nDeuxieme partie\n:end\n";
        Menu menu = new Menu(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        String result = menu.demanderPrompt();
        Assert.assertContains(result, "Premiere partie", "Doit contenir la première partie");
        Assert.assertContains(result, "Deuxieme partie", "Doit contenir la deuxième partie");
        Assert.assertFalse(result.contains(":end"), "Le mot-clé :end ne doit pas faire partie du prompt");
    }

    public void testCollageBlocDeCode() {
        String code = "public class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}\n:done\n";
        Menu menu = new Menu(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)));

        String result = menu.demanderPrompt();
        Assert.assertContains(result, "public class HelloWorld", "Le code source doit être préservé");
        Assert.assertContains(result, "System.out.println", "Le corps de méthode doit être préservé");
    }

    public void testSaisieUniligneAvecRetourLigne() {
        String input = "Comment faire une quiche lorraine ?\n\n";
        Menu menu = new Menu(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        String result = menu.demanderPrompt();
        Assert.assertContains(result, "Comment faire une quiche lorraine ?", "La question uniligne doit être lue");
    }
}
