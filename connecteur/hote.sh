#!/bin/sh
# ==============================================================================
# Connecteur local AIM2K26 (Linux / macOS) - hote Native Messaging de Chrome
#
# Chrome interdit a une extension de lancer une commande : il demarre ce script
# et lui parle sur stdin/stdout. Chaque message = 4 octets de longueur (ordre
# natif, little-endian sur x86 et Apple Silicon) suivis d'un JSON UTF-8.
#
# Volontairement en sh et non en Java : il sert justement a verifier Java.
# ==============================================================================

# Longueurs en octets et non en caracteres, meme avec des accents.
# (Java en deduirait une sortie ASCII : chaque appel force donc -Dstdout.encoding=UTF-8)
export LC_ALL=C

# Chrome lance le connecteur avec un PATH minimal : on complete avec les
# emplacements courants de Java (JAVA_HOME, SDKMAN, Homebrew)
# (JAVA_HOME vide donnerait "/bin" : il n'est pris que s'il est defini)
for dossier in ${JAVA_HOME:+"$JAVA_HOME/bin"} "$HOME/.sdkman/candidates/java/current/bin" \
               /opt/homebrew/opt/openjdk/bin /usr/local/opt/openjdk/bin; do
    [ -d "$dossier" ] && PATH="$PATH:$dossier"
done
export PATH

lire_message() {
    longueur=$(head -c 4 | od -An -tu4 | tr -d ' ')
    [ -n "$longueur" ] || return 1
    head -c "$longueur"
}

# Echappe un texte pour l'inserer dans une chaine JSON
echapper_json() {
    printf '%s' "$1" \
        | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/	/\\t/g' -e 's/\r//g' \
        | awk 'NR > 1 { printf "\\n" } { printf "%s", $0 }'
}

