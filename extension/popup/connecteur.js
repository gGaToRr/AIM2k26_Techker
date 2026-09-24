// Acces au connecteur local (Native Messaging), partage par les pages du popup.
// Une extension ne peut pas lancer de commande : elle passe par ce connecteur,
// installe par connecteur/installer.sh ou installer.bat.

const CONNECTEUR = "com.aim2k26.connecteur";

function envoyerAuConnecteur(message) {
    return new Promise((resoudre, rejeter) => {
        chrome.runtime.sendNativeMessage(CONNECTEUR, message, (reponse) => {
            if (chrome.runtime.lastError) {
                rejeter(new Error(chrome.runtime.lastError.message));
            } else if (reponse && reponse.erreur) {
                rejeter(new Error(reponse.erreur));
            } else {
                resoudre(reponse);
            }
        });
    });
}

function estConnecteurAbsent(erreur) {
    return /not found|introuvable|forbidden|exited/i.test(erreur.message);
}

// Lignes courtes : elles doivent tenir dans les zones etroites du popup
function lignesConnecteurAbsent(erreur) {
    return [
        "✘ Le connecteur local n'est pas",
        "  installé.",
        "",
        "Installez-le une fois, depuis le",
        "dossier du projet :",
        "",
        "• Linux / macOS :",
        "  ./connecteur/installer.sh",
        "• Windows : double-cliquez sur",
        "  connecteur\\installer.bat",
        "",
        "Puis rechargez l'extension.",
        "",
        `Détail : ${erreur.message}`
    ];
}
