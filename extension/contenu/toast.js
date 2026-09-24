// Toast affiche par-dessus la page du site, colle au bord droit de l'ecran.
//
// Charge avant detection.js (manifest.json > content_scripts), meme monde isole.
// Rendu dans un Shadow DOM : le CSS du site ne l'abime pas, et le sien ne fuit pas.

// dureeMs : duree d'affichage, ou null pour un toast qui reste jusqu'a fermer().
// variante : "info" (blanc) ou "avertissement" (orange-jaune).
// Renvoie { changer(texte, dureeMs, variante), fermer() } pour faire evoluer le meme toast
// (ex. "Amelioration en cours..." puis "Prompt ameliore").
globalThis.afficherToastAim2k26 = function (texte, dureeMs = 2500, variante = "info") {
    const DUREE_FONDU_MS = 250;

    const hote = document.createElement("div");
    hote.id = "aim2k26-toast";
    const racine = hote.attachShadow({ mode: "closed" });

    racine.innerHTML = `
        <style>
            :host { all: initial; }

            /* Colle au bord droit : seuls les coins gauches sont arrondis, pas de bordure a droite */
            .toast {
                position: fixed;
                top: 38vh;
                right: 0;
                z-index: 2147483647;
                display: flex;
                align-items: center;
                gap: 10px;
                max-width: 340px;
                padding: 12px 18px;
                font: 600 14px/1.4 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
                color: #111827;
                background: #ffffff;
                border: 1px solid #e2e5ea;
                border-right: none;
                border-radius: 12px 0 0 12px;
                box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
                cursor: pointer;
                opacity: 0;
                transform: translateX(100%);
                transition: opacity ${DUREE_FONDU_MS}ms ease, transform ${DUREE_FONDU_MS}ms ease;
            }

            .toast.visible {
                opacity: 1;
                transform: translateX(0);
            }

            svg {
                width: 18px;
                height: 18px;
                flex-shrink: 0;
            }

            /* Avertissement : resultat utilisable mais moins fiable */
            .toast.avertissement {
                color: #78350f;
                background: #fef3c7;
                border-color: #f59e0b;
            }

            .toast.avertissement path {
                stroke: #b45309;
            }

            @media (prefers-reduced-motion: reduce) {
                .toast { transition: none; }
            }
        </style>
        <div class="toast" role="status" aria-live="polite">
            <svg viewBox="0 0 32 32" aria-hidden="true">
                <path d="M4 25 C 11 25, 19 18, 19 11 C 19 6.5, 16 4.5, 13.5 4.5 C 10.5 4.5, 9 7, 9 10 C 9 17, 18 25, 28 25"
                      fill="none" stroke="#7b1e2b" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span></span>
        </div>
    `;

    const toast = racine.querySelector(".toast");
    const libelle = racine.querySelector("span");
    let minuterie = null;

    const fermer = () => {
        clearTimeout(minuterie);
        toast.classList.remove("visible");
        setTimeout(() => hote.remove(), DUREE_FONDU_MS);
    };

    const changer = (nouveauTexte, nouvelleDuree = 2500, nouvelleVariante = "info") => {
        // textContent et non innerHTML : le texte n'est jamais interprete comme du HTML
        libelle.textContent = nouveauTexte;
        toast.classList.toggle("avertissement", nouvelleVariante === "avertissement");
        clearTimeout(minuterie);
        if (nouvelleDuree !== null) minuterie = setTimeout(fermer, nouvelleDuree);
    };
    // Un clic le ferme tout de suite
    toast.addEventListener("click", fermer, { once: true });

    document.getElementById("aim2k26-toast")?.remove();
    document.documentElement.appendChild(hote);

    // Forcer le calcul du style initial avant d'ajouter la classe : sans cela, le
    // navigateur fusionne les deux etats et il n'y a pas d'animation d'entree.
    // (pas de requestAnimationFrame : suspendu dans les onglets en arriere-plan)
    toast.getBoundingClientRect();
    toast.classList.add("visible");
    changer(texte, dureeMs, variante);

    return { changer, fermer };
};
