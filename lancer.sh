#!/bin/bash
# ==============================================================================
# Compile et lance Prompting Tool (AIM2k26_Techker)
#
#   ./lancer.sh                      Mode interactif (menu)
#   ./lancer.sh -i "mon prompt" -e   Tous les arguments sont transmis a Main
#   ./lancer.sh --reset              Remise a zero (comme un clone neuf), puis lancement
# ==============================================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

CLASSPATH_RUN="bin:src/lib/*"

remettre_a_zero() {
    echo "🧹 Remise a zero du projet..."

    # Les modeles pesent plus d'1 Go chacun : on confirme avant de les supprimer
    if [ -d models ] && [ -n "$(ls -A models 2>/dev/null)" ]; then
        echo "   Modeles installes ($(du -sh models | cut -f1)) :"
        ls -1 models | sed 's/^/     - /'
        read -r -p "   Supprimer aussi les modeles ? Il faudra les retelecharger. [o/N] " reponse < /dev/tty
        if [[ "$reponse" =~ ^([oO]|[oO][uU][iI]|[yY])$ ]]; then
            rm -rf models
            echo "   [+] Modeles supprimes."
        else
            echo "   [-] Modeles conserves."
        fi
    fi

    rm -rf bin target .llm_config
    echo "   [+] Compilation (bin/, target/) et configuration (.llm_config/) supprimees."
    echo ""
}

if [ "$1" = "--reset" ]; then
    shift
    remettre_a_zero
fi

if ! command -v javac >/dev/null 2>&1; then
    echo "[!] javac introuvable : installez un JDK 21+ (ex: sudo apt install openjdk-21-jdk)." >&2
    exit 1
fi

echo "🔨 Compilation..."
mkdir -p bin
javac -sourcepath ".:src" -cp ".:src/lib/*" -d bin \
    Main.java \
    src/menu/*.java \
    src/cli/*.java \
    src/nlp/*.java \
    src/util/*.java \
    src/gen/*.java \
    src/llm/*.java
echo "   [+] OK"
echo ""

exec java -cp "$CLASSPATH_RUN" Main "$@"
