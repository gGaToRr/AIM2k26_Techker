// Service worker : porte les telechargements de modeles.
// Un telechargement dure environ une minute ; lance depuis le popup, il serait tue a la
// fermeture de celui-ci. Ici, il continue : Chrome garde le service worker en vie tant
// qu'un port vers le connecteur local (connectNative) est ouvert.

const CONNECTEUR = "com.aim2k26.connecteur";

// id du modele -> { lus, total, vitesse } pendant le telechargement
const enCours = new Map();

// Historique des prompts ameliores, le plus recent en premier. Il reste sur la machine
// (chrome.storage.local) et le popup l'affiche en direct via chrome.storage.onChanged.
const TAILLE_MAX_HISTORIQUE = 50;

async function enregistrerDansHistorique(original, resultat, onglet) {
    let site = "";
    try {
        site = new URL(onglet.url).hostname;
    } catch (erreur) {
        // Onglet sans URL lisible : l'entree est gardee, sans site
    }
    const entree = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        date: Date.now(),
        site,
        original,
        ameliore: resultat.ameliore,
        source: resultat.source,
        modele: resultat.modele,
        type: resultat.type,
        score: resultat.score
    };
    const { historique = [] } = await chrome.storage.local.get("historique");
    await chrome.storage.local.set({ historique: [entree, ...historique].slice(0, TAILLE_MAX_HISTORIQUE) });
}

// Notification seulement si le popup est ferme : ouvert, la page affiche deja le resultat.
// Discrete : pas de son, pas d'insistance, elle disparait seule.
async function notifierSiPopupFerme(modele, nom, ok, message) {
    const popups = await chrome.runtime.getContexts({ contextTypes: ["POPUP"] });
    if (popups.length > 0) {
        return;
    }
    chrome.notifications.create(`telechargement-${modele}`, {
        type: "basic",
        iconUrl: "icones/icone-128.png",
        title: ok ? `${nom} est installé` : `Échec du téléchargement`,
        message: ok ? "Le modèle est prêt à être utilisé." : `${nom} : ${message || "erreur inconnue"}`,
        priority: -1,
        silent: true
    });
}

// Visite d'un site de LLM : le content script affiche un toast dans la page,
// seulement si le popup est ferme (ouvert, l'utilisateur est deja dans l'extension)
async function popupFerme() {
    const popups = await chrome.runtime.getContexts({ contextTypes: ["POPUP"] });
    return popups.length === 0;
}

function diffuser(message) {
    // Aucun popup ouvert pour ecouter : ce n'est pas une erreur
    chrome.runtime.sendMessage(message).catch(() => {});
}

function telecharger(modele, nom) {
    if (enCours.has(modele)) {
        return;
    }
    enCours.set(modele, { lus: 0, total: 0, vitesse: 0 });
    diffuser({ type: "progression", modele, ...enCours.get(modele) });

    const port = chrome.runtime.connectNative(CONNECTEUR);
    let termine = false;

    const finir = (ok, message) => {
        if (termine) return;
        termine = true;
        enCours.delete(modele);
        diffuser({ type: "fin", modele, ok, message });
        notifierSiPopupFerme(modele, nom || modele, ok, message);
    };

    port.onMessage.addListener((message) => {
        if (message.type === "progression") {
            enCours.set(modele, { lus: message.lus, total: message.total, vitesse: message.vitesse });
            diffuser({ type: "progression", modele, ...enCours.get(modele) });
        } else if (message.type === "fin") {
            finir(message.ok, message.message);
            port.disconnect();
        }
    });

    // Connecteur absent ou arrete avant la fin
    port.onDisconnect.addListener(() => {
        const erreur = chrome.runtime.lastError;
        finir(false, erreur ? erreur.message : "connecteur interrompu");
    });

    port.postMessage({ action: "telecharger-modele", modele });
}

chrome.runtime.onMessage.addListener((message, expediteur, repondre) => {
    if (message.type === "ameliorer") {
        // Prompt intercepte sur un site : amelioration par le modele local (5 a 10 s)
        const demande = {
            action: "ameliorer", prompt: message.prompt,
            format: message.format, cible: message.cible, langue: message.langue,
            modele: message.modele, creativite: message.creativite, longueurMax: message.longueurMax
        };
        chrome.runtime.sendNativeMessage(CONNECTEUR, demande, (reponse) => {
            if (chrome.runtime.lastError) {
                repondre({ ok: false, message: chrome.runtime.lastError.message });
            } else {
                const resultat = reponse.resultat || { ok: false, message: reponse.erreur || "réponse vide" };
                if (resultat.ok) {
                    enregistrerDansHistorique(message.prompt, resultat, expediteur.tab || {});
                }
                repondre(resultat);
            }
        });
        // Reponse asynchrone : le canal doit rester ouvert
        return true;
    } else if (message.type === "site-charge") {
        popupFerme().then((ferme) => repondre({ afficherToast: ferme }));
        // Reponse asynchrone : le canal doit rester ouvert
        return true;
    } else if (message.type === "telecharger") {
        telecharger(message.modele, message.nom);
        repondre({ ok: true });
    } else if (message.type === "etat-telechargements") {
        repondre(Object.fromEntries(enCours));
    }
});

// Clic sur l'icone : panneau injecte dans l'onglet (contenu/panneau.js), aux coins arrondis.
// Chrome interdit l'injection sur ses propres pages (chrome://, Nouvel onglet, Web Store...) :
// la popup classique prend alors le relais.
const POPUP = "popup/popup.html";

chrome.action.onClicked.addListener(async (onglet) => {
    try {
        await chrome.scripting.executeScript({ target: { tabId: onglet.id }, files: ["contenu/panneau.js"] });
    } catch (erreur) {
        // Popup propre a cet onglet : les clics suivants l'ouvrent directement, sans nouvel essai
        await chrome.action.setPopup({ tabId: onglet.id, popup: POPUP });
        await chrome.action.openPopup({ windowId: onglet.windowId }).catch(() => {
            // Fenetre sans focus : la popup s'ouvrira au prochain clic
        });
    }
});

// Nouvelle page dans l'onglet : l'injection est peut-etre possible, retour au panneau.
// (changement.url n'est fourni qu'avec la permission "tabs" : le statut suffit)
chrome.tabs.onUpdated.addListener((idOnglet, changement) => {
    if (changement.status === "loading") chrome.action.setPopup({ tabId: idOnglet, popup: "" });
});
