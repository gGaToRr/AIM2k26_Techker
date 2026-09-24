package gen;

import nlp.PromptProfile;
import nlp.TypeOfPrompt;
import util.Log;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

// Regles de selection du sous-type (template) par archetype, chargees depuis
// src/genPrompt/regles_sous_types.properties.
//
// Ces listes de mots-cles vivaient en dur dans MetaPromptEngine : ajuster un
// synonyme imposait une recompilation, et la couverture par sous-type etait
// invisible. Les externaliser les rend inspectables et modifiables a chaud.
public final class SubTypeRules {

    public static final String FICHIER_REGLES = "regles_sous_types.properties";

    // Une regle : le sous-type vise, ses declencheurs, un contexte optionnel
    // exige en plus, et un predicat integre optionnel.
    public record Regle(String sousType, List<String> motsCles, List<String> contexteRequis, String condition) {

        public boolean correspond(String texteNormalise, PromptProfile profile, int nbMots) {
            if (condition != null && !condition.isBlank() && evaluerCondition(condition, profile, nbMots)) {
                return true;
            }
            if (motsCles.isEmpty()) {
                return false;
            }
            boolean declencheur = motsCles.stream().anyMatch(texteNormalise::contains);
            if (!declencheur) {
                return false;
            }
            if (contexteRequis.isEmpty()) {
                return true;
            }
            // Le contexte est une exigence supplementaire, pas une alternative :
            // "design" ne suffit pas, il faut aussi un indice que l'on parle de web.
            return contexteRequis.stream().anyMatch(texteNormalise::contains);
        }
    }

    // Predicats non exprimables en mots-cles, nommes depuis le fichier de regles
    private static boolean evaluerCondition(String condition, PromptProfile profile, int nbMots) {
        return switch (condition.trim()) {
            case "traductionDetectee" -> profile.targetTranslationLanguage().isPresent();
            case "expressionCourteSansQuestion" -> nbMots <= 3 && !profile.rawText().contains("?");
            default -> {
                Log.avertir("Condition inconnue dans " + FICHIER_REGLES + " : " + condition);
                yield false;
            }
        };
    }

    private static Map<TypeOfPrompt, List<Regle>> regles;
    private static Map<TypeOfPrompt, String> defauts;

    private SubTypeRules() {}

    // Selectionne le sous-type : premiere regle satisfaite, sinon le defaut de l'archetype
    public static String resoudre(TypeOfPrompt type, PromptProfile profile) {
        chargerSiNecessaire();

        String texteNormalise = profile.rawText().toLowerCase();
        int nbMots = profile.rawText().trim().split("\\s+").length;

        for (Regle regle : regles.getOrDefault(type, List.of())) {
            if (regle.correspond(texteNormalise, profile, nbMots)) {
                return regle.sousType();
            }
        }
        return defauts.getOrDefault(type, "");
    }

    public static List<Regle> reglesDe(TypeOfPrompt type) {
        chargerSiNecessaire();
        return regles.getOrDefault(type, List.of());
    }

    public static String defautDe(TypeOfPrompt type) {
        chargerSiNecessaire();
        return defauts.getOrDefault(type, "");
    }

    // Force un rechargement (tests, ou edition du fichier a chaud)
    public static synchronized void recharger() {
        regles = null;
        defauts = null;
        chargerSiNecessaire();
    }

    private static synchronized void chargerSiNecessaire() {
        if (regles != null) {
            return;
        }
        Properties proprietes = lireProprietes();

        Map<TypeOfPrompt, List<Regle>> parType = new LinkedHashMap<>();
        Map<TypeOfPrompt, String> parDefaut = new LinkedHashMap<>();

        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            String prefixe = type.name();
            parDefaut.put(type, proprietes.getProperty(prefixe + ".defaut", "").trim());

            List<Regle> listeDuType = new ArrayList<>();
            for (String sousType : decouper(proprietes.getProperty(prefixe + ".ordre", ""))) {
                listeDuType.add(new Regle(
                        sousType,
                        decouper(proprietes.getProperty(prefixe + "." + sousType + ".motsCles", "")),
                        decouper(proprietes.getProperty(prefixe + "." + sousType + ".contexteRequis", "")),
                        proprietes.getProperty(prefixe + "." + sousType + ".condition")
                ));
            }
            parType.put(type, List.copyOf(listeDuType));
        }

