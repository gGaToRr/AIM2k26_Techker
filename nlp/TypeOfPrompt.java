package nlp;

// Definition d'une classe enum pour les différents types de Prompts
public enum TypeOfPrompt {
    CODE("Prompt4Coding", 1),
    TRANSLATE("Prompt4Translate", 2),
    CORRECTANSWERS("Prompt4CorrectAnswers", 3),
    CREATION("Prompt4Creation", 4),
    FACTUALQUESTIONS("Prompt4FactualQuestions", 5);

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
