// Panneau de l'extension, injecte dans l'onglet au clic sur l'icone (background.js).
//
// Remplace la popup de Chrome, dont le cadre rectangulaire est dessine par le navigateur :
// ici le panneau fait partie de la page, ses coins arrondis laissent voir le site derriere.
// Les pages de la popup (popup.html, parametres.html...) y sont affichees telles quelles
// dans une iframe : elles gardent l'acces aux API de l'extension.
//
// Injecte a chaque clic : un clic ouvre le panneau, le suivant le ferme. L'etat reste
// dans le monde isole des content scripts, commun a toutes les injections dans l'onglet.

(() => {
    // fermer : fonction de l'injection qui a ouvert le panneau, seule a connaitre ses ecouteurs
    const etat = globalThis.AIM2K26_PANNEAU || (globalThis.AIM2K26_PANNEAU = { hote: null, fermer: null });

    let hote = null;
    let iframe = null;

    function fermer() {
        hote.remove();
        etat.hote = null;
        etat.fermer = null;
        window.removeEventListener("mousedown", clicExterieur, true);
        window.removeEventListener("keydown", toucheEchap, true);
        window.removeEventListener("message", messageDuPanneau);
    }

    // Comme la popup de Chrome : un clic a cote ferme le panneau
    // (un clic dans l'iframe n'arrive jamais ici, il reste dans son document)
    function clicExterieur(evenement) {
        if (!evenement.composedPath().includes(hote)) fermer();
    }

    function toucheEchap(evenement) {
        if (evenement.key === "Escape") fermer();
    }

    // Echap tape dans le panneau : la page de l'iframe le signale (popup/panneau.js)
    function messageDuPanneau(evenement) {
        if (evenement.source === iframe.contentWindow
            && evenement.data && evenement.data.aim2k26 === "fermer") {
            fermer();
        }
    }

    function ouvrir() {
        hote = document.createElement("div");
        // Shadow DOM ferme : le CSS du site ne touche pas au panneau, et inversement
        const racine = hote.attachShadow({ mode: "closed" });
        racine.innerHTML = `
            <style>
                :host { all: initial; }
                iframe {
                    position: fixed;
                    top: 8px;
                    right: 8px;
                    z-index: 2147483647;
                    width: 720px;
                    height: 600px;
                    max-width: calc(100vw - 16px);
                    max-height: calc(100vh - 16px);
                    border: none;
                    background: transparent;
                    /* Meme schema de couleurs que la page de l'iframe (popup.css) : sinon
                       Chrome peint un fond opaque derriere elle et les coins redeviennent carres */
                    color-scheme: light;
                }
            </style>
            <iframe allow="clipboard-write" title="AIM2K26"></iframe>`;
        iframe = racine.querySelector("iframe");
        iframe.src = chrome.runtime.getURL("popup/popup.html");
        document.documentElement.appendChild(hote);

        etat.hote = hote;
        etat.fermer = fermer;
        window.addEventListener("mousedown", clicExterieur, true);
        window.addEventListener("keydown", toucheEchap, true);
        window.addEventListener("message", messageDuPanneau);
    }

    if (etat.hote && etat.hote.isConnected) {
        etat.fermer();
    } else {
        // Panneau retire par le site (rechargement de son DOM) : ses ecouteurs aussi
        if (etat.fermer) etat.fermer();
        ouvrir();
    }
})();
