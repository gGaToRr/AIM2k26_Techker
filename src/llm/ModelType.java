package llm;

import nlp.TypeOfPrompt;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// Registre des modèles de langage légers (SLMs) supportés pour l'inférence locale
public enum ModelType {

    QWEN_CODER(
            "qwen-coder",
            "Qwen 2.5 Coder (1.5B Instruct)",
            "Programmation, Débogage, Architecture logicielle, Refactoring",
            "Modèle ultra-spécialisé pour la génération et l'analyse de code source dans plus de 90 langages.",
            "~1.1 Go (Quantifié Q4_K_M)",
            "~1.5 Go",
            "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
            "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/f86cb2c1fa58255f8052cc32aeede1b7482d4361/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
            "cc324af070c2ecbfd324a30884d2f951a7ff756aba85cb811a6ec436933bb046",
            1117320768L,
            List.of(TypeOfPrompt.CONCEPTION_ARCHITECTURE, TypeOfPrompt.DEPANNAGE_DIAGNOSTIC)
    ),

    GEMMA_GENERAL(
            "gemma-general",
            "Gemma 2 (2B Instruct)",
            "Pédagogie, Rédaction générale, Culture, Explications claires",
            "Modèle compact développé par Google, très éloquent en français pour la vulgarisation et l'apprentissage.",
            "~1.6 Go (Quantifié Q4_K_M)",
            "~2.0 Go",
            "gemma-2-2b-it-q4_k_m.gguf",
            "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/855f67caed130e1befc571b52bd181be2e858883/gemma-2-2b-it-Q4_K_M.gguf",
            "e0aee85060f168f0f2d8473d7ea41ce2f3230c1bc1374847505ea599288a7787",
            1708582752L,
            List.of(TypeOfPrompt.APPRENTISSAGE_TUTORIEL, TypeOfPrompt.CONCEPT_VULGARISATION, TypeOfPrompt.CREATION_REDACTION)
    ),

    DEEPSEEK_REASONING(
            "deepseek-reasoning",
            "DeepSeek R1 Distill (1.5B)",
            "Raisonnement logique, Comparaisons complexes, Mathématiques, Prise de décision",
            "Modèle de raisonnement avancé générant une chaîne de pensée pas-à-pas (Chain-of-Thought).",
            "~1.1 Go (Quantifié Q4_K_M)",
            "~1.5 Go",
            "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/3cb4d15544a2a5e07439592b9a0965b6445fbd34/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            "f3bdf9cf31dee4b57ae4e455a1cb0d01b5c2c1b50d72d3112141c195506c2840",
            1117321312L,
            List.of(TypeOfPrompt.COMPARAISON_DECISION, TypeOfPrompt.PROTOCOLE_RECETTE)
    ),

    SMOLLM_FAST(
            "smollm-fast",
            "SmolLM2 (1.7B Instruct)",
            "Synthèse ultra-rapide, Questions directes, Faible consommation mémoire",
            "Modèle ultra-compact conçu par HuggingFace, idéal pour des réponses immédiates sur machine modeste.",
            "~1.0 Go (Quantifié Q4_K_M)",
            "~1.2 Go",
            "smollm2-1.7b-instruct-q4_k_m.gguf",
            "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/2d4a76a30b4af41ecd395c35725ac11688d4cfe4/smollm2-1.7b-instruct-q4_k_m.gguf",
            "decd2598bc2c8ed08c19adc3c8fdd461ee19ed5708679d1c54ef54a5a30d4f33",
            1055609536L,
            List.of()
    );

    private final String id;
    private final String nomAffiche;
    private final String specialite;
    private final String description;
    private final String tailleDisque;
    private final String ramRecommandee;
    private final String nomFichier;
    private final String urlTelechargement;
    // Empreinte SHA-256 de reference du fichier .gguf, relevee sur la revision epinglee
    // dans urlTelechargement (en-tete X-Linked-ETag de Hugging Face).
    private final String sha256;
    private final long tailleOctets;
    private final List<TypeOfPrompt> archetypesCibles;

    ModelType(String id, String nomAffiche, String specialite, String description,
              String tailleDisque, String ramRecommandee, String nomFichier,
              String urlTelechargement, String sha256, long tailleOctets,
              List<TypeOfPrompt> archetypesCibles) {
        this.id = id;
        this.nomAffiche = nomAffiche;
        this.specialite = specialite;
        this.description = description;
        this.tailleDisque = tailleDisque;
        this.ramRecommandee = ramRecommandee;
        this.nomFichier = nomFichier;
        this.urlTelechargement = urlTelechargement;
        this.sha256 = sha256;
        this.tailleOctets = tailleOctets;
        this.archetypesCibles = archetypesCibles;
    }

    public String getId() {
        return id;
    }

    public String getNomAffiche() {
        return nomAffiche;
    }

    public String getSpecialite() {
        return specialite;
    }

    public String getDescription() {
        return description;
    }

    public String getTailleDisque() {
        return tailleDisque;
    }

    public String getRamRecommandee() {
        return ramRecommandee;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public String getUrlTelechargement() {
        return urlTelechargement;
    }

    public String getSha256() {
        return sha256;
    }

    public long getTailleOctets() {
        return tailleOctets;
    }

    public List<TypeOfPrompt> getArchetypesCibles() {
        return archetypesCibles;
    }

    // Résout un alias utilisateur en ModelType (ex: "qwen", "gemma", "r1", "smollm", "code")
    public static Optional<ModelType> fromAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return Optional.empty();
        }
        String clean = alias.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        for (ModelType model : values()) {
            String idClean = model.id.replaceAll("[^a-z0-9]", "");
            if (idClean.equals(clean) || model.name().toLowerCase().replaceAll("[^a-z0-9]", "").equals(clean)) {
                return Optional.of(model);
            }
        }
        if (clean.contains("qwen") || clean.contains("code") || clean.contains("dev")) {
            return Optional.of(QWEN_CODER);
        }
        if (clean.contains("gemma") || clean.contains("google") || clean.contains("redact") || clean.contains("general")) {
            return Optional.of(GEMMA_GENERAL);
        }
        if (clean.contains("deepseek") || clean.contains("r1") || clean.contains("reason") || clean.contains("logique")) {
            return Optional.of(DEEPSEEK_REASONING);
        }
        if (clean.contains("smol") || clean.contains("fast") || clean.contains("rapide") || clean.contains("light")) {
            return Optional.of(SMOLLM_FAST);
        }
        return Optional.empty();
    }

    public static List<ModelType> getAllAvailable() {
        return Arrays.asList(values());
    }
}
