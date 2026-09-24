// Page principale du popup : branchement des boutons de test.
// L'acces au connecteur local est dans connecteur.js, charge avant ce fichier.

const resultats = document.getElementById("resultats");

function afficher(lignes) {
    resultats.value = Array.isArray(lignes) ? lignes.join("\n") : lignes;
}

// --- Mise en forme ---

// La zone de resultats est etroite (~38 caracteres) et en police a chasse fixe :
// chaque ligne est pensee pour tenir, et les colonnes s'alignent au caractere.
const RETRAIT = " ".repeat(14);

function ligne(symbole, libelle, detail = "") {
    return `${symbole} ${detail ? libelle.padEnd(19) + detail : libelle}`;
}

function champ(libelle, valeur) {
    return `  ${libelle.padEnd(12)}${valeur}`;
}

function accorder(nombre, mot) {
    return `${mot}${nombre > 1 ? "s" : ""}`;
}

function formaterOctets(octets) {
    return `${(octets / 1024 ** 3).toFixed(2).replace(".", ",")} Go`;
}

function formaterSecondes(secondes) {
    return `${secondes.toFixed(1).replace(".", ",")} s`;
}

// Les messages de l'outil Java sont sans accents : on les retablit pour l'affichage
function accentuer(texte) {
    return texte
        .replace(/reponse/g, "réponse")
        .replace(/endommage\b/g, "endommagé");
}

// --- Test de Java ---

function extraireVersion(etat, motif) {
    const trouve = etat.sortie.match(motif);
    return trouve ? trouve[1] : "présent";
}

