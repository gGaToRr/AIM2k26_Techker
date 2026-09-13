package test.gen;

import test.framework.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TemplateSyntaxValidationTest {

    private static final Set<String> DOSSIERS_AUTORISES = Set.of(
            "learning",
            "architecture",
            "troubleshooting",
            "creation",
            "protocol",
            "comparison",
            "concept"
    );

    public void testIntegriteEtSyntaxeTousLesTemplatesMarkdown() throws IOException {
        Path rootGenPrompt = Path.of("src", "genPrompt");
        if (!Files.exists(rootGenPrompt)) {
            rootGenPrompt = Path.of("genPrompt");
        }
        Assert.assertTrue(Files.exists(rootGenPrompt), "Le dossier genPrompt doit exister (dans src/genPrompt ou genPrompt)");

        try (Stream<Path> stream = Files.walk(rootGenPrompt)) {
            List<Path> markdownFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .toList();

            Assert.assertGreaterThanOrEqual(14, markdownFiles.size(), "Au moins 14 templates Markdown dans genPrompt/");

            for (Path templatePath : markdownFiles) {
                String fileName = templatePath.getFileName().toString();
                String parentFolder = templatePath.getParent().getFileName().toString();
                String content = Files.readString(templatePath);

                // 1. Dossier parent conforme
                Assert.assertTrue(DOSSIERS_AUTORISES.contains(parentFolder), "Le template " + fileName + " est dans un dossier autorisé (" + parentFolder + ")");

                // 2. Taille minimale
                Assert.assertGreaterThanOrEqual(300, content.length(), "Taille du template " + fileName + " >= 300 caractères");

                // 3. Sections requises
                Assert.assertContains(content, "# RÔLE & EXPERTISE", "Section # RÔLE & EXPERTISE dans " + fileName);
                Assert.assertContains(content, "<format_de_sortie>", "Balise <format_de_sortie> dans " + fileName);
                Assert.assertContains(content, "</format_de_sortie>", "Balise fermante </format_de_sortie> dans " + fileName);

                // 4. Équilibre strict des tags Mustache {{ et }}
                int countOpen = countOccurrences(content, "{{");
                int countClose = countOccurrences(content, "}}");
                Assert.assertEquals(countOpen, countClose, "Équilibre {{ et }} dans " + fileName);

                // 5. Équilibre des sections Mustache conditionnelles {{#tag}} et {{/tag}}
                verifierEquilibreSectionsMustache(content, fileName);
            }
        }
    }

    private void verifierEquilibreSectionsMustache(String content, String fileName) {
        Pattern blockPattern = Pattern.compile("\\{\\{([#/\\^])([a-zA-Z0-9_]+)\\}\\}");
        Matcher matcher = blockPattern.matcher(content);

        java.util.Stack<String> stack = new java.util.Stack<>();
        while (matcher.find()) {
            String type = matcher.group(1);
            String tag = matcher.group(2);

            if (type.equals("#") || type.equals("^")) {
                stack.push(tag);
            } else if (type.equals("/")) {
                Assert.assertFalse(stack.isEmpty(), "Fermeture de tag inattendue {{/" + tag + "}} dans " + fileName);
                String lastOpened = stack.pop();
                Assert.assertEquals(lastOpened, tag, "Le tag fermant {{/" + tag + "}} ne correspond pas au tag ouvrant {{" + lastOpened + "}} dans " + fileName);
            }
        }

        Assert.assertTrue(stack.isEmpty(), "Toutes les sections conditionnelles Mustache doivent être fermées dans " + fileName + " (reste: " + stack + ")");
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
