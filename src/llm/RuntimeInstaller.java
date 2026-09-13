package llm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// Installateur automatique du runtime natif d'inférence (llama-cli / llama.cpp)
public class RuntimeInstaller {

    private static final String GITHUB_RELEASES_URL =
            "https://api.github.com/repos/ggml-org/llama.cpp/releases?per_page=1";

    // Variantes accélérées à exclure : nécessitent des drivers/matériel spécifiques
    // non garantis présents sur la machine de l'utilisateur. On privilégie le build CPU.
    private static final List<String> MOTS_CLES_EXCLUS =
            List.of("cuda", "vulkan", "sycl", "rocm", "openvino", "opencl");

    public record RuntimeAsset(String name, String url) {
    }

    // Résout le répertoire du runtime, avec valeur par défaut si absent
    private static String normaliserRepertoire(String runtimeDir) {
        return (runtimeDir != null && !runtimeDir.isBlank()) ? runtimeDir : "runtime";
    }

    // Détermine la plateforme courante (OS + architecture) au format "os-arch"
    public static String detecterPlateforme() {
        return detecterPlateforme(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    public static String detecterPlateforme(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);

        boolean isArm64 = arch.contains("aarch64") || arch.contains("arm64");
        boolean isX64 = arch.contains("amd64") || arch.contains("x86_64") || arch.equals("x64");

        String osKey;
        if (os.contains("linux")) {
            osKey = "linux";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osKey = "macos";
        } else if (os.contains("win")) {
            osKey = "windows";
        } else {
            return "inconnu";
        }

        if (isArm64) {
            return osKey + "-arm64";
        } else if (isX64) {
            return osKey + "-x64";
        }
        return "inconnu";
    }

    // Choisit l'asset de release le plus adapté (build CPU simple, sans dépendance GPU/driver)
    public static String choisirAsset(List<String> nomsAssets, String plateforme) {
        if (nomsAssets == null || plateforme == null || plateforme.equals("inconnu")) {
            return null;
        }

        String[] parts = plateforme.split("-");
        if (parts.length != 2) {
            return null;
        }
        String osToken = switch (parts[0]) {
            case "linux" -> "ubuntu";
            case "windows" -> "win";
            default -> parts[0];
        };
        String archToken = parts[1];

        String meilleur = null;
        for (String nom : nomsAssets) {
            String lower = nom.toLowerCase(Locale.ROOT);

            if (!lower.startsWith("llama-") || !lower.contains("-bin-")) {
                continue;
            }
            if (!lower.contains(osToken) || !lower.contains(archToken)) {
                continue;
            }
            boolean exclu = MOTS_CLES_EXCLUS.stream().anyMatch(lower::contains);
            if (exclu) {
                continue;
            }

            if (meilleur == null || nom.length() < meilleur.length()) {
                meilleur = nom;
            }
        }
        return meilleur;
    }

    // Extraction légère (regex) du tag de version depuis la réponse JSON de l'API GitHub
    public static String extraireTagVersion(String json) {
        if (json == null) return null;
        Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    // Extraction des assets {name, browser_download_url} depuis le JSON de l'API GitHub.
    // Isole d'abord chaque objet asset par équilibrage d'accolades avant d'y chercher les champs,
    // car chaque asset contient un objet "uploader" imbriqué (avec ses propres { }) entre "name"
    // et "browser_download_url" : une simple regex de proximité s'y casse les dents.
    public static List<RuntimeAsset> extraireAssetsDepuisJson(String json) {
        List<RuntimeAsset> assets = new ArrayList<>();
        if (json == null) return assets;

        int assetsIdx = json.indexOf("\"assets\"");
        if (assetsIdx == -1) return assets;
        int arrayStart = json.indexOf('[', assetsIdx);
        if (arrayStart == -1) return assets;

        String arrayContent = extraireBlocEquilibre(json, arrayStart, '[', ']');
        if (arrayContent == null) return assets;

        for (String objet : decouperObjetsDeNiveauSuperieur(arrayContent)) {
            String nom = extraireChamp(objet, "name");
            String url = extraireChamp(objet, "browser_download_url");
            if (nom != null && url != null) {
                assets.add(new RuntimeAsset(nom, url));
            }
        }
        return assets;
    }

    // Renvoie la sous-chaîne délimitée par une paire de délimiteurs équilibrée (ex: [...] ou {...})
    private static String extraireBlocEquilibre(String texte, int indexOuvrant, char ouvrant, char fermant) {
        int profondeur = 0;
        for (int i = indexOuvrant; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == ouvrant) {
                profondeur++;
            } else if (c == fermant) {
                profondeur--;
                if (profondeur == 0) {
                    return texte.substring(indexOuvrant, i + 1);
                }
            }
        }
        return null;
    }

    // Découpe un tableau JSON en ses objets de premier niveau, sans être perturbé par leurs objets imbriqués
    private static List<String> decouperObjetsDeNiveauSuperieur(String tableau) {
        List<String> objets = new ArrayList<>();
        int profondeur = 0;
        int debut = -1;
        for (int i = 0; i < tableau.length(); i++) {
            char c = tableau.charAt(i);
            if (c == '{') {
                if (profondeur == 0) debut = i;
                profondeur++;
            } else if (c == '}') {
                profondeur--;
                if (profondeur == 0 && debut != -1) {
                    objets.add(tableau.substring(debut, i + 1));
                    debut = -1;
                }
            }
        }
        return objets;
    }

    private static String extraireChamp(String objetJson, String champ) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(champ) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(objetJson);
        return m.find() ? m.group(1) : null;
    }

