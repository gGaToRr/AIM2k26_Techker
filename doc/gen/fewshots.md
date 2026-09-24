# 🎯 Documentation : `src/gen/FewShotLibrary.java`

## 📌 Pourquoi

Les modèles visés par l'inférence locale sont petits — 1,5 à 2 milliards de paramètres. Ils suivent nettement mieux un format qu'on leur **montre** qu'un format qu'on leur **décrit**.

Mais un exemple hors sujet dégrade la réponse : le modèle imite le domaine de l'exemple plutôt que la demande. D'où une **sélection par pertinence**, et non un bloc fixe.

---

## 📁 Format des fichiers

`src/genPrompt/fewshots/<archetype>.md`, ou `<sous_type>.md` pour spécialiser.

```markdown
## EXEMPLE

### DEMANDE
Mon appli Java plante avec NullPointerException dans le service de panier.

### REPONSE
**1. Diagnostic & Cause Racine**
...
```

Les marqueurs sont **ancrés en début de ligne**. Sans cette contrainte, l'en-tête explicative d'un fichier — qui cite `### DEMANDE` entre guillemets — était elle-même lue comme un exemple, produisant une paire vide. C'est un test qui l'a révélé.

**Résolution** : le sous-type d'abord, l'archétype à défaut. Un fichier dont le nom ne correspond ni à l'un ni à l'autre ne serait jamais chargé — un test le signale.

---

## 🎚️ Sélection

Les exemples sont classés par **recouvrement lexical** avec la demande, normalisé par l'union des vocabulaires :

```
score = |mots communs| / |union des mots|
```

| Choix | Raison |
|:---|:---|
| Normalisation par l'union | Un exemple très long gagnerait sinon par accumulation |
| Mots vides écartés | « le », « une », « dans » créeraient une proximité fictive entre deux sujets sans rapport |
| Accents normalisés, seuil à 3 lettres | « modèle » et « modele » doivent se rencontrer |
| **Tri stable** | À score égal, l'ordre du fichier tranche : la sélection est reproductible |

---

## 📏 Budget de contexte

| Limite | Valeur |
|:---|:---|
| `MAX_EXEMPLES` | 2 |
| `MAX_CARACTERES` | 2400 |

Ce n'est pas une précaution cosmétique : au-delà, les exemples chassent la demande réelle hors de la fenêtre de contexte du modèle. Un exemple qui ne tient pas dans le budget restant est écarté plutôt que tronqué — un exemple coupé en deux enseigne un format faux.

---

## 🧩 Injection

```mustache
{{#hasFewShot}}
<exemples_de_reference>
Voici des exemples du niveau de detail et du format attendus. Ne reponds pas a ces exemples :
inspire-toi de leur structure pour traiter la demande ci-dessus.

{{EXEMPLES_FEW_SHOT}}
</exemples_de_reference>
{{/hasFewShot}}
```

La consigne « ne réponds pas à ces exemples » n'est pas décorative : sans elle, un petit modèle traite volontiers la demande de l'exemple à la place de celle de l'utilisateur.

Le bloc est présent dans les 18 templates, et `{{#hasFewShot}}` évite une section vide quand aucun exemple n'est disponible.
