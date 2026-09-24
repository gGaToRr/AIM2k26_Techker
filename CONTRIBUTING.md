# Contribuer à AIM2k26_Techker

## # Convention de nommage FR/EN

Le projet mélange français et anglais. Ce n'est pas un accident : la règle est **le français pour le métier, l'anglais pour la structure**. Elle est appliquée partout dans le code existant, mais n'était écrite nulle part — d'où ce document.

### La règle

| Élément | Langue | Exemples réels du projet |
|:---|:---|:---|
| **Noms de types** (classes, records, enums, interfaces) | **Anglais** | `GenerationResult`, `RoutingDecision`, `TemplateLoader`, `PromptProfile`, `ModelType` |
| **Méthodes de logique métier** | **Français** | `genererPromptOptimise`, `chargerTemplate`, `telechargerModele`, `verifierIntegrite`, `demanderPermissionUtilisateur` |
| **Variables locales et champs** | **Français** | `cheminFichier`, `tailleAttendue`, `dossier`, `empreinte` |
| **Accesseurs de records / API publique** | **Anglais** | `fullText()`, `totalTokens()`, `modelUsed()`, `primaryType()` |
| **Constantes** | **Français** | `CACHE_TEMPLATES`, `RACINES_TEMPLATES`, `DOSSIERS_ARCHETYPES` |
| **Commentaires et messages utilisateur** | **Français** | — |

### Pourquoi cette frontière

Un type est une **structure** : il se lit comme du Java et gagne à rester dans la langue du langage, surtout quand il est destiné à être manipulé depuis l'extérieur (`LlmBackend.GenerationResult`).

Une méthode métier décrit une **intention fonctionnelle**, exprimée dans la langue du domaine et de la documentation du projet. `genererPromptOptimise` dit ce que fait le produit ; `generateOptimizedPrompt` dirait la même chose dans une langue que ni les specs, ni les issues, ni la doc n'emploient.

### Cas limites

- **Méthode d'infrastructure sur un type anglais** : suit le type. `Log.debug()`, `Log.info()` restent en anglais — ce sont des noms de niveaux normalisés, pas du métier. Mais `Log.avertir()`, `Log.erreur()` et `Log.exceptionIgnoree()` sont en français : ils décrivent une intention propre au projet.
- **Méthode redéfinie** (`@Override`) : impose la signature de l'interface parente, y compris `generate`, `format`, `accept`.
- **Accesseur de record** : le nom du composant fait foi. Un record déclaré en anglais expose des accesseurs anglais.

### Accents

**Aucun accent dans les identifiants**, y compris français : `verifierIntegrite`, pas `vérifierIntégrité`. Les accents sont réservés aux chaînes de caractères, aux commentaires et à la documentation.

Les **messages console** s'écrivent sans accents lorsqu'ils transitent par un terminal dont l'encodage n'est pas garanti (bannières, onboarding, barres de progression) — c'est la raison des `Telechargement`, `Verification`, `deja installe` que l'on trouve dans `ModelInstaller` et `LlmEngine`.

---

## # Convention de message de commit

```
[type] chemin/ou/scope - description a l imperatif (Issue #N)
```

| Type | Usage |
|:---|:---|
| `[feat]` | Nouvelle capacité |
| `[fix]` | Correction de bug |
| `[perf]` | Optimisation à comportement constant |
| `[refactor]` | Restructuration à comportement constant |
| `[test]` | Ajout ou durcissement de tests |
| `[docs]` | Documentation |
| `[style]` | Mise en forme |
| `[chore]` | Outillage, configuration |
| `[ci]` | Intégration continue |

Le corps du message, s'il existe, explique **le problème** avant la solution. Exemple du dépôt :

```
[fix] gen/TemplateLoader.java - confinement des chemins de templates (Issues #58, #61)

chargerTemplateParNom construisait un Path directement depuis la saisie
-t/--template : un nom contenant ../ pouvait faire lire un .md arbitraire
hors du dossier des templates.
```

Les messages de commit s'écrivent **sans accents**, pour la même raison que les messages console.

---

## # Tests

Toute modification de comportement s'accompagne de tests dans `src/test/`, exécutés par :

```bash
bash src/scripts/run_tests.sh
```

Deux exigences :

1. **Un test de non-régression doit échouer contre le code d'avant.** Un test qui passe dans les deux cas ne prouve rien.
2. **Aucun test ne doit atteindre le réseau.** Les téléchargements (modèles, runtime natif) passent par des coutures injectables prévues pour cela — voir `LlmEngine.RuntimeProvisioner` et `ModelInstaller.verifierIntegrite(Path, String, long, PrintStream)`.

---

## # Structure

| Dossier | Contenu |
|:---|:---|
| `src/nlp/` | Analyse linguistique du prompt brut |
| `src/gen/` | Meta-prompting et rendu des templates |
| `src/genPrompt/` | Templates Markdown par archétype |
| `src/cli/` | Analyse des arguments et sorties |
| `src/llm/` | Inférence locale, routage et installation des modèles |
| `src/util/` | Utilitaires transverses (journal) |
| `src/menu/` | Mode interactif |
| `src/test/` | Suites TDD et framework maison |
| `doc/` | Documentation par module |
