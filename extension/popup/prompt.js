// Page de detail d'un prompt de l'historique (prompt.html?id=...)

async function afficherDetail() {
    const id = new URLSearchParams(location.search).get("id");
    const entree = (await lireHistorique()).find((e) => e.id === id);
    const detail = document.getElementById("detail-prompt");

    if (!entree) {
        const message = document.createElement("p");
        message.className = "message-liste";
        message.textContent = "Ce prompt n'est plus dans l'historique.";
        detail.replaceChildren(message);
        return;
    }

    const remplir = (idElement, texte) => {
        document.getElementById(idElement).textContent = texte;
    };
    remplir("titre-prompt", `Prompt du ${formaterDateCourte(entree.date)}`);
    remplir("info-type", libelleType(entree.type));
    remplir("info-source", entree.source === "nlp" ? "NLP seul" : (entree.modele || "Modèle local"));
    remplir("info-site", entree.site || "—");
    remplir("info-score", Number.isFinite(entree.score) ? `${entree.score} / 100` : "—");
    remplir("prompt-original", entree.original);
    remplir("prompt-ameliore", entree.ameliore);
}

afficherDetail();
