// Page des parametres : chaque reglage branche est enregistre dans chrome.storage.local
// ("parametres"). Les scripts injectes dans les sites le lisent et suivent ses changements
// en direct, sans recharger la page.

const REGLAGES_BRANCHES = {
    // Mode auto : amelioration, collage et envoi du prompt sans relecture
    "auto-correction": "autoCorrection",
    // Mise en forme du prompt ameliore : txt, md ou json (option -o de l'outil)
    "format-sortie": "formatSortie",
    // IA destinataire (option -a) ; "auto" la deduit du site ou le prompt est envoye
    "llm-cible": "llmCible",
    // Langue de redaction du prompt ameliore (option -l) ; "auto" garde celle du prompt
    "langue": "langue",
    // Modele local qui reecrit le prompt (option -m) ; "auto" laisse le routage choisir
    "modele-utilise": "modeleUtilise",
    // Temperature du modele local (0 a 1)
    "creativite": "creativite",
    // Nombre maximal de tokens produits par le modele local
    "longueur": "longueurMax",
    // Copie du prompt ameliore dans le presse-papiers des qu'il est insere
    "copie-auto": "copieAuto"
};

// Interrupteur : coche ou non. Liste de choix : valeur choisie, sinon celle par defaut du HTML.
const lireValeur = (element) => element.type === "checkbox" ? element.checked : element.value;

async function chargerParametres() {
    const { parametres = {} } = await chrome.storage.local.get("parametres");
    for (const [idElement, cle] of Object.entries(REGLAGES_BRANCHES)) {
        const element = document.getElementById(idElement);
        if (element.type === "checkbox") {
            element.checked = Boolean(parametres[cle]);
        } else if (parametres[cle] !== undefined) {
            element.value = parametres[cle];
        }
        element.addEventListener("change", async () => {
            const { parametres: actuels = {} } = await chrome.storage.local.get("parametres");
            await chrome.storage.local.set({ parametres: { ...actuels, [cle]: lireValeur(element) } });
        });
    }
}

// Desinstallation : Chrome affiche sa propre confirmation, puis efface le stockage de
// l'extension (parametres, historique). Les modeles installes sur la machine restent :
// ils se gerent depuis la page Modeles.
document.getElementById("supprimer-tout").addEventListener("click", () => {
    chrome.management.uninstallSelf({ showConfirmDialog: true }).catch(() => {
        // Confirmation refusee par l'utilisateur : rien a faire
    });
});

chargerParametres();
