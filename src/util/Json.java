package util;

// Echappement minimal pour ecrire du JSON a la main (le projet n'a pas de bibliotheque JSON)
public final class Json {

    private Json() {}

    public static String echapper(String texte) {
        if (texte == null) return "";
        return texte.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String chaine(String texte) {
        return texte == null ? "null" : "\"" + echapper(texte) + "\"";
    }

    // Lit la valeur texte d'une cle dans un objet JSON simple, echappements compris
    // (retours a la ligne, guillemets, codes unicode...). Renvoie null si la cle est absente ou n'est pas un texte.
    // Suffisant pour les messages de l'extension, sans bibliotheque JSON.
    public static String extraireChaine(String json, String cle) {
        if (json == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(cle) + "\"\\s*:\\s*\"").matcher(json);
        if (!m.find()) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = m.end(); i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (++i >= json.length()) return null;
            char e = json.charAt(i);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (i + 4 >= json.length()) return null;
                    sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> sb.append(e); // \" \\ \/
            }
        }
        return null; // chaine non terminee
    }
}
