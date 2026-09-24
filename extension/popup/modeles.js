// Page "Modeles open source" : liste reelle des modeles, telechargement et suppression.
// La liste vient du registre de l'outil Java (via connecteur.js). Les telechargements
// sont portes par le service worker (background.js) : ils continuent popup ferme.

const liste = document.getElementById("liste-modeles");
const gabarit = document.getElementById("gabarit-modele");

// id -> elements de la ligne, pour les mises a jour de progression sans tout redessiner
const lignes = new Map();
// id -> dernier message d'echec ; a part, car la liste est reconstruite apres chaque action
const erreurs = new Map();

function formaterGo(octets) {
    return `${(octets / 1024 ** 3).toFixed(2).replace(".", ",")} Go`;
}

// "~1.1 Go (Quantifie Q4_K_M)" -> "~1,1 Go"
function tailleAnnoncee(modele) {
    return modele.tailleDisque.replace(/\s*\(.*\)$/, "").replace(".", ",");
}

function afficherMessage(lignesTexte) {
    liste.replaceChildren();
    const element = document.createElement("li");
    element.className = "message-liste";
    element.textContent = lignesTexte.join("\n");
    liste.append(element);
}

// --- Rendu ---

function mettreAJourLigne(modele, telechargement) {
    const ligne = lignes.get(modele.id);
    const { boutonTelecharger, boutonSupprimer, details, barre, remplissage } = ligne;

    details.className = "details-modele";
    barre.hidden = true;
    boutonTelecharger.disabled = modele.installe || Boolean(telechargement);
    boutonSupprimer.disabled = !modele.installe || Boolean(telechargement);

    if (telechargement) {
        const pourcentage = telechargement.total > 0
            ? Math.floor((telechargement.lus * 100) / telechargement.total) : 0;
        barre.hidden = false;
        remplissage.style.width = `${pourcentage}%`;
        details.textContent = pourcentage >= 100
            ? "Vérification de l'intégrité…"
            : `Téléchargement : ${pourcentage} % · ${telechargement.vitesse.toFixed(1).replace(".", ",")} Mo/s`;
    } else if (erreurs.has(modele.id)) {
        details.classList.add("erreur");
        details.textContent = `Échec : ${erreurs.get(modele.id)}`;
    } else if (modele.installe) {
        details.classList.add("installe");
        details.textContent = `Installé · ${formaterGo(modele.tailleOctets)}`;
    } else {
        details.textContent = `${tailleAnnoncee(modele)} · ${modele.specialite}`;
    }
}

function construireLigne(modele) {
    const element = gabarit.content.firstElementChild.cloneNode(true);
    const ligne = {
        modele,
        boutonTelecharger: element.querySelector(".telecharger"),
        boutonSupprimer: element.querySelector(".supprimer"),
        details: element.querySelector(".details-modele"),
        barre: element.querySelector(".barre-progression"),
        remplissage: element.querySelector(".remplissage")
    };
    element.querySelector(".nom-modele").textContent = modele.nom;
    ligne.boutonTelecharger.setAttribute("aria-label", `Télécharger ${modele.nom}`);
    ligne.boutonSupprimer.setAttribute("aria-label", `Supprimer ${modele.nom}`);
    ligne.boutonTelecharger.addEventListener("click", () => telecharger(modele.id));
    ligne.boutonSupprimer.addEventListener("click", () => supprimer(modele.id));
    lignes.set(modele.id, ligne);
    return element;
}

async function chargerListe() {
    try {
        const [{ resultat }, telechargements] = await Promise.all([
            envoyerAuConnecteur({ action: "liste-modeles" }),
            chrome.runtime.sendMessage({ type: "etat-telechargements" })
        ]);
        if (!resultat.ok) {
            afficherMessage(["✘ L'outil n'a pas pu lister", "les modèles.", "", resultat.sortie.trim()]);
            return;
        }
        lignes.clear();
        liste.replaceChildren(...resultat.donnees.modeles.map(construireLigne));
        for (const { modele } of lignes.values()) {
            mettreAJourLigne(modele, telechargements[modele.id]);
        }
    } catch (erreur) {
        afficherMessage(estConnecteurAbsent(erreur)
            ? lignesConnecteurAbsent(erreur)
            : ["✘ Erreur du connecteur local.", "", erreur.message]);
    }
}

// --- Actions ---

function telecharger(id) {
    erreurs.delete(id);
    mettreAJourLigne(lignes.get(id).modele, { lus: 0, total: 0, vitesse: 0 });
    // Le nom sert a la notification de fin, si le popup a ete ferme entre-temps
    chrome.runtime.sendMessage({ type: "telecharger", modele: id, nom: lignes.get(id).modele.nom });
}

async function supprimer(id) {
    const { modele } = lignes.get(id);
    if (!confirm(`Supprimer ${modele.nom} (${formaterGo(modele.tailleOctets)}) ?\n\n`
        + "Il faudra le retélécharger pour l'utiliser à nouveau.")) {
        return;
    }
    try {
        const { resultat } = await envoyerAuConnecteur({ action: "supprimer-modele", modele: id });
        if (resultat.ok) {
            erreurs.delete(id);
        } else {
            erreurs.set(id, "suppression impossible");
        }
    } catch (erreur) {
        erreurs.set(id, erreur.message);
    }
    await chargerListe();
}

// Progression diffusee par le service worker, meme si le telechargement a ete lance
// lors d'une ouverture precedente du popup
chrome.runtime.onMessage.addListener((message) => {
    const ligne = lignes.get(message.modele);
    if (!ligne) return;

    if (message.type === "progression") {
        mettreAJourLigne(ligne.modele, message);
    } else if (message.type === "fin") {
        if (message.ok) {
            erreurs.delete(message.modele);
        } else {
            erreurs.set(message.modele, message.message || "échec du téléchargement");
        }
        chargerListe();
    }
});

chargerListe();
