package llm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Gestion de la configuration persistante et des autorisations d'inférence locale
public class LlmConfig {

    private static final String DEFAULT_CONFIG_DIR = ".llm_config";
    private static final String CONFIG_FILENAME = "settings.json";

    private boolean permissionAccordee;
    private String modeleParDefaut;
    private String repertoireModeles;
    private double temperature;
    private int maxTokens;
    private boolean streamingActive;

    public LlmConfig() {
        this.permissionAccordee = false;
        this.modeleParDefaut = "auto";
        this.repertoireModeles = "models";
        this.temperature = 0.7;
        this.maxTokens = 2048;
        this.streamingActive = true;
    }

    public LlmConfig(boolean permissionAccordee, String modeleParDefaut, String repertoireModeles,
                     double temperature, int maxTokens, boolean streamingActive) {
        this.permissionAccordee = permissionAccordee;
        this.modeleParDefaut = modeleParDefaut != null ? modeleParDefaut : "auto";
        this.repertoireModeles = repertoireModeles != null ? repertoireModeles : "models";
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.streamingActive = streamingActive;
    }

    public boolean isPermissionAccordee() {
        return permissionAccordee;
    }

    public void setPermissionAccordee(boolean permissionAccordee) {
        this.permissionAccordee = permissionAccordee;
    }

    public String getModeleParDefaut() {
        return modeleParDefaut;
    }

    public void setModeleParDefaut(String modeleParDefaut) {
        this.modeleParDefaut = modeleParDefaut;
    }

    public String getRepertoireModeles() {
        return repertoireModeles;
    }

    public void setRepertoireModeles(String repertoireModeles) {
        this.repertoireModeles = repertoireModeles;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isStreamingActive() {
        return streamingActive;
    }

    public void setStreamingActive(boolean streamingActive) {
        this.streamingActive = streamingActive;
    }

    // Sérialisation JSON simple sans dépendance externe
    public String toJson() {
        return "{\n" +
                "  \"permissionAccordee\": " + permissionAccordee + ",\n" +
                "  \"modeleParDefaut\": \"" + escapeJson(modeleParDefaut) + "\",\n" +
                "  \"repertoireModeles\": \"" + escapeJson(repertoireModeles) + "\",\n" +
                "  \"temperature\": " + temperature + ",\n" +
                "  \"maxTokens\": " + maxTokens + ",\n" +
                "  \"streamingActive\": " + streamingActive + "\n" +
                "}";
    }

    // Désérialisation JSON robuste et légère
    public static LlmConfig fromJson(String json) {
        LlmConfig config = new LlmConfig();
        if (json == null || json.isBlank()) {
            return config;
        }

        try {
            if (json.contains("\"permissionAccordee\"")) {
                config.setPermissionAccordee(extractBoolean(json, "permissionAccordee", false));
            }
            if (json.contains("\"modeleParDefaut\"")) {
                config.setModeleParDefaut(extractString(json, "modeleParDefaut", "auto"));
            }
            if (json.contains("\"repertoireModeles\"")) {
                config.setRepertoireModeles(extractString(json, "repertoireModeles", "models"));
            }
            if (json.contains("\"temperature\"")) {
                config.setTemperature(extractDouble(json, "temperature", 0.7));
            }
            if (json.contains("\"maxTokens\"")) {
                config.setMaxTokens(extractInt(json, "maxTokens", 2048));
            }
            if (json.contains("\"streamingActive\"")) {
                config.setStreamingActive(extractBoolean(json, "streamingActive", true));
            }
        } catch (Exception e) {
            // Fallback sur config par défaut en cas de corruption
        }

        return config;
    }

    public static Path getDefaultConfigPath() {
        return Paths.get(System.getProperty("user.dir"), DEFAULT_CONFIG_DIR, CONFIG_FILENAME);
    }

    public void sauvegarder(Path destination) throws IOException {
        if (destination.getParent() != null) {
            Files.createDirectories(destination.getParent());
        }
        Files.writeString(destination, toJson(), StandardCharsets.UTF_8);
    }

    public void sauvegarderParDefaut() throws IOException {
        sauvegarder(getDefaultConfigPath());
    }

    public static LlmConfig charger(Path chemin) {
        if (chemin != null && Files.exists(chemin)) {
            try {
                String contenu = Files.readString(chemin, StandardCharsets.UTF_8);
                return fromJson(contenu);
            } catch (IOException e) {
                // Fallback sur config par défaut
            }
        }
        return new LlmConfig();
    }

    public static LlmConfig chargerParDefaut() {
        return charger(getDefaultConfigPath());
    }

    // Utilitaires de parsing JSON légers
    private static String extractString(String json, String key, String def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        int startQuote = json.indexOf("\"", colon);
        if (startQuote == -1) return def;
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return def;
        return json.substring(startQuote + 1, endQuote);
    }

    private static boolean extractBoolean(String json, String key, boolean def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        int comma = json.indexOf(",", colon);
        int brace = json.indexOf("}", colon);
        int end = comma != -1 ? (brace != -1 ? Math.min(comma, brace) : comma) : brace;
        if (end == -1) end = json.length();
        String val = json.substring(colon + 1, end).trim();
        return Boolean.parseBoolean(val);
    }

    private static double extractDouble(String json, String key, double def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        int comma = json.indexOf(",", colon);
        int brace = json.indexOf("}", colon);
        int end = comma != -1 ? (brace != -1 ? Math.min(comma, brace) : comma) : brace;
        if (end == -1) end = json.length();
        String val = json.substring(colon + 1, end).trim();
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int extractInt(String json, String key, int def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return def;
        int colon = json.indexOf(":", idx);
        int comma = json.indexOf(",", colon);
        int brace = json.indexOf("}", colon);
        int end = comma != -1 ? (brace != -1 ? Math.min(comma, brace) : comma) : brace;
        if (end == -1) end = json.length();
        String val = json.substring(colon + 1, end).trim();
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
