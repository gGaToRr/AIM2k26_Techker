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
- Écriture dans un fichier temporaire `.part` puis renommage atomique final pour garantir l'intégrité du fichier.
