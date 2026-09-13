package nlp;

// Les 7 Archétypes Universels d'Intention applicables à tout domaine
public enum TypeOfPrompt {
    APPRENTISSAGE_TUTORIEL("Archetype4Learning", 1),
    CONCEPTION_ARCHITECTURE("Archetype4Architecture", 2),
    DEPANNAGE_DIAGNOSTIC("Archetype4Troubleshooting", 3),
    CREATION_REDACTION("Archetype4Creation", 4),
    PROTOCOLE_RECETTE("Archetype4Protocol", 5),
    COMPARAISON_DECISION("Archetype4Comparison", 6),
    CONCEPT_VULGARISATION("Archetype4FactualConcept", 7);

    private final String label;
    private final int count;

    TypeOfPrompt(String label, int count) {
        this.label = label;
        this.count = count;
    }

    public String getLabel() {
        return this.label;
    }

    public int getCount() {
        return this.count;
    }
}