    // Extrait une archive .zip (builds Windows) avec protection contre le path traversal (zip-slip)
    public static void extraireZip(Path archive, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        Path racine = destDir.toAbsolutePath().normalize();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];

            while ((entry = zis.getNextEntry()) != null) {
                Path cible = racine.resolve(entry.getName()).normalize();
                if (!cible.startsWith(racine)) {
                    throw new IOException("Entree d'archive suspecte (path traversal) : " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(cible);
                } else {
                    if (cible.getParent() != null) {
                        Files.createDirectories(cible.getParent());
                    }
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(cible))) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // Extrait une archive .tar.gz (builds Linux/macOS) via la commande système tar
    public static void extraireTarGz(Path archive, Path destDir) throws IOException, InterruptedException {
        Files.createDirectories(destDir);

        ProcessBuilder pb = new ProcessBuilder(
                "tar", "-xzf", archive.toAbsolutePath().toString(),
                "-C", destDir.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // Purge la sortie du process pour éviter un blocage sur un buffer plein
            }
        }

        int code = process.waitFor();
        if (code != 0) {
            throw new IOException("Extraction tar.gz echouee (code de sortie " + code + ")");
        }
    }

    // Nom du binaire attendu selon l'OS courant
    private static String nomBinaireAttendu() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "llama-cli.exe" : "llama-cli";
    }

    // Recherche récursive (profondeur limitée) du binaire dans l'arborescence extraite
    private static Path localiserBinaire(Path racine, String nomBinaire) {
        if (racine == null || !Files.isDirectory(racine)) {
            return null;
        }
        try (var stream = Files.walk(racine, 4)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(nomBinaire))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    // Chemin du binaire runtime installé, ou null s'il est absent
    public static Path getRuntimeBinaryPath(String runtimeDir) {
        Path racine = Paths.get(normaliserRepertoire(runtimeDir));
        return localiserBinaire(racine, nomBinaireAttendu());
    }

    // Vérifie si le runtime natif est déjà installé et exécutable
    public static boolean isRuntimeInstalled(String runtimeDir) {
        Path binaire = getRuntimeBinaryPath(runtimeDir);
        return binaire != null && Files.isRegularFile(binaire) && Files.isExecutable(binaire);
    }

    // Télécharge un fichier via HTTP en flux (sans barre de progression, fichiers runtime plus légers que les modèles)
    private static void telechargerFichier(String url, Path destination) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Prompting-Tool-AIM2k26")
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur HTTP " + response.statusCode() + " lors du telechargement de " + url);
        }

        if (destination.getParent() != null) {
            Files.createDirectories(destination.getParent());
        }
        try (InputStream in = new BufferedInputStream(response.body());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(destination))) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    // Télécharge et installe automatiquement le runtime natif adapté à la plateforme courante
    public static boolean telechargerEtInstallerRuntime(String runtimeDir, PrintStream out) {
        String dir = normaliserRepertoire(runtimeDir);

        if (isRuntimeInstalled(dir)) {
            return true;
        }

        try {
            String plateforme = detecterPlateforme();
            out.println("[v] Recherche du runtime natif llama-cli pour la plateforme " + plateforme + "...");

            String releasesJson = httpGetTexte(GITHUB_RELEASES_URL);
            String tag = extraireTagVersion(releasesJson);
            List<RuntimeAsset> assets = extraireAssetsDepuisJson(releasesJson);
            List<String> noms = assets.stream().map(RuntimeAsset::name).toList();

            String assetChoisi = choisirAsset(noms, plateforme);
            if (assetChoisi == null) {
                out.println("[!] Aucun runtime precompile disponible pour la plateforme " + plateforme + ".");
                return false;
            }

            String url = assets.stream()
                    .filter(a -> a.name().equals(assetChoisi))
                    .map(RuntimeAsset::url)
                    .findFirst()
                    .orElse(null);
            if (url == null) {
                out.println("[!] URL de telechargement introuvable pour " + assetChoisi + ".");
                return false;
            }

            out.println("[v] Telechargement du runtime " + assetChoisi + " (version " + tag + ")...");
            Path racine = Paths.get(dir);
            Files.createDirectories(racine);
            Path archive = racine.resolve(assetChoisi);
            telechargerFichier(url, archive);

            out.println("[v] Extraction du runtime natif...");
            if (assetChoisi.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                extraireZip(archive, racine);
            } else {
                extraireTarGz(archive, racine);
            }
            Files.deleteIfExists(archive);

            Path binaire = localiserBinaire(racine, nomBinaireAttendu());
            if (binaire == null) {
                out.println("[!] Binaire " + nomBinaireAttendu() + " introuvable dans l'archive telechargee.");
                return false;
            }
            binaire.toFile().setExecutable(true);

            out.println("[+] Runtime natif installe : " + binaire.toAbsolutePath());
            return true;

        } catch (Exception e) {
            out.println("[!] Echec de l'installation du runtime natif : " + e.getMessage());
            return false;
        }
    }

    private static String httpGetTexte(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Prompting-Tool-AIM2k26")
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur HTTP " + response.statusCode() + " lors de l'appel a l'API GitHub");
        }
        return response.body();
    }
}
