// Sites de LLM pris en charge et selecteurs de leur zone de saisie.
//
// Charge en premier (manifest.json > content_scripts, document_start). Les valeurs sont
// posees sur globalThis, commun a tous les scripts de l'extension dans la page.
//
// Les selecteurs vont du plus precis au plus general. Les interfaces changent souvent :
// si aucun ne correspond, detection.js bascule sur sa detection generique.
//
// Ajouter un site = une entree ici + son motif dans "matches" du manifest.

globalThis.AIM2K26_SITES = {
    // OpenAI : div contenteditable ProseMirror, identifiant stable
    "chatgpt.com": ["#prompt-textarea", "div.ProseMirror[contenteditable='true']"],
    "chat.openai.com": ["#prompt-textarea"],

    // Anthropic : ProseMirror dans un fieldset
    "claude.ai": ["div.ProseMirror[contenteditable='true']", "fieldset div[contenteditable='true']"],

    // Google
    "gemini.google.com": ["rich-textarea div[contenteditable='true']", "div.ql-editor[contenteditable='true']"],
    "aistudio.google.com": ["ms-prompt-input-wrapper textarea", "textarea[aria-label*='prompt' i]"],

    // Mistral (Le Chat) : editeur ProseMirror, ancien textarea en secours
    "chat.mistral.ai": ["div.ProseMirror[contenteditable='true']", "textarea[name='message.text']", "textarea"],

    // DeepSeek
    "chat.deepseek.com": ["textarea#chat-input", "textarea"],

    // xAI (Grok) : editeur riche recent, textarea sur les versions precedentes
    "grok.com": ["div.ProseMirror[contenteditable='true']", "textarea[aria-label*='Grok' i]", "textarea"],

    // Microsoft Copilot
    "copilot.microsoft.com": ["textarea#userInput", "textarea"],

    // Meta AI : editeur Lexical (role textbox)
    "www.meta.ai": ["div[contenteditable='true'][role='textbox']"],

    // Perplexity : editeur Lexical, ancien textarea en secours
    "www.perplexity.ai": ["#ask-input", "div[contenteditable='true'][role='textbox']", "textarea"],
    "perplexity.ai": ["#ask-input", "div[contenteditable='true'][role='textbox']", "textarea"],

    // Alibaba (Qwen)
    "chat.qwen.ai": ["textarea#chat-input", "textarea"],

    // Moonshot (Kimi)
    "www.kimi.com": ["div.chat-input-editor[contenteditable='true']", "div[contenteditable='true']"],
    "kimi.com": ["div.chat-input-editor[contenteditable='true']", "div[contenteditable='true']"],

    // Zhipu (GLM / Z.ai)
    "chat.z.ai": ["textarea#chat-input", "textarea"],

    // Quora (Poe) : textarea a hauteur variable
    "poe.com": ["textarea[class*='GrowingTextArea']", "textarea"],

    // Hugging Face (HuggingChat, sous /chat uniquement : voir le manifest)
    "huggingface.co": ["form textarea", "textarea"]
};

// Sites ou Entree seule ajoute une ligne (l'envoi se fait avec Ctrl+Entree) :
// y intercepter Entree bloquerait la saisie
globalThis.AIM2K26_ENTREE_NOUVELLE_LIGNE = new Set(["aistudio.google.com"]);

// Reglage "LLM cible" sur Automatique : l'IA est deduite du site. Les autres sites
// recoivent le prompt sans adaptation. Meta AI n'y figure pas : l'adaptation LLaMA
// ajoute des jetons de gabarit de chat, faits pour une API et non pour une zone de saisie.
globalThis.AIM2K26_CIBLES = {
    "chatgpt.com": "gpt",
    "chat.openai.com": "gpt",
    "claude.ai": "claude",
    "gemini.google.com": "gemini",
    "aistudio.google.com": "gemini",
    "chat.deepseek.com": "deepseek"
};
