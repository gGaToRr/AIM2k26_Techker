# 📄 Documentation : `nlp/DomainExtractor.java`

## 📌 Rôle du Fichier
`nlp/DomainExtractor.java` identifie le **domaine d'expertise métier**, extrait le **sujet pivot** de la requête et forge un **persona d'expert adapté** pour guider le LLM.

---

## 🏛️ Les 10 Familles de Domaines Référencées

1. **Musique & Lutherie** → *"Tu es un Maître Musicien, Compositeur et Pédagogue Musical de renommée internationale."*
2. **Gastronomie & Arts Culinaires** → *"Tu es un Chef Étoilé et Expert Pédagogue en Haute Gastronomie."*
3. **Aéronautique & Transports** → *"Tu es un Ingénieur Senior en Aéronautique & Systèmes de Transport et Pilote Instructeur."*
4. **Astrophysique & Physique** → *"Tu es un Astrophysicien et Enseignant-Chercheur Émérite en Cosmologie."*
5. **Génie Mécanique & Industrie** → *"Tu es un Ingénieur Mécanicien Senior et Concepteur Industriel de référence."*
6. **Sport & Biomécanique** → *"Tu es un Préparateur Physique d'Athlètes de Haut Niveau et Expert en Biomécanique."*
7. **Histoire & Civilisations** → *"Tu es un Historien Universitaire et Érudit Spécialiste des Civilisations."*
8. **Droit & Réglementation** → *"Tu es un Juriste Émérite et Conseiller Spécialiste du Droit et de la Réglementation."*
9. **Économie & Finance** → *"Tu es un Économiste et Analyste Stratégique de Marché de premier plan."*
10. **Médecine & Sciences Biomédicales** → *"Tu es un Médecin et Chercheur Spécialiste en Physiologie & Sciences Biomédicales."*

---

## 🎯 Extraction de Sujet Pivot & Forçage

* **Extraction Automatique** : Analyse les motifs d'action (ex: `"Comment fonctionne [un turboréacteur]"` → sujet : `"turboréacteur"`).
* **Forçage par Drapeau CLI (`-d` / `--domain`)** : Permet de forcer un domaine connu ou sur-mesure (ex: `-d "Cybersecurity"` → *"Tu es un Spécialiste Senior, Consultant et Pédagogue d'Élite en Cybersecurity."*).

---

## 👨‍💻 Exemple d'Utilisation

```java
DomainInfo info = DomainExtractor.analyser("Recette de la tarte au citron meringuée étape par étape");
System.out.println("Domaine : " + info.domainName()); // Gastronomie & Arts Culinaires
System.out.println("Sujet   : " + info.extractedTopic()); // tarte au citron meringuée
System.out.println("Persona : " + info.expertPersona()); // Tu es un Chef Étoilé...
```
