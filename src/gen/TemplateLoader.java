package gen;

import nlp.TypeOfPrompt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Charge et met en cache les templates Markdown du dossier genPrompt pour les 7 Archétypes Universels
public class TemplateLoader {

    private static final Map<String, String> CACHE_TEMPLATES = new HashMap<>();

    // Seules racines depuis lesquelles un template peut etre lu. Tout chemin resolu
    // en dehors de ce perimetre est refuse, quelle que soit la saisie utilisateur.
    private static final List<String> RACINES_TEMPLATES = List.of("src/genPrompt", "genPrompt");

    private static final String[] DOSSIERS_ARCHETYPES = {
            "learning", "architecture", "troubleshooting", "creation", "protocol", "comparison", "concept"
    };

    // Resout un chemin relatif sous l'une des racines autorisees.
    // Renvoie un Optional vide si le fichier n'existe pas OU si la resolution
    // sort du perimetre (ex: "../../etc/passwd", chemin absolu).
    public static Optional<Path> resoudreCheminTemplate(String relatif) {
        return resoudreSousRacines(relatif, Files::isRegularFile);
    }

    // Meme garantie de confinement, pour un dossier d'archetype
    public static Optional<Path> resoudreDossierTemplate(String relatif) {
        return resoudreSousRacines(relatif, Files::isDirectory);
    }

    private static Optional<Path> resoudreSousRacines(String relatif, java.util.function.Predicate<Path> accepte) {
        if (relatif == null || relatif.isBlank()) {
            return Optional.empty();
        }

        Path candidat;
        try {
            candidat = Path.of(relatif);
        } catch (Exception invalide) {
            return Optional.empty();
        }

        // Un chemin absolu ne peut par definition pas etre contenu dans une racine relative
        if (candidat.isAbsolute()) {
            return Optional.empty();
        }

        for (String racine : RACINES_TEMPLATES) {
            Path base = Path.of(racine).toAbsolutePath().normalize();
            Path resolu = base.resolve(candidat).normalize();

            // Confinement : apres normalisation, le chemin doit rester sous la racine
            if (!resolu.startsWith(base)) {
                continue;
            }
            if (accepte.test(resolu)) {
                return Optional.of(resolu);
            }
        }
        return Optional.empty();
    }

    // Charge un template à partir de l'Archétype et du sous-type
    public static String chargerTemplate(TypeOfPrompt type, String subType) {
        String dossier = switch (type) {
            case APPRENTISSAGE_TUTORIEL -> "learning";
            case CONCEPTION_ARCHITECTURE -> "architecture";
            case DEPANNAGE_DIAGNOSTIC -> "troubleshooting";
            case CREATION_REDACTION -> "creation";
            case PROTOCOLE_RECETTE -> "protocol";
            case COMPARAISON_DECISION -> "comparison";
            case CONCEPT_VULGARISATION -> "concept";
        };

        String cleCache = dossier + "/" + subType;
        if (CACHE_TEMPLATES.containsKey(cleCache)) {
            return CACHE_TEMPLATES.get(cleCache);
        }

        Optional<Path> cheminFichier = resoudreCheminTemplate(dossier + "/" + subType + ".md");
        if (cheminFichier.isPresent()) {
            try {
                String contenu = Files.readString(cheminFichier.get());
                CACHE_TEMPLATES.put(cleCache, contenu);
                return contenu;
            } catch (IOException ignored) {
                // En cas d'erreur de lecture, on bascule sur le fallback
            }
        }

        return fallbackTemplate(type);
    }

    // Charge un template spécifique par son nom ou son chemin (utilisé pour le flag -t/--template)
    public static Optional<String> chargerTemplateParNom(String nom) {
        if (nom == null || nom.isBlank()) return Optional.empty();
        String clean = nom.trim().replace(".md", "");
        String avecExtension = nom.trim().endsWith(".md") ? nom.trim() : nom.trim() + ".md";

        // 1. Chemin relatif fourni tel quel, confine sous les racines de templates
        Optional<String> direct = lire(resoudreCheminTemplate(avecExtension));
        if (direct.isPresent()) return direct;

        // 2. Recherche du nom exact dans chacun des dossiers d'archetypes
        for (String dossier : DOSSIERS_ARCHETYPES) {
            Optional<String> trouve = lire(resoudreCheminTemplate(dossier + "/" + clean + ".md"));
            if (trouve.isPresent()) return trouve;
        }

        // 3. Correspondance souple (ex: "feynman" -> "feynman_learning.md")
        for (String dossier : DOSSIERS_ARCHETYPES) {
            Optional<Path> dir = resoudreDossierTemplate(dossier);
            if (dir.isEmpty()) continue;

            try (var stream = Files.list(dir.get())) {
                for (Path file : stream.toList()) {
                    String fname = file.getFileName().toString().replace(".md", "");
                    if (fname.equalsIgnoreCase(clean) || fname.toLowerCase().contains(clean.toLowerCase())) {
                        return Optional.of(Files.readString(file));
                    }
                }
            } catch (IOException ignored) {}
        }

        return Optional.empty();
    }

    private static Optional<String> lire(Optional<Path> chemin) {
        if (chemin.isEmpty()) return Optional.empty();
        try {
            return Optional.of(Files.readString(chemin.get()));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    // Template de secours universel au cas où le fichier n'est pas trouvé
    private static String fallbackTemplate(TypeOfPrompt type) {
        return """
                # RÔLE & EXPERTISE
                {{#hasDomainExpertise}}
                {{domainPersona}}
                {{/hasDomainExpertise}}
                {{^hasDomainExpertise}}
                Tu es un Assistant IA Expert et Conseiller Stratégique de rang mondial.
                {{/hasDomainExpertise}}

                <contexte>
                - Archétype : {{type}}
                {{#hasDomainExpertise}}
                - Domaine : {{domainName}} (Sujet : {{domainTopic}})
                {{/hasDomainExpertise}}
                - Langue : {{language}}
                </contexte>

                <instruction_principale>
                {{cleanedMission}}
                </instruction_principale>

                {{#hasMultipleObjectives}}
                <objectifs_specifiques>
                {{#objectives}}
                - {{.}}
                {{/objectives}}
                </objectifs_specifiques>
                {{/hasMultipleObjectives}}

                <directives>
                - Répondre avec rigueur, précision, clarté et pédagogie.
                - Découper la réponse en étapes logiques et structurées.
                {{#hasAutoConstraints}}
                {{#autoConstraints}}
                - {{.}}
                {{/autoConstraints}}
                {{/hasAutoConstraints}}
                </directives>

                <format_de_sortie>
                - Rédiger la réponse en Markdown clair et soigné.
                </format_de_sortie>
                """;
    }
}
