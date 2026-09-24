// Content script : intercepte l'envoi du prompt (Entree ou bouton d'envoi), le fait
// ameliorer par le modele local, puis le remet dans la zone de saisie.
//
// Parcours : l'utilisateur envoie -> l'envoi est bloque -> amelioration (5 a 10 s) ->
// le prompt ameliore remplace le sien -> il relit et envoie a nouveau, cette fois sans
// interception. Le prompt ne quitte la machine que lorsque l'utilisateur l'envoie au site.
//
// Injecte a document_start, AVANT tout script du site : les ecouteurs en phase de capture
// sur window s'executent dans l'ordre d'enregistrement. Enregistres apres ceux du site,
// ils arriveraient trop tard et le site aurait deja envoye le prompt.
// La zone de saisie (detection.js, injecte plus tard) est lue au moment de l'envoi.

(() => {
    const journaliser = (...elements) => console.log("[AIM2K26]", ...elements);

    const INDICES_ENVOI = /send|envoy|submit|soumettre/i;
    // Au-dela, l'amelioration est abandonnee : un service worker arrete par Chrome ne
    // doit pas bloquer l'envoi de prompts pour le reste de la conversation
    const DELAI_MAX_MS = 90_000;

    // Mode auto (parametre "Auto-correction") : amelioration, collage et envoi sans relecture.
    // Lu dans chrome.storage et suivi en direct : pas besoin de recharger la page.
    let modeAuto = false;
    // Interrupteur on/off de la barre AIM2K26 : desactivee, l'extension laisse tout passer
    let extensionActive = true;
    // Reglages de mise en forme transmis a l'outil avec chaque prompt ("auto" = pas de forcage)
    let formatSortie = "md";
    let llmCible = "auto";
    let langue = "auto";
    let modeleUtilise = "auto";
    // Reglages du modele local, transmis en texte (lus comme tels par l'outil)
    let creativite = "0.7";
    let longueurMax = "2048";
    // Copie du prompt ameliore dans le presse-papiers des son insertion
    let copieAuto = false;
    const appliquerParametres = (parametres = {}) => {
        modeAuto = Boolean(parametres.autoCorrection);
        formatSortie = parametres.formatSortie || "md";
        llmCible = parametres.llmCible || "auto";
        langue = parametres.langue || "auto";
        modeleUtilise = parametres.modeleUtilise || "auto";
        creativite = String(parametres.creativite ?? "0.7");
        longueurMax = String(parametres.longueurMax ?? "2048");
        copieAuto = Boolean(parametres.copieAuto);
        extensionActive = parametres.extensionActive !== false;
        // Extension coupee : les prompts partent sans nous, le dernier texte insere n'a plus
        // de sens (il laisserait passer, a la reactivation, un prompt non ameliore)
        if (!extensionActive) texteInsere = null;
    };
    chrome.storage.local.get("parametres").then(({ parametres }) => appliquerParametres(parametres));
    chrome.storage.onChanged.addListener((changements, zoneStockage) => {
        if (zoneStockage === "local" && changements.parametres) {
            appliquerParametres(changements.parametres.newValue);
        }
    });

    let enCours = false;
    // Texte mis dans la zone par la derniere amelioration (ou texte d'origine si elle a
    // echoue). Un envoi qui porte encore ce texte est un envoi relu : il passe. Tout autre
    // texte est un nouveau prompt, donc ameliore. Ne depend pas d'avoir vu passer l'envoi
    // precedent (bouton non reconnu, envoi par un raccourci du site...).
    let texteInsere = null;
    // L'utilisateur a-t-il tape dans la zone depuis l'insertion ? Sinon, l'envoi porte
    // forcement le texte insere, quel que soit le rendu qu'en a fait l'editeur du site.
    let retoucheDepuisInsertion = false;

    // Detection refaite a chaque envoi : la zone change a chaque message ou conversation
    const zone = () => globalThis.AIM2K26_ZONE && globalThis.AIM2K26_ZONE.trouver();
    const lireTexte = (element) => globalThis.AIM2K26_ZONE.lireTexte(element);
    // Lettres et chiffres seulement : l'editeur du site transforme le Markdown insere
    // ("# ROLE" devient un titre "ROLE", "**" disparait), le texte relu n'est donc pas
    // identique caractere pour caractere au texte insere
    const signature = (texte) => texte.toLowerCase().replace(/[^\p{L}\p{N}]/gu, "");

    function estTexteRelu(texte) {
        if (texteInsere === null) return false;
        // Aucune frappe depuis l'insertion : c'est le texte insere, sans comparaison
        // possible a rater (sinon boucle : chaque envoi relance une amelioration)
        if (!retoucheDepuisInsertion) return true;
        // Retouche : meme debut, a la mise en forme pres
        return signature(texte).startsWith(signature(texteInsere).slice(0, 20));
    }

    // --- Interception ---

    function bloquer(evenement) {
        evenement.preventDefault();
        // Au stade de la capture sur window : le gestionnaire du site ne recoit jamais l'evenement
        evenement.stopImmediatePropagation();
    }

    function intercepter(evenement) {
        if (!extensionActive) return;
        const zoneActuelle = zone();
        if (!zoneActuelle) return;
        const texte = lireTexte(zoneActuelle);
        // Rien a ameliorer : le site gere normalement
        if (!texte.trim()) return;

        if (enCours) {
            bloquer(evenement);
            return;
        }
        if (estTexteRelu(texte)) {
            texteInsere = null;
            journaliser("envoi relu : transmis au site");
            return;
        }
        bloquer(evenement);
        journaliser(`envoi intercepté (${evenement.type}) : amélioration demandée`);
        ameliorer(zoneActuelle, texte);
    }

    // Le bouton clique est-il l'envoi de cette zone ?
    // (un <button> est "submit" par defaut : seul un type="submit" explicite compte,
    // sinon le bouton "joindre un fichier" serait pris pour un envoi)
    function estBoutonEnvoi(bouton, zoneActuelle) {
        const indices = [bouton.getAttribute("aria-label"), bouton.getAttribute("data-testid"),
            bouton.getAttribute("title"), bouton.id].join(" ");
        if (!INDICES_ENVOI.test(indices) && bouton.getAttribute("type") !== "submit") return false;

        const formulaire = zoneActuelle.closest("form");
        if (formulaire && formulaire.contains(bouton)) return true;
        // Sans formulaire commun (ex. Claude), le bouton peut etre loin dans l'arbre du DOM
        // mais il est toujours colle a la zone a l'ecran
        const zoneRect = zoneActuelle.getBoundingClientRect();
        const boutonRect = bouton.getBoundingClientRect();
        const ecartVertical = Math.max(0, boutonRect.top - zoneRect.bottom, zoneRect.top - boutonRect.bottom);
        return ecartVertical < 120 && boutonRect.left < zoneRect.right + 120 && boutonRect.right > zoneRect.left - 120;
    }

    window.addEventListener("keydown", (evenement) => {
        if (evenement.key !== "Enter" || evenement.shiftKey || evenement.altKey || evenement.isComposing) return;
        const zoneActuelle = zone();
        // Entree tapee ailleurs (recherche, renommage d'une conversation...) : pas un envoi
        if (!zoneActuelle || !zoneActuelle.contains(evenement.target)) return;
        // Sur ces sites, Entree seule ajoute une ligne : seul Ctrl/Cmd+Entree envoie
        const toucheEnvoi = globalThis.AIM2K26_ENTREE_NOUVELLE_LIGNE.has(location.hostname)
            ? evenement.ctrlKey || evenement.metaKey
            : true;
        if (toucheEnvoi) intercepter(evenement);
    }, true);

    window.addEventListener("click", (evenement) => {
        const bouton = evenement.target.closest && evenement.target.closest("button, [role='button']");
        const zoneActuelle = zone();
        if (bouton && zoneActuelle && estBoutonEnvoi(bouton, zoneActuelle)) intercepter(evenement);
    }, true);

    // Frappe de l'utilisateur dans la zone (les insertions de l'extension se font pendant
    // enCours, donc ne comptent pas)
    window.addEventListener("input", (evenement) => {
        const zoneActuelle = zone();
        if (!enCours && zoneActuelle && zoneActuelle.contains(evenement.target)) retoucheDepuisInsertion = true;
    }, true);

    // Pas d'ecoute de "submit" : un <button> sans type dans un formulaire le declenche
    // aussi (ex. "joindre un fichier"), ce qui lancerait une amelioration parasite.
    // Tout envoi passe de toute facon par Entree ou par un clic, deja interceptes.

    // --- Ecriture dans la zone ---

    function toutSelectionner(element) {
        const selection = window.getSelection();
        const plage = document.createRange();
        plage.selectNodeContents(element);
        selection.removeAllRanges();
        selection.addRange(plage);
    }

    const attendre = (ms) => new Promise((resoudre) => setTimeout(resoudre, ms));

    // Le texte de la zone est-il bien le texte insere, en entier ? Debut ET fin sont
    // compares : un texte ampute de son debut ou de sa fin ne doit jamais partir.
    // (a la mise en forme pres : l'editeur convertit le Markdown colle)
    function estEcrit(element, texte) {
        const attendu = signature(texte);
        const present = signature(lireTexte(element));
        return present.startsWith(attendu.slice(0, 40)) && present.endsWith(attendu.slice(-40));
    }

    // Les sites sont en React : modifier le DOM directement ne suffit pas, leur etat
    // interne garderait l'ancien texte et c'est lui qui serait envoye.
    async function ecrireTexte(element, texte) {
        element.focus();
        if (element instanceof HTMLTextAreaElement) {
            // Setter natif : React surcharge celui de l'element et ignorerait la modification
            Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value").set.call(element, texte);
            element.dispatchEvent(new Event("input", { bubbles: true }));
            return estEcrit(element, texte);
        }

        // Editeurs riches (ProseMirror, Lexical, Quill) : un collage garde les paragraphes
        toutSelectionner(element);
        const donnees = new DataTransfer();
        donnees.setData("text/plain", texte);
        element.dispatchEvent(new ClipboardEvent("paste", { clipboardData: donnees, bubbles: true, cancelable: true }));
        // Certains editeurs traitent le collage en differe : verifier trop tot relancerait
        // une saisie par-dessus un collage en cours, et les deux se melangent (debut perdu)
        await attendre(150);
        if (estEcrit(element, texte)) return true;

        // Editeur qui ignore les collages simules : saisie simulee
        journaliser("collage simulé ignoré ou incomplet : saisie simulée");
        toutSelectionner(element);
        // L'editeur ne lit la nouvelle selection qu'au "selectionchange", lui aussi differe :
        // sans ce delai, la saisie remplacerait l'ancienne selection et non tout le texte
        await attendre(50);
        document.execCommand("insertText", false, texte);
        await attendre(50);
        return estEcrit(element, texte);
    }

    // --- Envoi automatique ---

    function trouverBoutonEnvoi(zoneActuelle) {
        return [...document.querySelectorAll("button, [role='button']")].find((bouton) =>
            estBoutonEnvoi(bouton, zoneActuelle)
            && !bouton.disabled && bouton.getAttribute("aria-disabled") !== "true"
            && bouton.getClientRects().length > 0);
    }

    // Envoi du texte insere comme le ferait l'utilisateur. Il repasse par nos propres
    // ecouteurs, qui le reconnaissent comme "relu" et le laissent aller au site.
    async function envoyerAutomatiquement(zoneActuelle) {
        // Le site doit d'abord prendre en compte le nouveau texte : son bouton d'envoi
        // s'active quelques instants apres (etat React)
        for (let essai = 0; essai < 10; essai++) {
            await attendre(100);
            const bouton = trouverBoutonEnvoi(zoneActuelle);
            if (bouton) {
                journaliser("mode auto : envoi par le bouton du site");
                bouton.click();
                return;
            }
        }
        journaliser("mode auto : aucun bouton d'envoi actif, envoi par Entrée");
        zoneActuelle.dispatchEvent(new KeyboardEvent("keydown", {
            key: "Enter", code: "Enter", keyCode: 13, which: 13, bubbles: true, cancelable: true
        }));
    }

    // --- Amelioration ---

    // Presse-papiers : la page a le focus (l'utilisateur vient d'envoyer), sinon le
    // navigateur refuse l'ecriture. Un echec ne doit pas gener l'insertion deja faite.
    async function copier(texte) {
        try {
            await navigator.clipboard.writeText(texte);
            journaliser("prompt amélioré copié dans le presse-papiers");
        } catch (erreur) {
            journaliser("copie automatique impossible :", erreur.message);
        }
    }

    // IA a laquelle le prompt est destine : choisie dans les parametres, ou deduite du site
    function cibleEffective() {
        if (llmCible !== "auto") return llmCible;
        return globalThis.AIM2K26_CIBLES[location.hostname] || "auto";
    }

    async function ameliorer(zoneActuelle, texte) {
        enCours = true;
        let envoyerEnsuite = false;
        const toast = globalThis.afficherToastAim2k26("Amélioration du prompt…", null);
        try {
            const resultat = await Promise.race([
                chrome.runtime.sendMessage({
                    type: "ameliorer", prompt: texte, format: formatSortie, cible: cibleEffective(), langue,
                    modele: modeleUtilise, creativite, longueurMax
                }),
                new Promise((resoudre) => setTimeout(
                    () => resoudre({ ok: false, message: "délai dépassé" }), DELAI_MAX_MS))
            ]);
            journaliser("réponse de l'amélioration :", resultat && resultat.ok
                ? `ok (${resultat.modele}, ${resultat.secondes} s)` : resultat);
            // Par defaut (echec), c'est le texte d'origine qui pourra partir tel quel
            texteInsere = texte;
            retoucheDepuisInsertion = false;
            if (!resultat || !resultat.ok) {
                const cause = (resultat && resultat.message) || "erreur inconnue";
                toast.changer(modeAuto
                    ? `Amélioration impossible (${cause}) : prompt envoyé tel quel.`
                    : `Amélioration impossible (${cause}). Entrée pour envoyer tel quel.`, 5000);
                envoyerEnsuite = modeAuto;
            } else if (!zoneActuelle.isConnected) {
                // L'utilisateur a change de conversation pendant l'amelioration
                toast.fermer();
            } else if (await ecrireTexte(zoneActuelle, resultat.ameliore)) {
                texteInsere = resultat.ameliore;
                journaliser(`prompt amélioré inséré dans la zone de saisie (source : ${resultat.source})`);
                if (copieAuto) await copier(resultat.ameliore);
                const suite = modeAuto ? "Envoyé automatiquement." : "Entrée pour envoyer.";
                if (resultat.source === "nlp") {
                    // Repli sans modele : meta-prompt du moteur NLP, utile mais moins fiable
                    toast.changer("Amélioré par le NLP seul, fiabilité réduite : installez un modèle "
                        + `pour de meilleurs résultats. ${suite}`, 6000, "avertissement");
                } else {
                    toast.changer(modeAuto ? "Prompt amélioré et envoyé" : "Prompt amélioré · Entrée pour l'envoyer", 3500);
                }
                envoyerEnsuite = modeAuto;
            } else {
                journaliser("échec de l'insertion : le texte de la zone n'a pas changé");
                // Mode auto : jamais d'envoi d'un texte que l'on n'a pas pu verifier
                toast.changer("Impossible d'insérer le prompt amélioré. Entrée pour envoyer tel quel.", 5000);
            }
        } catch (erreur) {
            texteInsere = texte;
            journaliser("erreur :", erreur);
            // Extension rechargee pendant que la page etait ouverte : ce script est orphelin
            toast.changer("Extension mise à jour : rechargez la page.", 5000);
        } finally {
            // Reussie ou non, l'amelioration ne doit jamais bloquer l'envoi suivant :
            // texteInsere laisse toujours passer le texte present dans la zone
            enCours = false;
        }
        // Apres enCours = false : sinon notre propre intercepteur bloquerait cet envoi
        if (envoyerEnsuite && zoneActuelle.isConnected) {
            await envoyerAutomatiquement(zoneActuelle);
        }
    }
})();
