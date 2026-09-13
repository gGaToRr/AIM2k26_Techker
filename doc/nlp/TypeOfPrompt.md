# 📄 Documentation : `nlp/TypeOfPrompt.java`

## 📌 Rôle du Fichier
`nlp/TypeOfPrompt.java` définit les **7 Archétypes Universels d'Intention** qui catégorisent l'intégralité des requêtes possibles qu'un humain peut adresser à un modèle de langage.

---

## 🌟 Les 7 Archétypes Universels

| Enum Value | Label Affiché | Intention & Cas d'Usage | Templates Associés |
| :--- | :--- | :--- | :--- |
| `APPRENTISSAGE_TUTORIEL` | `Archetype4Learning` | Apprendre, comprendre les bases, cours débutant, vulgarisation pas-à-pas. | `guide_debutant`, `feynman_learning` |
| `CONCEPTION_ARCHITECTURE` | `Archetype4Architecture` | Concevoir un système, générer du code production-ready, structure de dossiers, interface UI. | `code_generation`, `architecture_systeme`, `frontend_ui` |
| `DEPANNAGE_DIAGNOSTIC` | `Archetype4Troubleshooting` | Trouver un bug, identifier la cause racine, refactoriser du code propre, auditer la sécurité. | `root_cause_debug`, `refactor_clean`, `audit_review` |
| `CREATION_REDACTION` | `Archetype4Creation` | Storytelling, brainstorming d'idées, relecture/correction orthographique, traduction technique. | `storytelling`, `brainstorming`, `proofreading`, `technical_translation` |
| `PROTOCOLE_RECETTE` | `Archetype4Protocol` | Recette de cuisine, protocole technique, procédure opératoire standard étape par étape. | `recette_culinaire`, `protocole_technique` |
| `COMPARAISON_DECISION` | `Archetype4Comparison` | Comparer deux technologies/produits, matrice comparative, aide à la décision stratégique. | `matrice_comparative`, `aide_decision` |
| `CONCEPT_VULGARISATION` | `Archetype4FactualConcept` | Expliquer un concept théorique, encyclopédique, méthode Feynman avec analogies simples. | `concept_encyclopedique`, `vulgarisation_feynman` |

---

## 🛠️ Méthodes

* `getLabel()` : Retourne le label formel de l'archétype (ex: `Archetype4Learning`).
