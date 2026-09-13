package test.menu;

import menu.Menu;
import test.framework.Assert;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class MenuTest {

    public void testSaisieMultiligneAvecValidationDoneMajusculesEtMinuscules() {
        String inputDoneMin = "Première ligne\nDeuxième ligne\n:done\n";
        Menu menu1 = new Menu(new ByteArrayInputStream(inputDoneMin.getBytes(StandardCharsets.UTF_8)));
        String res1 = menu1.demanderPrompt();
        Assert.assertContains(res1, "Première ligne", "Ligne 1 lue");
        Assert.assertContains(res1, "Deuxième ligne", "Ligne 2 lue");
        Assert.assertFalse(res1.contains(":done"), ":done exclu");

        String inputDoneMaj = "Ligne A\nLigne B\n:DONE\n";
        Menu menu2 = new Menu(new ByteArrayInputStream(inputDoneMaj.getBytes(StandardCharsets.UTF_8)));
        String res2 = menu2.demanderPrompt();
        Assert.assertContains(res2, "Ligne A", "Ligne A lue");
        Assert.assertFalse(res2.contains(":DONE"), ":DONE exclu");
    }

    public void testSaisieMultiligneAvecValidationEnd() {
        String input = "Bloc de texte introductif\nSuite du texte\n:END\n";
        Menu menu = new Menu(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        String res = menu.demanderPrompt();
        Assert.assertContains(res, "Bloc de texte introductif", "Intro lue");
        Assert.assertFalse(res.contains(":END"), ":END exclu");
    }

    public void testPreservationCodeAvecDeuxPointsInternes() {
        String code = "const config = { host: 'localhost', port: 8080 };\nconsole.log(config);\n:done\n";
        Menu menu = new Menu(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)));
        String res = menu.demanderPrompt();
        Assert.assertContains(res, "host: 'localhost'", "Les deux-points internes dans le code sont préservés");
        Assert.assertContains(res, "port: 8080", "Le port est préservé");
    }

    public void testCollageBlocDeCodeAvecIndentationEtAccolades() {
        String code = "public class App {\n    public static void main(String[] args) {\n        System.out.println(\"Test\");\n    }\n}\n:done\n";
        Menu menu = new Menu(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)));
        String res = menu.demanderPrompt();
        Assert.assertContains(res, "public class App", "Entête de classe");
        Assert.assertContains(res, "    public static void main", "Indentation 4 espaces préservée");
        Assert.assertContains(res, "}", "Accolades préservées");
    }
}
