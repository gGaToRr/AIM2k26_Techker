// Barre AIM2K26 affichee sous le bloc de saisie du site (au-dessus s'il n'y a pas de place) :
//  - le badge "Mode auto", visible quand le parametre "Auto-correction" est actif ;
//  - un interrupteur qui active ou desactive l'extension sur les sites. Il reste visible
//    extension desactivee, pour pouvoir la reactiver.
//
// Superposee a la page (position fixe) plutot qu'inseree dans le compositeur du site :
// ajouter un element dans l'arbre gere par React pourrait casser son rendu.
// Suit les parametres en direct (chrome.storage).

(() => {
    const ECART = 8;

    let modeAuto = false;
    let extensionActive = true;
    let hote = null;
    let barre = null;
    let badge = null;
    let interrupteur = null;

    function creer() {
        hote = document.createElement("div");
        hote.id = "aim2k26-barre";
        const racine = hote.attachShadow({ mode: "closed" });
        racine.innerHTML = `
            <style>
                :host { all: initial; }

                .barre {
                    position: fixed;
                    z-index: 2147483646;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    gap: 6px;
                    /* Seul l'interrupteur capte les clics : le reste ne gene jamais la page */
                    pointer-events: none;
                    font: 12px/1 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
                    opacity: 0;
                    transition: opacity 0.3s ease;
                }

                .barre.visible { opacity: 1; }

                /* Pastille commune : fond sombre translucide, ombre douce */
                .pastille {
                    position: relative;
                    height: 24px;
                    border-radius: 999px;
                    overflow: hidden;
                    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.35);
                }

                .interieur {
                    position: relative;
                    display: flex;
                    align-items: center;
                    gap: 7px;
                    height: calc(100% - 3px);
                    margin: 1.5px;
                    padding: 0 12px 0 9px;
                    border-radius: 999px;
                    background: rgba(17, 17, 20, 0.92);
                    backdrop-filter: blur(8px);
                    white-space: nowrap;
                }

                /* --- Badge "Mode auto" --- */

                .badge[hidden] { display: none; }

                /* Contour anime : un degrade conique qui tourne sans fin sous le contenu */
                .contour {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    width: 400px;
                    height: 400px;
                    margin: -200px 0 0 -200px;
                    background: conic-gradient(#7b1e2b, #e11d48, #f59e0b, #7b1e2b 60%, #7b1e2b);
                    animation: tourner 3s linear infinite;
                }

                /* Boucle du logo qui se dessine puis s'efface, sans fin */
                svg {
                    width: 16px;
                    height: 16px;
                    flex-shrink: 0;
                }

                path {
                    fill: none;
                    stroke: url(#degrade);
                    stroke-width: 3.2;
                    stroke-linecap: round;
                    stroke-linejoin: round;
                    stroke-dasharray: 100;
                    animation: dessiner 2.4s ease-in-out infinite;
                }

                .titre {
                    font-weight: 700;
                    color: #ffffff;
                }

                .separateur {
                    width: 3px;
                    height: 3px;
                    border-radius: 50%;
                    background: rgba(255, 255, 255, 0.35);
                }

                .detail {
                    color: rgba(255, 255, 255, 0.72);
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                /* --- Interrupteur on/off --- */

                .interrupteur {
                    pointer-events: auto;
                    cursor: pointer;
                    border: 1px solid rgba(255, 255, 255, 0.14);
                    padding: 0;
                    background: none;
                    font: inherit;
                }

                .interrupteur .interieur {
                    margin: 0;
                    height: 100%;
                    padding: 0 12px 0 6px;
                    color: rgba(255, 255, 255, 0.85);
                }

                .interrupteur:hover .interieur { background: rgba(38, 38, 44, 0.95); }

                .interrupteur:focus-visible {
                    outline: 2px solid #f59e0b;
                    outline-offset: 2px;
                }

                .glissiere {
                    position: relative;
                    width: 26px;
                    height: 14px;
                    border-radius: 999px;
                    background: rgba(255, 255, 255, 0.25);
                    transition: background 0.2s ease;
                }

                .glissiere::after {
                    content: "";
                    position: absolute;
                    top: 2px;
                    left: 2px;
                    width: 10px;
                    height: 10px;
                    border-radius: 50%;
                    background: #ffffff;
                    transition: transform 0.2s ease;
                }

                .interrupteur[aria-checked="true"] .glissiere { background: #b91c3c; }
                .interrupteur[aria-checked="true"] .glissiere::after { transform: translateX(12px); }

                @keyframes tourner {
                    to { transform: rotate(360deg); }
                }

                @keyframes dessiner {
                    0%   { stroke-dashoffset: 100; }
                    50%  { stroke-dashoffset: 0; }
                    100% { stroke-dashoffset: -100; }
                }

                @media (prefers-reduced-motion: reduce) {
                    .contour, path { animation: none; }
                    path { stroke-dasharray: none; }
                    .barre, .glissiere, .glissiere::after { transition: none; }
                }
            </style>
            <div class="barre">
                <div class="pastille badge" role="status">
                    <div class="contour"></div>
                    <div class="interieur">
                        <svg viewBox="2.5 3 27 23.5" aria-hidden="true">
                            <defs>
                                <linearGradient id="degrade" x1="0" y1="0" x2="1" y2="1">
                                    <stop offset="0" stop-color="#fb7185"/>
                                    <stop offset="1" stop-color="#f59e0b"/>
                                </linearGradient>
                            </defs>
                            <path pathLength="100"
                                  d="M4 25 C 11 25, 19 18, 19 11 C 19 6.5, 16 4.5, 13.5 4.5 C 10.5 4.5, 9 7, 9 10 C 9 17, 18 25, 28 25"/>
                        </svg>
                        <span class="titre">Mode auto</span>
                        <span class="separateur"></span>
                        <span class="detail">AIM2K26 améliore et envoie vos prompts</span>
                    </div>
                </div>
                <button type="button" class="pastille interrupteur" role="switch">
                    <span class="interieur">
                        <span class="glissiere" aria-hidden="true"></span>
                        <span class="etat"></span>
                    </span>
                </button>
            </div>
        `;
        barre = racine.querySelector(".barre");
        badge = racine.querySelector(".badge");
        interrupteur = racine.querySelector(".interrupteur");
        interrupteur.addEventListener("click", basculerExtension);
        document.documentElement.appendChild(hote);
    }

    async function basculerExtension() {
        const { parametres = {} } = await chrome.storage.local.get("parametres");
        await chrome.storage.local.set({ parametres: { ...parametres, extensionActive: !extensionActive } });
    }

    function mettreAJourContenu() {
        badge.hidden = !(modeAuto && extensionActive);
        interrupteur.setAttribute("aria-checked", String(extensionActive));
        interrupteur.querySelector(".etat").textContent = extensionActive
            ? "AIM2K26 activé"
            : "AIM2K26 désactivé";
    }

    function estBlocVisible(element) {
        const style = getComputedStyle(element);
        const fondVisible = style.backgroundColor !== "rgba(0, 0, 0, 0)" && style.backgroundColor !== "transparent";
        return fondVisible || parseFloat(style.borderRadius) >= 12 || parseFloat(style.borderBottomWidth) > 0;
    }

    // La zone de saisie n'est souvent qu'une partie du bloc de saisie (a cote du "+", du
    // choix de modele, du micro...). On remonte jusqu'au bloc visible qui l'entoure.
    function trouverCompositeur(zone) {
        let meilleur = zone;
        let element = zone.parentElement;
        for (let niveau = 0; niveau < 8 && element && element !== document.body; niveau++) {
            const rect = element.getBoundingClientRect();
            // Trop haut : on est sorti du bloc de saisie (conversation entiere, page)
            if (rect.height > 400) break;
            if (estBlocVisible(element)) meilleur = element;
            element = element.parentElement;
        }
        return meilleur;
    }

    function positionner() {
        const zone = globalThis.AIM2K26_ZONE && globalThis.AIM2K26_ZONE.trouver();
        if (!zone) {
            if (barre) barre.classList.remove("visible");
            return;
        }
        if (!hote) creer();
        mettreAJourContenu();

        const rect = trouverCompositeur(zone).getBoundingClientRect();
        barre.style.maxWidth = `${Math.max(160, rect.width - 40)}px`;
        const largeur = barre.offsetWidth;
        const hauteur = barre.offsetHeight;
        barre.style.left = `${rect.left + (rect.width - largeur) / 2}px`;

        // Sous le bloc de saisie, ou au-dessus s'il est colle au bas de l'ecran
        const dessous = rect.bottom + ECART;
        const place = dessous + hauteur <= window.innerHeight - 4;
        barre.style.top = `${place ? dessous : Math.max(4, rect.top - ECART - hauteur)}px`;
        barre.classList.add("visible");
    }

    function appliquerParametres(parametres = {}) {
        modeAuto = Boolean(parametres.autoCorrection);
        // Active par defaut : absent du stockage tant que l'utilisateur n'a rien change
        extensionActive = parametres.extensionActive !== false;
        positionner();
    }

    chrome.storage.local.get("parametres").then(({ parametres }) => appliquerParametres(parametres));
    chrome.storage.onChanged.addListener((changements, zoneStockage) => {
        if (zoneStockage === "local" && changements.parametres) {
            appliquerParametres(changements.parametres.newValue);
        }
    });

    // Le bloc bouge (redimensionnement, zone qui grandit en tapant, changement de conversation)
    window.addEventListener("resize", positionner);
    window.addEventListener("scroll", positionner, true);
    setInterval(positionner, 400);
})();
