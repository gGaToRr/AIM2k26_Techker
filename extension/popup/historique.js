// Lecture et mise en forme de l'historique des prompts ameliores, partagees par la
// page principale (liste) et la page de detail. Les entrees sont ecrites par background.js.

const LIBELLES_TYPES = {
    APPRENTISSAGE_TUTORIEL: "Apprentissage",
    CONCEPTION_ARCHITECTURE: "Conception",
    DEPANNAGE_DIAGNOSTIC: "Dépannage",
    CREATION_REDACTION: "Rédaction",
    PROTOCOLE_RECETTE: "Protocole",
    COMPARAISON_DECISION: "Comparaison",
    CONCEPT_VULGARISATION: "Vulgarisation"
};

function libelleType(type) {
    return LIBELLES_TYPES[type] || "Autre";
}

function deuxChiffres(nombre) {
    return String(nombre).padStart(2, "0");
}

// "Aujourd'hui, 14:32", "Hier, 18:47" ou "12/09, 10:05"
function formaterDateRelative(horodatage) {
    const date = new Date(horodatage);
    const heure = `${deuxChiffres(date.getHours())}:${deuxChiffres(date.getMinutes())}`;
    const minuit = new Date();
    minuit.setHours(0, 0, 0, 0);
    if (date >= minuit) return `Aujourd'hui, ${heure}`;
    if (date >= minuit - 24 * 3600 * 1000) return `Hier, ${heure}`;
    return `${deuxChiffres(date.getDate())}/${deuxChiffres(date.getMonth() + 1)}, ${heure}`;
}

// "23/09, 14:32"
function formaterDateCourte(horodatage) {
    const date = new Date(horodatage);
    return `${deuxChiffres(date.getDate())}/${deuxChiffres(date.getMonth() + 1)}, `
        + `${deuxChiffres(date.getHours())}:${deuxChiffres(date.getMinutes())}`;
}

async function lireHistorique() {
    const { historique = [] } = await chrome.storage.local.get("historique");
    return historique;
}
