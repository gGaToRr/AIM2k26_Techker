package test.util;

import test.framework.Assert;
import util.Log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

// Tests du journal minimal et de la tracabilite des exceptions non propagees (Issues #63, #64)
public class LogTest {

    private static String capturer(Log.Niveau niveau, Runnable action) {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        PrintStream flux = new PrintStream(tampon);
        Log.Niveau precedent = Log.niveau();
        try {
            Log.configurer(niveau);
            Log.brancherSortie(flux);
            action.run();
            flux.flush();
            return tampon.toString();
        } finally {
            Log.configurer(precedent);
            Log.brancherSortie(System.err);
        }
    }

    public void testAvertissementEmisAuNiveauParDefaut() {
        String sortie = capturer(Log.Niveau.AVERTISSEMENT, () -> Log.avertir("fichier absent"));
        Assert.assertContains(sortie, "AVERTISSEMENT", "Niveau etiquete");
        Assert.assertContains(sortie, "fichier absent", "Message present");
    }

    public void testDebugMuetTantQueLeModeVerbeuxEstInactif() {
        String sortie = capturer(Log.Niveau.AVERTISSEMENT, () -> Log.debug("detail interne"));
        Assert.assertNotContains(sortie, "detail interne", "Le debug ne doit pas polluer la sortie par defaut");
    }

    public void testDebugVisibleEnModeVerbeux() {
        String sortie = capturer(Log.Niveau.DEBUG, () -> Log.debug("detail interne"));
        Assert.assertContains(sortie, "detail interne", "Le mode verbeux libere le debug");
    }

    // Coeur de l'issue #63 : une exception non propagee doit laisser une trace exploitable
    public void testExceptionIgnoreeTraceTypeEtMessage() {
        String sortie = capturer(Log.Niveau.DEBUG,
                () -> Log.exceptionIgnoree("Lecture du template", new java.io.IOException("disque plein")));

        Assert.assertContains(sortie, "Lecture du template", "Le contexte de l'incident");
        Assert.assertContains(sortie, "IOException", "Le type de l'exception");
        Assert.assertContains(sortie, "disque plein", "Le message de l'exception");
    }

    public void testExceptionIgnoreeResteMuetteHorsModeVerbeux() {
        String sortie = capturer(Log.Niveau.AVERTISSEMENT,
                () -> Log.exceptionIgnoree("Lecture du template", new java.io.IOException("disque plein")));

        Assert.assertNotContains(sortie, "disque plein",
                "Le diagnostic detaille ne s'affiche qu'en mode verbeux");
    }

    public void testErreurJointLaCause() {
        String sortie = capturer(Log.Niveau.ERREUR,
                () -> Log.erreur("Lecture impossible", new IllegalStateException("etat invalide")));

        Assert.assertContains(sortie, "ERREUR", "Niveau etiquete");
        Assert.assertContains(sortie, "Lecture impossible", "Message present");
        Assert.assertContains(sortie, "etat invalide", "Cause jointe");
    }

    public void testNiveauSilencieuxCoupeTout() {
        String sortie = capturer(Log.Niveau.SILENCIEUX, () -> {
            Log.erreur("grave", new RuntimeException("boum"));
            Log.avertir("attention");
            Log.info("info");
        });
        Assert.assertEquals("", sortie, "Le niveau SILENCIEUX n'emet rien");
    }

    public void testConfigurationDepuisLeFlagVerbose() {
        Log.Niveau precedent = Log.niveau();
        try {
            Log.configurerDepuisVerbose(true);
            Assert.assertEquals(Log.Niveau.DEBUG, Log.niveau(), "-V active le niveau DEBUG");

            Log.configurerDepuisVerbose(false);
            Assert.assertEquals(Log.Niveau.AVERTISSEMENT, Log.niveau(), "Sans -V, seuil sur AVERTISSEMENT");
        } finally {
            Log.configurer(precedent);
        }
    }
}
