# ⬇️ Documentation : `src/llm/ModelInstaller.java`

## 🎯 Rôle du Fichier

Le composant [`ModelInstaller.java`](file:///home/kaets0ner/Desktop/Project_Ia/llm/ModelInstaller.java) orchestre **l'onboarding pour les utilisateurs débutants**, la gestion des permissions de téléchargement et le téléchargement HTTP résilient des fichiers de modèles GGUF.

---

## 🌟 Expérience Utilisateur Débutant ("Noob-Friendly")

Lors du premier lancement en mode exécution locale (`-e`), l'outil détecte si le modèle requis est déjà présent dans le dossier local `models/`. Si ce n'est pas le cas, un écran d'accueil détaillé et bienveillant est affiché :
- **Explication claire** : Ce qu'est un LLM local qui s'exécute sur le processeur/GPU personnel.
- **Rassurement sur la sécurité** : Aucun transfert de données vers Internet, confidentialité totale.
- **Transparence sur les coûts & ressources** : Gratuité complète, absence d'abonnement et indication exacte de l'espace disque consommé (1.0 à 1.6 Go).
- **Choix interactif** :
  1. `[1]` Autoriser le téléchargement automatique du modèle recommandé.
  2. `[2]` Choisir un modèle spécifique dans le catalogue.
  3. `[3]` Refuser / Reporter et générer uniquement le texte du méta-prompt.

---

## 🚀 Téléchargement Interactif & Barre de Progression

Le téléchargement s'effectue via `java.net.http.HttpClient` avec :
- Écoute en flux continu (`InputStream` bufferisé).
- Affichage dynamique de la vitesse (en Mo/s) et du pourcentage de progression :
  ```
  Progression : [===================>          ]  65% (1040.0 / 1600.0 Mo à 18.50 Mo/s)
  ```
- Écriture dans un fichier temporaire `.part` puis renommage atomique final.

---

## 🔒 Vérification d'Intégrité (SHA-256)

Le renommage `.part` → fichier final n'a lieu **qu'après validation de l'empreinte**. Un binaire de plusieurs Go exécuté ensuite par `llama-cli` ne doit jamais provenir d'une source non vérifiée : URL redirigée, miroir compromis ou transfert corrompu.

Deux contrôles successifs, du moins coûteux au plus coûteux :

| Contrôle | Référence | En cas d'écart |
|:---|:---|:---|
| Taille exacte en octets | `ModelType.getTailleOctets()` | Rejet immédiat, sans lire le fichier |
| Empreinte SHA-256 | `ModelType.getSha256()` | Rejet, empreintes attendue/obtenue affichées |

Dans les deux cas le fichier `.part` est supprimé et `telechargerModele` renvoie `false` : **aucun fichier final n'est créé**. Le `.part` est également nettoyé si une exception interrompt le transfert.

`calculerSha256` travaille en flux (tampon de 64 Ko), sans jamais charger le modèle en mémoire.

### Épinglage des révisions

Une empreinte figée n'a de sens que face à un contenu figé. Les URL de `ModelType` pointent donc sur une **révision précise** du dépôt Hugging Face (`/resolve/<commit-sha>/…`) et non sur `main`, qui peut être republié à tout moment. Les valeurs de référence proviennent de l'en-tête `X-Linked-ETag` renvoyé par Hugging Face pour cette révision.

> Mettre à jour un modèle impose donc de mettre à jour ensemble la révision, l'empreinte et la taille.

---

## 🗂️ Cycle de vie complet des modèles

`ModelInstaller` couvre le téléchargement (Create) et la détection (Read). La suppression (Delete) vit dans **`ModelManager`**, pilotée en ligne de commande par `ModelsCommand`.

| Commande | Effet |
|:---|:---|
| `-ml`, `--models-list` | Tableau : statut, taille réelle sur disque, date d'installation, espace total |
| `-mi`, `--models-info <nom>` | Fiche technique : spécialité, RAM conseillée, empreinte, chemin local |
| `-md`, `--models-delete <nom>` | Supprime un modèle, après confirmation |
| `-mp`, `--models-purge` | Supprime tous les modèles et réinitialise l'autorisation |

Les alias du registre fonctionnent comme pour `-m/--model` : `qwen`, `code`, `r1`…

### Trois garde-fous

**Aucune suppression sans accord explicite.** La confirmation est en `[o/N]` : une réponse vide vaut refus. On parle de plusieurs Go à retélécharger.

**Le `.part` est nettoyé avec le modèle.** Un téléchargement interrompu laisse un fichier partiel à côté du `.gguf`. Le supprimer seul ne libérerait pas réellement l'espace annoncé.

**La purge réinitialise `permissionAccordee`.** Cette permission avait été accordée pour un téléchargement précis. La reconduire tacitement après une purge relancerait un téléchargement de plusieurs Go que l'utilisateur vient justement d'annuler — il la redonne, ou non.