ecrire_message() {
    longueur=${#1}
    octet() { printf "\\$(printf '%03o' "$1")"; }
    octet $((longueur & 255))
    octet $(((longueur >> 8) & 255))
    octet $(((longueur >> 16) & 255))
    octet $(((longueur >> 24) & 255))
    printf '%s' "$1"
}

# Lance "<commande> -version" : trouve seulement si la commande existe ET reussit
# (sur macOS, /usr/bin/java existe meme sans JDK et echoue avec un message)
tester_commande() {
    if ! command -v "$1" >/dev/null 2>&1; then
        printf '{"trouve":false,"sortie":"commande introuvable"}'
        return
    fi
    sortie=$("$1" -version 2>&1)
    if [ $? -eq 0 ]; then trouve=true; else trouve=false; fi
    printf '{"trouve":%s,"sortie":"%s"}' "$trouve" "$(echapper_json "$sortie")"
}

# Le connecteur vit dans <projet>/connecteur : l'outil Java est juste au-dessus
RACINE="$(cd "$(dirname "$0")/.." && pwd)"
# Une fois pour toutes : un cd fait dans $(...) serait perdu a la sortie du sous-shell
cd "$RACINE" || exit 1

# Compile l'outil Java s'il ne l'a jamais ete. En cas d'echec, ecrit un
# {"ok":false,"sortie":...} sur stdout et renvoie 1.
preparer_outil() {
    [ -f bin/Main.class ] && return 0
    if ! command -v javac >/dev/null 2>&1; then
        printf '{"ok":false,"sortie":"Outil non compile et javac introuvable : lancez d abord le test Java."}'
        return 1
    fi
    mkdir -p bin
    compilation=$(javac -sourcepath ".:src" -cp ".:src/lib/*" -d bin Main.java src/menu/*.java src/cli/*.java \
        src/nlp/*.java src/util/*.java src/gen/*.java src/llm/*.java 2>&1)
    if [ $? -ne 0 ]; then
        printf '{"ok":false,"sortie":"%s"}' "$(echapper_json "Echec de compilation : $compilation")"
        return 1
    fi
}

# Lance l'outil Java ; sa sortie JSON est renvoyee telle quelle sous "donnees"
outil_json() {
    preparer_outil || return
    sortie=$(java -Dstdout.encoding=UTF-8 -cp "bin:src/lib/*" Main "$@" -o json 2>&1)
    case "$sortie" in
        "{"*) printf '{"ok":true,"donnees":%s}' "$sortie" ;;
        *)    printf '{"ok":false,"sortie":"%s"}' "$(echapper_json "$sortie")" ;;
    esac
}

# Delegue a l'outil Java (--models-check) : integrite SHA-256 et test de generation
tester_modeles() {
    preparer_outil || return
    # -o json : l'outil renvoie un resultat structure, l'extension se charge de la mise en forme
    sortie=$(java -Dstdout.encoding=UTF-8 -cp "bin:src/lib/*" Main --models-check -o json 2>&1)
    case "$sortie" in
        "{"*) printf '{"ok":true,"verification":%s}' "$sortie" ;;
        *)    printf '{"ok":false,"sortie":"%s"}' "$(echapper_json "$sortie")" ;;
    esac
}

message=$(lire_message)
champ() {
    printf '%s' "$message" | sed -n "s/.*\"$1\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\\1/p"
}
action=$(champ action)
# Identifiant de modele : lettres, chiffres et tirets seulement (il finit en argument de commande)
modele=$(champ modele | tr -cd 'a-z0-9-')

case "$action" in
    ameliorer)
        # Le message entier passe a Java sur stdin : le prompt peut contenir n'importe quel
        # caractere, que sed ne saurait pas extraire proprement du JSON
        if ! erreur=$(preparer_outil); then
            ecrire_message "{\"action\":\"ameliorer\",\"resultat\":$erreur}"
            exit 0
        fi
        reponse=$(printf '%s' "$message" | java -Dstdout.encoding=UTF-8 -cp "bin:src/lib/*" Main --improve-json 2>/dev/null)
        case "$reponse" in
            "{"*) ecrire_message "{\"action\":\"ameliorer\",\"resultat\":$reponse}" ;;
            *)    ecrire_message "{\"action\":\"ameliorer\",\"resultat\":{\"ok\":false,\"message\":\"reponse illisible de l'outil\"}}" ;;
        esac
        ;;
    test-modeles)
        ecrire_message "{\"action\":\"test-modeles\",\"resultat\":$(tester_modeles)}"
        ;;
    liste-modeles)
        ecrire_message "{\"action\":\"liste-modeles\",\"resultat\":$(outil_json --models-list)}"
        ;;
    supprimer-modele)
        ecrire_message "{\"action\":\"supprimer-modele\",\"resultat\":$(outil_json --models-delete "$modele" --yes)}"
        ;;
    telecharger-modele)
        # Plusieurs messages : un par ligne JSON de l'outil (progression), puis la fin.
        # Appele via connectNative : Chrome garde le connecteur en vie jusqu'a la fin.
        if ! erreur=$(preparer_outil); then
            ecrire_message "{\"type\":\"fin\",\"ok\":false,\"message\":\"$(echapper_json "$erreur")\"}"
            exit 0
        fi
        # "moteur" : le moteur llama.cpp ; "base" : la base de prompts ; telecharges comme les modeles
        if [ "$modele" = "moteur" ]; then
            set -- --runtime-install
        elif [ "$modele" = "base" ]; then
            set -- --corpus-install
        else
            set -- --models-install "$modele"
        fi
        java -Dstdout.encoding=UTF-8 -cp "bin:src/lib/*" Main "$@" -o json 2>/dev/null | while IFS= read -r ligne_json; do
            case "$ligne_json" in "{"*) ecrire_message "$ligne_json" ;; esac
        done
        ;;
    test-java)
        ecrire_message "{\"action\":\"test-java\",\"java\":$(tester_commande java),\"javac\":$(tester_commande javac)}"
        ;;
    *)
        ecrire_message "{\"erreur\":\"action inconnue : $(echapper_json "$action")\"}"
        ;;
esac
