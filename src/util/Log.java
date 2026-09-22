package util;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

// Journal minimal de l'application, bati sur java.util.logging.
//
// Deux sorties bien distinctes coexistent dans ce projet :
//   - la sortie standard, qui EST le produit (prompt genere, onboarding, barre de progression) ;
//   - le diagnostic, destine a comprendre un echec, qui part ici et donc sur stderr.
// Melanger les deux casserait les usages en pipe (`Main -r | pbcopy`).
public final class Log {

    public enum Niveau {
        SILENCIEUX(Level.OFF),
        ERREUR(Level.SEVERE),
        AVERTISSEMENT(Level.WARNING),
        INFO(Level.INFO),
        DEBUG(Level.FINE);

        private final Level niveauJul;

        Niveau(Level niveauJul) {
            this.niveauJul = niveauJul;
        }

        Level versJul() {
            return niveauJul;
        }
    }

    private static final Logger JOURNAL = Logger.getLogger("aim2k26");
    private static StreamHandler handler;
    private static Niveau niveauCourant = Niveau.AVERTISSEMENT;

    static {
        JOURNAL.setUseParentHandlers(false);
        brancherSortie(System.err);
        configurer(Niveau.AVERTISSEMENT);
    }

    private Log() {}

    // Definit le seuil de verbosite, independamment des flags CLI
    public static synchronized void configurer(Niveau niveau) {
        niveauCourant = niveau != null ? niveau : Niveau.AVERTISSEMENT;
        JOURNAL.setLevel(niveauCourant.versJul());
        if (handler != null) {
            handler.setLevel(niveauCourant.versJul());
        }
    }

    // Raccourci branche sur le flag -V/--verbose
    public static void configurerDepuisVerbose(boolean verbeux) {
        configurer(verbeux ? Niveau.DEBUG : Niveau.AVERTISSEMENT);
    }

    public static Niveau niveau() {
        return niveauCourant;
    }

    // Redirige le journal vers un autre flux (tests, ou capture applicative)
    public static synchronized void brancherSortie(PrintStream flux) {
        if (handler != null) {
            handler.flush();
            JOURNAL.removeHandler(handler);
        }
        handler = new StreamHandler(flux, new FormateurCompact());
        handler.setLevel(niveauCourant.versJul());
        JOURNAL.addHandler(handler);
    }

    public static void debug(String message) {
        ecrire(Level.FINE, message, null);
    }

    public static void info(String message) {
        ecrire(Level.INFO, message, null);
    }

    public static void avertir(String message) {
        ecrire(Level.WARNING, message, null);
    }

    public static void erreur(String message, Throwable cause) {
        ecrire(Level.SEVERE, message, cause);
    }

    // Trace une exception que l'appelant a choisi de ne pas propager.
    // Le flux nominal continue, mais l'incident cesse d'etre invisible.
    public static void exceptionIgnoree(String contexte, Throwable cause) {
        if (cause == null) {
            ecrire(Level.FINE, contexte, null);
            return;
        }
        ecrire(Level.FINE, contexte + " -> " + cause.getClass().getSimpleName()
                + (cause.getMessage() != null ? " : " + cause.getMessage() : ""), null);
    }

    private static synchronized void ecrire(Level niveau, String message, Throwable cause) {
        if (!JOURNAL.isLoggable(niveau)) {
            return;
        }
        LogRecord enregistrement = new LogRecord(niveau, message);
        if (cause != null) {
            enregistrement.setThrown(cause);
        }
        JOURNAL.log(enregistrement);
        if (handler != null) {
            handler.flush();
        }
    }

    // Format compact sur une ligne : le journal doit rester lisible dans un terminal
    private static final class FormateurCompact extends java.util.logging.Formatter {
        @Override
        public String format(LogRecord enregistrement) {
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(etiquette(enregistrement.getLevel())).append("] ")
              .append(formatMessage(enregistrement)).append(System.lineSeparator());
            if (enregistrement.getThrown() != null) {
                sb.append("    cause : ").append(enregistrement.getThrown()).append(System.lineSeparator());
            }
            return sb.toString();
        }

        private String etiquette(Level niveau) {
            if (niveau == Level.SEVERE) return "ERREUR";
            if (niveau == Level.WARNING) return "AVERTISSEMENT";
            if (niveau == Level.INFO) return "INFO";
            return "DEBUG";
        }
    }
}