        regles = Map.copyOf(parType);
        defauts = Map.copyOf(parDefaut);
    }

    private static Properties lireProprietes() {
        Properties proprietes = new Properties();
        Optional<Path> chemin = TemplateLoader.resoudreCheminTemplate(FICHIER_REGLES);

        if (chemin.isPresent()) {
            try (InputStream flux = Files.newInputStream(chemin.get())) {
                proprietes.load(new java.io.InputStreamReader(flux, java.nio.charset.StandardCharsets.UTF_8));
                return proprietes;
            } catch (Exception e) {
                Log.erreur("Lecture de " + FICHIER_REGLES + ", repli sur les regles integrees", e);
            }
        } else {
            Log.avertir(FICHIER_REGLES + " introuvable, repli sur les regles integrees.");
        }

        // Repli : le produit doit continuer de fonctionner meme si le fichier de
        // configuration est absent ou illisible.
        try {
            proprietes.load(new java.io.StringReader(REGLES_INTEGREES));
        } catch (Exception e) {
            Log.erreur("Chargement des regles integrees", e);
        }
        return proprietes;
    }

    private static List<String> decouper(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return List.of();
        }
        List<String> elements = new ArrayList<>();
        for (String morceau : valeur.split(",")) {
            String propre = morceau.trim().toLowerCase();
            if (!propre.isEmpty()) {
                elements.add(propre);
            }
        }
        return List.copyOf(elements);
    }

    // Copie de secours, identique au fichier livre
    private static final String REGLES_INTEGREES = """
            APPRENTISSAGE_TUTORIEL.ordre = guide_debutant
            APPRENTISSAGE_TUTORIEL.defaut = feynman_learning
            APPRENTISSAGE_TUTORIEL.guide_debutant.motsCles = debutant, initiation, premier, bases, cours
            CONCEPTION_ARCHITECTURE.ordre = frontend_ui, architecture_systeme
            CONCEPTION_ARCHITECTURE.defaut = code_generation
            CONCEPTION_ARCHITECTURE.frontend_ui.motsCles = design, bouton, carte, ombre, layout
            CONCEPTION_ARCHITECTURE.frontend_ui.contexteRequis = site, css, html, tailwind, frontend, web, responsive
            CONCEPTION_ARCHITECTURE.architecture_systeme.motsCles = architecture, arborescence, structure de dossier, structure des dossier, organisation
            DEPANNAGE_DIAGNOSTIC.ordre = audit_review, refactor_clean
            DEPANNAGE_DIAGNOSTIC.defaut = root_cause_debug
            DEPANNAGE_DIAGNOSTIC.audit_review.motsCles = review, audit, securite, conformite
            DEPANNAGE_DIAGNOSTIC.refactor_clean.motsCles = refactor, clean, amelior, optimis
            CREATION_REDACTION.ordre = technical_translation, proofreading, brainstorming
            CREATION_REDACTION.defaut = storytelling
            CREATION_REDACTION.technical_translation.motsCles = tradui, translate
            CREATION_REDACTION.technical_translation.condition = traductionDetectee
            CREATION_REDACTION.proofreading.motsCles = corrig, orthographe, grammaire, relectur
            CREATION_REDACTION.brainstorming.motsCles = brainstorm, idee, concept
            PROTOCOLE_RECETTE.ordre = recette_culinaire
            PROTOCOLE_RECETTE.defaut = protocole_technique
            PROTOCOLE_RECETTE.recette_culinaire.motsCles = recette, cuisin, cuisson, gateau, tarte, plat, ingredient
            COMPARAISON_DECISION.ordre = aide_decision
            COMPARAISON_DECISION.defaut = matrice_comparative
            COMPARAISON_DECISION.aide_decision.motsCles = choisir, choix, arbitrage, decision, lequel
            CONCEPT_VULGARISATION.ordre = concept_encyclopedique
            CONCEPT_VULGARISATION.defaut = vulgarisation_feynman
            CONCEPT_VULGARISATION.concept_encyclopedique.condition = expressionCourteSansQuestion
            """;
}
