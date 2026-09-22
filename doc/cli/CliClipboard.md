# 📄 Documentation : `src/cli/CliClipboard.java`

## 📌 Rôle du Fichier
`src/cli/CliClipboard.java` offre une couche d'abstraction pour copier automatiquement le prompt généré dans le **presse-papiers système** de la machine hôte.

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

---

## ⚡ Cache de Détection des Utilitaires

`copierTexte` teste jusqu'à quatre utilitaires (`wl-copy`, `xclip`, `xsel`, `pbcopy`) avant d'en retenir un. Chaque test lançait un process `which` : jusqu'à **4 process système par copie**, relancés à l'identique à chaque appel.

Le `PATH` ne change pas pendant la durée du process : une commande absente le reste, une commande présente aussi. Le résultat est donc mémorisé par nom dans un `ConcurrentHashMap`.

| Avant | Après |
|:---|:---|
| Jusqu'à 4 `which` par copie | Jusqu'à 4 `which` pour **toute** la durée du process |
| Résultat négatif re-sondé indéfiniment | Résultat négatif mis en cache au même titre qu'un positif |

`nombreDeSondagesSysteme()` expose le nombre de process réellement lancés — c'est ce compteur que les tests vérifient, plutôt qu'une mesure de durée qui serait instable.