async function testerJava() {
    afficher("Test de Java en cours…");
    try {
        const { java, javac } = await envoyerAuConnecteur({ action: "test-java" });

        const lignes = ["Java", ""];
        lignes.push(ligne(java.trouve ? "✔" : "✘", "Java (exécution)",
            java.trouve ? extraireVersion(java, /version "([^"]+)"/) : "introuvable"));
        lignes.push(ligne(javac.trouve ? "✔" : "✘", "JDK (compilation)",
            javac.trouve ? extraireVersion(javac, /javac (\S+)/) : "introuvable"));
        lignes.push("");

        if (java.trouve && javac.trouve) {
            lignes.push("→ Tout est prêt : l'outil peut être", "  compilé et lancé.");
        } else if (java.trouve) {
            lignes.push("→ Le JDK manque pour compiler",
                "  l'outil : installez un JDK 21+",
                "  (ex. Temurin sur adoptium.net).");
        } else {
            lignes.push("→ Java n'est pas installé :",
                "  installez un JDK 21+",
                "  (ex. Temurin sur adoptium.net).");
        }
        afficher(lignes);
    } catch (erreur) {
        afficherErreur(erreur);
    }
}

// --- Test des modeles installes ---

function decrireFichier(modele) {
    const taille = formaterOctets(modele.tailleOctets);
    if (modele.fichier === "INTACT") {
        return [`intact · ${taille}`, modele.detailFichier];
    }
    if (modele.fichier === "ILLISIBLE") {
        return [`illisible · ${taille}`];
    }
    return [`endommagé · ${taille}`,
        modele.detailFichier.startsWith("taille") ? "taille incorrecte" : "SHA-256 invalide"];
}

function decrireGeneration(modele) {
    if (modele.generation === "OK") {
        return [`réussie en ${formaterSecondes(modele.secondes)}`];
    }
    const etat = modele.generation === "ECHEC" ? "échec" : "non testée";
    return [etat, accentuer(modele.detailGeneration)];
}

function decrireModeleInstalle(modele) {
    const intact = modele.fichier === "INTACT";
    const symbole = intact && modele.generation === "OK" ? "✔" : intact ? "⚠" : "✘";

    // Une valeur trop longue passe sur la ligne suivante, alignee sur la colonne des valeurs
    const bloc = (libelle, [premiere, ...suite]) =>
        [champ(libelle, premiere), ...suite.filter(Boolean).map((l) => RETRAIT + l)];

    return [
        ligne(symbole, modele.nom),
        ...bloc("Fichier", decrireFichier(modele)),
        ...bloc("Génération", decrireGeneration(modele))
    ];
}

async function testerModeles() {
    afficher(["Vérification des modèles…", "",
        "Contrôle d'intégrité puis test",
        "de génération : quelques secondes",
        "par modèle."]);
    try {
        const { resultat } = await envoyerAuConnecteur({ action: "test-modeles" });
        if (!resultat.ok) {
            afficher(["✘ L'outil n'a pas pu vérifier", "  les modèles.", "", resultat.sortie.trim()]);
            return;
        }

        const { modeles, moteurDisponible } = resultat.verification;
        const installes = modeles.filter((m) => m.fichier !== "ABSENT");
        const absents = modeles.filter((m) => m.fichier === "ABSENT");
        const fonctionnels = installes.filter((m) => m.fichier === "INTACT" && m.generation === "OK");

        const lignes = [`Modèles installés : ${installes.length} sur ${modeles.length}`, ""];

        if (installes.length === 0) {
            lignes.push("→ Aucun modèle installé.", "  Utilisez « Télécharger les",
                "  modèles » pour en ajouter un.");
            afficher(lignes);
            return;
        }
        if (!moteurDisponible) {
            lignes.push("⚠ Moteur llama.cpp introuvable :", "  la génération n'est pas testée.", "");
        }

        installes.forEach((modele) => lignes.push(...decrireModeleInstalle(modele), ""));

        if (absents.length > 0) {
            lignes.push(`Non ${accorder(absents.length, "installé")} :`);
            absents.forEach((modele) => lignes.push(ligne("○", modele.nom)));
            lignes.push("");
        }

        const n = fonctionnels.length;
        lignes.push(`→ ${n} ${accorder(n, "modèle")} ${accorder(n, "fonctionnel")}`,
            `  sur ${installes.length} ${accorder(installes.length, "installé")}.`);
        afficher(lignes);
    } catch (erreur) {
        afficherErreur(erreur);
    }
}

// --- Erreurs ---

function afficherErreur(erreur) {
    afficher(estConnecteurAbsent(erreur)
        ? lignesConnecteurAbsent(erreur)
        : ["✘ Erreur du connecteur local.", "", erreur.message]);
}

// --- Historique des prompts ameliores ---

const listeHistorique = document.getElementById("liste-historique");

function carteHistorique(entree) {
    const element = document.createElement("li");
    const lien = document.createElement("a");
    lien.className = "carte-prompt";
    lien.href = `prompt.html?id=${encodeURIComponent(entree.id)}`;

    const texte = document.createElement("span");
    texte.className = "texte-prompt";
    texte.textContent = entree.original;

    const meta = document.createElement("span");
    meta.className = "meta-prompt";
    meta.textContent = [libelleType(entree.type), entree.source === "nlp" ? "NLP" : null,
        formaterDateRelative(entree.date)].filter(Boolean).join(" · ");

    lien.append(texte, meta);
    element.append(lien);
    return element;
}

async function afficherHistorique() {
    const historique = await lireHistorique();
    if (historique.length === 0) {
        const vide = document.createElement("li");
        vide.className = "message-liste";
        vide.textContent = "Aucun prompt amélioré pour l'instant.";
        listeHistorique.replaceChildren(vide);
        return;
    }
    // Pas de defilement dans l'extension : seules les cartes qui tiennent sont visibles
    listeHistorique.replaceChildren(...historique.slice(0, 8).map(carteHistorique));
}

// En direct : un prompt ameliore pendant que le popup est ouvert apparait aussitot
chrome.storage.onChanged.addListener((changements, zone) => {
    if (zone === "local" && changements.historique) afficherHistorique();
});

afficherHistorique();

document.getElementById("test-modeles").addEventListener("click", testerModeles);
document.getElementById("test-java").addEventListener("click", testerJava);
