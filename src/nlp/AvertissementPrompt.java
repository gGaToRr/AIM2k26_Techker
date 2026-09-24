package nlp;

import java.util.Optional;

// Avertit quand le prompt initial est trop court pour etre bien ameliore : en dessous
// de quelques phrases, le modele doit deviner le contexte et les attentes.
public final class AvertissementPrompt {

    public static final int LONGUEUR_MINIMALE = 100;
    public static final String MESSAGE_PROMPT_COURT =
            "Attention votre prompt initial contient trop peu d'information.";

    private AvertissementPrompt() {}

    // Espaces de debut et de fin exclus : ils n'apportent aucune information
    public static Optional<String> verifier(String prompt) {
        String texte = prompt == null ? "" : prompt.strip();
        return texte.codePointCount(0, texte.length()) < LONGUEUR_MINIMALE
                ? Optional.of(MESSAGE_PROMPT_COURT)
                : Optional.empty();
    }
}
