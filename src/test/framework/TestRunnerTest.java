package test.framework;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

// Tests du framework de test lui-meme : selection des cas, hooks et isolation (Issue #68)
public class TestRunnerTest {

    // --- Classes fixtures, analysees par reflexion ---

    // Classe migree : l'annotation fait autorite
    public static class FixtureAnnotee {
        @Test public void testUn() {}
        @Test public void testDeux() {}
        // Piege de l'issue : methode utilitaire nommee comme un test, mais non annotee
        public void testUtilitaireQuiNEnEstPasUn() {
            throw new IllegalStateException("cette methode ne doit jamais etre executee comme un test");
        }
        @Test public void aaaNomSansPrefixe() {}
    }

    // Classe non migree : l'ancienne convention doit continuer a fonctionner
    public static class FixtureParConvention {
        public void testAlpha() {}
        public void testBeta() {}
        public void methodeOrdinaire() {}
    }

    private static List<String> nomsDesCas(Class<?> fixture) {
        List<String> noms = new ArrayList<>();
        for (Method m : TestRunner.collecterCasDeTest(fixture)) {
            noms.add(m.getName());
        }
        return noms;
    }

    @Test
    public void testAnnotationFaitAutoriteSurLaConvention() {
        List<String> cas = nomsDesCas(FixtureAnnotee.class);

        Assert.assertSize(cas, 3, "Seules les methodes annotees sont retenues");
        Assert.assertTrue(cas.contains("aaaNomSansPrefixe"),
                "Une methode annotee sans prefixe 'test' doit etre retenue");
        Assert.assertFalse(cas.contains("testUtilitaireQuiNEnEstPasUn"),
                "Une methode nommee testXxx mais non annotee ne doit pas etre executee");
    }

    @Test
    public void testRepliSurLaConventionPourUneClasseNonMigree() {
        List<String> cas = nomsDesCas(FixtureParConvention.class);

        Assert.assertSize(cas, 2, "Repli sur le prefixe quand aucune annotation n'est presente");
        Assert.assertFalse(cas.contains("methodeOrdinaire"), "Les autres methodes restent ignorees");
    }

    @Test
    public void testOrdreDeterministeDesCas() {
        // getDeclaredMethods ne garantit aucun ordre : deux appels doivent donner le meme rapport
        Assert.assertEquals(nomsDesCas(FixtureAnnotee.class), nomsDesCas(FixtureAnnotee.class),
                "Ordre stable d'une execution a l'autre");
        Assert.assertEquals(List.of("aaaNomSansPrefixe", "testDeux", "testUn"), nomsDesCas(FixtureAnnotee.class),
                "Cas tries par nom");
    }

    // --- Hooks et isolation, verifies en executant reellement une fixture ---

    public static class FixtureAvecHooks {
        static final List<String> JOURNAL = new ArrayList<>();
        static int instances = 0;

        public FixtureAvecHooks() { instances++; }

        @BeforeEach public void preparer() { JOURNAL.add("avant"); }
        @AfterEach  public void nettoyer() { JOURNAL.add("apres"); }

        @Test public void testA() { JOURNAL.add("A"); }
        @Test public void testB() { JOURNAL.add("B"); }
    }

    @Test
    public void testHooksEncadrentChaqueCas() {
        FixtureAvecHooks.JOURNAL.clear();
        FixtureAvecHooks.instances = 0;

        executerSilencieusement(FixtureAvecHooks.class);

        Assert.assertEquals(List.of("avant", "A", "apres", "avant", "B", "apres"),
                FixtureAvecHooks.JOURNAL, "setUp et tearDown encadrent chaque cas, pas la classe");
    }

    @Test
    public void testInstanceNeuveParCasDeTest() {
        FixtureAvecHooks.JOURNAL.clear();
        FixtureAvecHooks.instances = 0;

        executerSilencieusement(FixtureAvecHooks.class);

        Assert.assertEquals(2, FixtureAvecHooks.instances,
                "Une instance par cas : l'etat d'un test ne fuit pas dans le suivant");
    }

    public static class FixtureQuiEchoue {
        static final List<String> JOURNAL = new ArrayList<>();

        @AfterEach public void nettoyer() { JOURNAL.add("nettoyage"); }
        @Test public void testQuiEchoue() { throw new IllegalStateException("echec volontaire"); }
    }

    @Test
    public void testTearDownExecuteMemeApresUnEchec() {
        FixtureQuiEchoue.JOURNAL.clear();

        executerSilencieusement(FixtureQuiEchoue.class);

        Assert.assertEquals(List.of("nettoyage"), FixtureQuiEchoue.JOURNAL,
                "Le nettoyage doit avoir lieu meme si le cas a echoue");
    }

    // Execute une fixture sans polluer le rapport de la suite reelle
    private static void executerSilencieusement(Class<?> fixture) {
        java.io.PrintStream original = System.out;
        try {
            System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
            TestRunner runner = new TestRunner();
            runner.registerTestClass(fixture);
            runner.runAll();
        } finally {
            System.setOut(original);
        }
    }
}
