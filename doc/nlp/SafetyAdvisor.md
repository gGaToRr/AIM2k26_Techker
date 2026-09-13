# 📄 Documentation : `nlp/SafetyAdvisor.java`

## 📌 Rôle du Fichier
`nlp/SafetyAdvisor.java` est un **module d'analyse préventive et non bloquant**. Il vérifie si le prompt contient des secrets accidentels (mots de passe, clés API, tokens JWT) ou des termes sensibles avant envoi vers un LLM.

---

## 🔒 Types d'Informations Détectées

1. **Secrets Applicatifs & Identifiants** :
   - Clés d'API (`api_key`, `secret_key`, `token`, `bearer`, `jwt`).
   - Mots de passe (`password`, `motdepasse`, `passwd`, `admin123`).
   - Chaînes de connexion de bases de données (`postgres://...`, `mysql://...`).

2. **Comportement Non Bloquant** :
   - Si un secret est identifié, l'application affiche un encadré d'avertissement jaune préventif recommandant d'utiliser des variables d'environnement ou placeholders, mais **ne bloque pas l'exécution de l'utilisateur**.

---

## 📋 Structure du Résultat (`SafetyReport`)

```java
public record SafetyReport(
        boolean containsSensitiveTerms,  // True si un terme sensible est identifié
        List<String> matchedTerms,       // Liste des catégories détectées
        String warningMessage            // Message d'avertissement formaté pour l'utilisateur
)
```

---

## 👨‍💻 Exemple d'Utilisation

```java
SafetyReport report = SafetyAdvisor.analyser("Voici ma clé api_key=sk-123456789");
if (report.containsSensitiveTerms()) {
    System.out.println(report.warningMessage());
}
```
