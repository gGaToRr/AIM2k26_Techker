# 📄 Documentation : `cli/CliClipboard.java`

## 📌 Rôle du Fichier
`cli/CliClipboard.java` offre une couche d'abstraction pour copier automatiquement le prompt généré dans le **presse-papiers système** de la machine hôte.

---

## ⚙️ Mécanisme Multi-Plateforme

Pour garantir une compatibilité maximale sans dépendances externes lourdes, la classe teste les utilitaires dans l'ordre suivant :

1. **Linux Wayland** : Détecte la variable d'environnement `WAYLAND_DISPLAY` et utilise la commande `wl-copy`.
2. **Linux X11** : Utilise `xclip -selection clipboard` ou `xsel --clipboard --input`.
3. **macOS** : Utilise l'utilitaire natif `pbcopy`.
4. **Fallback Universel (Java AWT)** : Si aucun utilitaire CLI n'est disponible ou fonctionnel, bascule sur `Toolkit.getDefaultToolkit().getSystemClipboard()`.

---

## 🛠️ Méthodes Principales

* `copierTexte(String texte)` : Retourne `true` si le texte a été copié avec succès dans le presse-papiers, `false` sinon. Ne lève jamais d'exception non gérée vers l'utilisateur.

---

## 👨‍💻 Exemple d'Utilisation

```java
String superPrompt = "# RÔLE & EXPERTISE...";
boolean succes = CliClipboard.copierTexte(superPrompt);
if (succes) {
    System.out.println("Prompt copié dans le presse-papiers !");
}
```
