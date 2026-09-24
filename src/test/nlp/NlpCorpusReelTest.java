package test.nlp;

import nlp.DomainExtractor;
import nlp.Lemmatizer;
import nlp.TechStackDetector;
import test.framework.Assert;
import test.framework.Test;

// Non-regression tiree de l'analyse de 470 000 vrais prompts (corpus oasst2 + WildChat) :
// chaque cas est un faux positif ou une erreur reellement observee.
public class NlpCorpusReelTest {

    private static String langue(String prompt) {
        return Lemmatizer.detecterLangue(prompt, Lemmatizer.tokeniser(prompt));
    }

    // 31 % des prompts anglais etaient detectes en francais : les mots outils anglais
    // ("the", "to", "for") etaient retires avant la detection
    @Test
    public void testPromptsAnglaisCourts() {
        Assert.assertEquals("EN", langue("10011001 binary number to number"), "to");
        Assert.assertEquals("EN", langue("write top and bottom text for a meme of a man holding a burnt hot dog"), "Meme");
        Assert.assertEquals("EN", langue("suppose you are a senior software developer specialize in rabbitMQ"), "you are");
        Assert.assertEquals("EN", langue("is it ok to exit currently running threads using detachthread()"), "is it");
    }

    // L'espagnol et l'allemand sortaient en francais a 95-99 %
    @Test
    public void testEspagnolEtAllemand() {
        Assert.assertEquals("ES", langue("Dame los pasos de las cosas que debería de aprender para ser un desarrollador"), "Espagnol");
        Assert.assertEquals("ES", langue("¿Cómo puedo hacer una tortilla?"), "Ponctuation espagnole");
        Assert.assertEquals("DE", langue("Kannst du mir bitte erklären, wie das funktioniert?"), "Allemand");
    }

    @Test
    public void testFrancaisConserve() {
        Assert.assertEquals("FR", langue("fais une fonction python de tri"), "Mots outils francais");
        Assert.assertEquals("FR", langue("site web restaurant menu"), "Sans indice : francais par defaut");
        Assert.assertEquals("FR", langue("Explique-moi la récursivité"), "Accent");
    }

    // "of course" classait les prompts en Sport, "composition" (prompts d'images) en Musique
    @Test
    public void testFauxAmisDesDomaines() {
        Assert.assertFalse(domaine("Of course, can you rewrite this paragraph in a formal tone?").startsWith("Sciences du Sport"),
                "of course n'est pas une course");
        Assert.assertFalse(domaine("a castle at dawn, dramatic composition, cinematic lighting, 8k").startsWith("Musique"),
                "composition d'image");
        Assert.assertFalse(domaine("I have a pain in my knee after running").startsWith("Gastronomie"), "pain anglais");
        Assert.assertEquals("Gastronomie & Arts Culinaires", domaine("Comment faire du pain maison au levain ?"), "pain francais");
    }

    private static String domaine(String prompt) {
        return DomainExtractor.analyser(prompt, Lemmatizer.detecterLangue(prompt, java.util.List.of())).domainName();
    }

    // "unreal engine" comme style d'image classait 10 % du corpus en Game Dev
    @Test
    public void testTechnologiesAmbigues() {
        Assert.assertFalse(TechStackDetector.detecterTechnologies(
                "portrait of a knight, unreal engine 5, octane render, highly detailed").contains("Unity / Game Dev"),
                "Style d'image, pas du developpement");
        Assert.assertTrue(TechStackDetector.detecterTechnologies("how to code minecraft in unity")
                .contains("Unity / Game Dev"), "Vrai developpement de jeu");
        Assert.assertFalse(TechStackDetector.detecterTechnologies("how did people react to the news?")
                .contains("JavaScript / TypeScript"), "react verbe");
        Assert.assertTrue(TechStackDetector.detecterTechnologies("create a react component with a button")
                .contains("JavaScript / TypeScript"), "React framework");
    }
}
