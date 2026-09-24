package test.gen;

import gen.SubTypeRules;
import gen.TemplateLoader;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.TypeOfPrompt;
import test.framework.Assert;
import test.framework.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Tests des regles de sous-type externalisees (Issue #66)
public class SubTypeRulesTest {

    private static String sousTypePour(String texte, TypeOfPrompt type) {
        PromptProfile profile = Lemmatizer.analyser(texte);
        return SubTypeRules.resoudre(type, profile);
    }

    // --- Le fichier de configuration est bien celui qui pilote la decision ---

    @Test
    public void testLeFichierDeReglesEstPresentEtChargeable() {
        Assert.assertTrue(TemplateLoader.resoudreCheminTemplate(SubTypeRules.FICHIER_REGLES).isPresent(),
                "Le fichier de regles doit etre livre avec le projet");
    }

    @Test
    public void testChaqueArchetypePossedeUnSousTypeParDefaut() {
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            String defaut = SubTypeRules.defautDe(type);
            Assert.assertNotNull(defaut, "Defaut declare pour " + type.name());
            Assert.assertFalse(defaut.isBlank(), "Defaut non vide pour " + type.name());
        }
    }

    // Une regle sans declencheur ni condition ne pourrait jamais se satisfaire
    @Test
    public void testAucuneRegleInerte() {
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            for (SubTypeRules.Regle regle : SubTypeRules.reglesDe(type)) {
                boolean exploitable = !regle.motsCles().isEmpty()
                        || (regle.condition() != null && !regle.condition().isBlank());
                Assert.assertTrue(exploitable,
                        "La regle " + type.name() + "." + regle.sousType() + " n'a ni mot-cle ni condition");
            }
        }
    }

    // Chaque sous-type declare doit correspondre a un template reellement livre
    @Test
    public void testChaqueSousTypeDeclarePossedeSonTemplate() {
        List<String> manquants = new ArrayList<>();
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            List<String> aVerifier = new ArrayList<>();
            aVerifier.add(SubTypeRules.defautDe(type));
            for (SubTypeRules.Regle regle : SubTypeRules.reglesDe(type)) {
                aVerifier.add(regle.sousType());
            }
            for (String sousType : aVerifier) {
                if (TemplateLoader.chargerTemplateParNom(sousType).isEmpty()) {
                    manquants.add(type.name() + "." + sousType);
                }
            }
        }
        Assert.assertTrue(manquants.isEmpty(), "Sous-types sans template livre : " + manquants);
    }

    // --- Comportement identique a la classification precedemment codee en dur ---

    @Test
    public void testSelectionParMotCle() {
        Assert.assertEquals("guide_debutant", sousTypePour("Cours d'initiation pour debutant", TypeOfPrompt.APPRENTISSAGE_TUTORIEL), "Declencheur debutant");
        Assert.assertEquals("feynman_learning", sousTypePour("Explique la recursion", TypeOfPrompt.APPRENTISSAGE_TUTORIEL), "Defaut de l'archetype");
        Assert.assertEquals("audit_review", sousTypePour("Fais un audit de securite", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC), "Declencheur audit");
        Assert.assertEquals("refactor_clean", sousTypePour("Il faut refactor ce module", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC), "Declencheur refactor");
        Assert.assertEquals("root_cause_debug", sousTypePour("Ca plante au demarrage", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC), "Defaut de l'archetype");
        Assert.assertEquals("recette_culinaire", sousTypePour("Donne la recette du gateau", TypeOfPrompt.PROTOCOLE_RECETTE), "Declencheur recette");
        Assert.assertEquals("aide_decision", sousTypePour("Lequel choisir entre les deux", TypeOfPrompt.COMPARAISON_DECISION), "Declencheur choix");
    }

    // Les mots-cles sont des fragments : "amelior" doit capturer "amelioration"
    @Test
    public void testLesMotsClesSontDesFragments() {
        Assert.assertEquals("refactor_clean", sousTypePour("Propose une amelioration du code", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC),
                "amelior doit capturer amelioration");
        Assert.assertEquals("refactor_clean", sousTypePour("Il faut optimiser la boucle", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC),
                "optimis doit capturer optimiser");
    }

    // --- contexteRequis est une exigence supplementaire, pas une alternative ---

    @Test
    public void testContexteRequisEstUneConjonction() {
        // Declencheur present + contexte web present -> frontend_ui
        Assert.assertEquals("frontend_ui",
                sousTypePour("Ameliore le design du bouton sur mon site web", TypeOfPrompt.CONCEPTION_ARCHITECTURE),
                "Declencheur ET contexte web");

        // Declencheur present mais aucun contexte web -> ce n'est pas du frontend
        Assert.assertNotEquals("frontend_ui",
                sousTypePour("Dessine le design du bouton de la machine a cafe", TypeOfPrompt.CONCEPTION_ARCHITECTURE),
                "Un declencheur sans contexte web ne suffit pas");

        // Contexte web present mais aucun declencheur -> ce n'est pas du frontend non plus
        Assert.assertNotEquals("frontend_ui",
                sousTypePour("Cree une API web en Java", TypeOfPrompt.CONCEPTION_ARCHITECTURE),
                "Un contexte sans declencheur ne suffit pas");
    }

    // --- Ordre d'evaluation : la premiere regle satisfaite l'emporte ---

    @Test
    public void testPremiereRegleSatisfaiteLEmporte() {
        // "audit" (1re regle) et "refactor" (2e) sont tous deux presents
        Assert.assertEquals("audit_review",
                sousTypePour("Fais un audit puis refactor le module", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC),
                "L'ordre declare dans le fichier tranche les ambiguites");
    }

    // --- Conditions integrees, non exprimables en mots-cles ---

    @Test
    public void testConditionExpressionCourteSansQuestion() {
        Assert.assertEquals("concept_encyclopedique", sousTypePour("Entropie", TypeOfPrompt.CONCEPT_VULGARISATION),
                "Expression courte sans point d'interrogation");
        Assert.assertEquals("vulgarisation_feynman", sousTypePour("Qu'est-ce que l'entropie ?", TypeOfPrompt.CONCEPT_VULGARISATION),
                "Une question bascule sur la vulgarisation");
    }

    // --- Modifiable sans recompilation : c'est l'objet meme de l'issue ---

    @Test
    public void testUnMotCleAjouteAuFichierPrendEffetApresRechargement() throws Exception {
        Path fichier = TemplateLoader.resoudreCheminTemplate(SubTypeRules.FICHIER_REGLES).orElseThrow();
        String original = Files.readString(fichier);
        try {
            Assert.assertEquals("root_cause_debug", sousTypePour("Fais une inspection du module", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC),
                    "Avant modification, inspection n'est pas un declencheur");

            Files.writeString(fichier, original.replace(
                    "DEPANNAGE_DIAGNOSTIC.audit_review.motsCles = review, audit, securite, conformite",
                    "DEPANNAGE_DIAGNOSTIC.audit_review.motsCles = review, audit, securite, conformite, inspection"));
            SubTypeRules.recharger();

            Assert.assertEquals("audit_review", sousTypePour("Fais une inspection du module", TypeOfPrompt.DEPANNAGE_DIAGNOSTIC),
                    "Le mot-cle ajoute au fichier prend effet sans recompilation");
        } finally {
            Files.writeString(fichier, original);
            SubTypeRules.recharger();
        }
    }

    // Le repli integre doit rester identique au fichier livre, sinon son absence
    // changerait silencieusement le comportement du produit
    @Test
    public void testLeRepliIntegreEstIdentiqueAuFichierLivre() throws Exception {
        Path fichier = TemplateLoader.resoudreCheminTemplate(SubTypeRules.FICHIER_REGLES).orElseThrow();
        String original = Files.readString(fichier);

        List<String> depuisFichier = new ArrayList<>();
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            depuisFichier.add(type.name() + "=" + SubTypeRules.defautDe(type) + SubTypeRules.reglesDe(type));
        }

        Path deplace = fichier.resolveSibling(SubTypeRules.FICHIER_REGLES + ".bak");
        try {
            Files.move(fichier, deplace);
            SubTypeRules.recharger();

            List<String> depuisRepli = new ArrayList<>();
            for (TypeOfPrompt type : TypeOfPrompt.values()) {
                depuisRepli.add(type.name() + "=" + SubTypeRules.defautDe(type) + SubTypeRules.reglesDe(type));
            }

            Assert.assertEquals(depuisFichier, depuisRepli,
                    "Le repli integre doit produire exactement les memes regles que le fichier");
        } finally {
            if (Files.exists(deplace)) {
                Files.move(deplace, fichier);
            } else {
                Files.writeString(fichier, original);
            }
            SubTypeRules.recharger();
        }
    }
}
