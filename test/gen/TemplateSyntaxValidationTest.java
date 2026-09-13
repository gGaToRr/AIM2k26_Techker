package test.gen;

import test.framework.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class TemplateSyntaxValidationTest {

    public void testIntegriteSyntaxiqueTousLesTemplates() throws IOException {
        Path rootGenPrompt = Path.of("genPrompt");
        Assert.assertTrue(Files.exists(rootGenPrompt), "Le dossier genPrompt doit exister");

        try (Stream<Path> stream = Files.walk(rootGenPrompt)) {
            List<Path> markdownFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .toList();

            Assert.assertTrue(!markdownFiles.isEmpty(), "Le dossier genPrompt doit contenir des templates Markdown");

            for (Path templatePath : markdownFiles) {
                String content = Files.readString(templatePath);
                String fileName = templatePath.getFileName().toString();

                Assert.assertTrue(!content.isBlank(), "Le template " + fileName + " ne doit pas être vide");
                Assert.assertContains(content, "# RÔLE & EXPERTISE", "Le template " + fileName + " doit contenir la section # RÔLE & EXPERTISE");
                Assert.assertContains(content, "<format_de_sortie>", "Le template " + fileName + " doit contenir la balise <format_de_sortie>");

                // Vérification de l'équilibre des tags Mustache {{ et }}
                int countOpen = countOccurrences(content, "{{");
                int countClose = countOccurrences(content, "}}");
                Assert.assertEquals(countOpen, countClose, "Les balises Mustache {{ et }} doivent être équilibrées dans " + fileName);
            }
        }
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }
}
