// Pages de la popup affichees dans le panneau injecte (contenu/panneau.js) et non dans la
// popup de Chrome : fond transparent autour de la carte, pour que ses coins arrondis
// laissent voir le site. Charge dans <head>, avant le premier rendu.
if (window.top !== window) {
    document.documentElement.classList.add("panneau");
    // Echap dans le panneau : le content script, seul a pouvoir le retirer, le ferme
    window.addEventListener("keydown", (evenement) => {
        if (evenement.key === "Escape") window.parent.postMessage({ aim2k26: "fermer" }, "*");
    });
}
