// Content script : repere la zone ou l'utilisateur tape son prompt sur les sites de LLM.
//
// Injecte par Chrome (manifest.json > content_scripts) dans un monde isole : il voit le
// DOM de la page mais pas ses variables JavaScript, et la page ne voit pas les siennes.
//
// Ces sites sont des applications React : la zone de saisie est souvent un <div
// contenteditable> (ProseMirror), recree a chaque changement de conversation sans
// rechargement. Un MutationObserver la retrouve donc a chaque fois.

(() => {
    // Mode developpement : traces en console.
    // Le texte du prompt n'est jamais journalise, seulement sa longueur.
    const DEBOGAGE = true;

    // Selecteurs connus par site : contenu/sites.js, charge juste avant ce fichier
    const SELECTEURS_PAR_SITE = globalThis.AIM2K26_SITES || {};

    const CANDIDATS_GENERIQUES = "textarea, [contenteditable='true'], [role='textbox']";
    const INDICES_PROMPT = /prompt|message|ask|chat|question|écri|ecri|demand|send|reply/i;

    let zoneActuelle = null;
    // Le service worker n'est prevenu qu'une fois par chargement de page, pas a chaque
    // changement de conversation (la zone est alors recreee sans rechargement)
    let chargementSignale = false;

    function journaliser(...elements) {
        if (DEBOGAGE) console.log("[AIM2K26]", ...elements);
    }

    function estVisible(element) {
        const rect = element.getBoundingClientRect();
        const style = getComputedStyle(element);
        return rect.width > 0 && rect.height > 0 && style.visibility !== "hidden" && style.display !== "none";
    }

    // Une zone de prompt est large, visible, modifiable, et souvent annoncee comme telle
    function scoreGenerique(element) {
        if (!estVisible(element) || element.closest("nav, header, aside")) return -1;
        if (element.disabled || element.readOnly) return -1;
        const rect = element.getBoundingClientRect();
        if (rect.width < 200) return -1;

        const indices = [element.id, element.getAttribute("aria-label"), element.getAttribute("placeholder"),
            element.getAttribute("data-placeholder"), element.getAttribute("name")].join(" ");
        let score = rect.width / 100;
        if (INDICES_PROMPT.test(indices)) score += 10;
        // Sur ces sites, la zone de saisie est en bas de l'ecran
        if (rect.top > window.innerHeight / 2) score += 3;
        if (element === document.activeElement) score += 5;
        return score;
    }

    // Meilleur candidat selon le score generique (null si aucun n'est plausible)
    function meilleurCandidat(elements) {
        let meilleure = null;
        let meilleurScore = 0;
        for (const element of elements) {
            const score = scoreGenerique(element);
            if (score > meilleurScore) {
                meilleure = element;
                meilleurScore = score;
            }
        }
        return meilleure;
    }

    function trouverZone() {
        for (const selecteur of SELECTEURS_PAR_SITE[location.hostname] || []) {
            // Un selecteur large ("textarea") peut viser plusieurs elements (champ de
            // renommage, recherche...) : le score generique les departage
            const zone = meilleurCandidat(document.querySelectorAll(selecteur));
            if (zone) return zone;
        }
        return meilleurCandidat(document.querySelectorAll(CANDIDATS_GENERIQUES));
    }

    // textarea : .value ; contenteditable : texte affiche (ProseMirror y garde les sauts de ligne)
    function lireTexte(zone) {
        return zone instanceof HTMLTextAreaElement ? zone.value : zone.innerText;
    }

    function surSaisie() {
        journaliser(`saisie : ${lireTexte(zoneActuelle).trim().length} caractères`);
    }

    function attacher(zone) {
        if (zoneActuelle === zone) return;
        if (zoneActuelle) {
            zoneActuelle.removeEventListener("input", surSaisie);
        }
        zoneActuelle = zone;
        zone.addEventListener("input", surSaisie);
        zone.dataset.aim2k26 = "zone-prompt";
        journaliser("zone de saisie détectée :", zone);

        if (!chargementSignale) {
            chargementSignale = true;
            // Le service worker decide (toast seulement si le popup est ferme).
            // catch : extension rechargee pendant que la page etait ouverte, ce script est orphelin
            chrome.runtime.sendMessage({ type: "site-charge", site: location.hostname })
                .then((reponse) => {
                    if (reponse && reponse.afficherToast) globalThis.afficherToastAim2k26("AIM2K26 loaded properly");
                })
                .catch(() => {});
        }
    }

    function verifier() {
        // La zone a pu etre retiree du DOM (changement de conversation)
        if (zoneActuelle && !zoneActuelle.isConnected) {
            journaliser("zone de saisie retirée de la page");
            zoneActuelle = null;
        }
        const zone = trouverZone();
        if (zone) attacher(zone);
    }

    // Les mutations arrivent en rafales : une seule verification par rafale.
    // (setTimeout et non requestAnimationFrame, suspendu dans les onglets en arriere-plan)
    let verificationPrevue = false;
    const observateur = new MutationObserver(() => {
        if (verificationPrevue) return;
        verificationPrevue = true;
        setTimeout(() => {
            verificationPrevue = false;
            verifier();
        }, 50);
    });

    observateur.observe(document.documentElement, { childList: true, subtree: true });
    verifier();

    // Partage avec amelioration.js (meme monde isole). trouver() refait la detection a
    // l'instant : apres un envoi, le site recree la zone et l'observateur (differe de
    // 50 ms) n'a pas forcement encore vu la nouvelle.
    globalThis.AIM2K26_ZONE = {
        trouver: () => {
            verifier();
            return zoneActuelle;
        },
        lireTexte
    };
})();
