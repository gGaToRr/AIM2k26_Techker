#!/bin/sh
# ==============================================================================
# Installe le connecteur local AIM2K26 pour Chrome / Chromium (Linux et macOS)
#
#   ./connecteur/installer.sh               installe
#   ./connecteur/installer.sh --desinstaller retire la declaration
# ==============================================================================

set -e

NOM="com.aim2k26.connecteur"
# Identifiant fixe de l'extension, derive de la cle "key" de extension/manifest.json
ID_EXTENSION="hgikcaghkempfkljikcopdfncnhjkanl"

DIR="$(cd "$(dirname "$0")" && pwd)"
HOTE="$DIR/hote.sh"

case "$(uname -s)" in
    Darwin)
        DOSSIERS="$HOME/Library/Application Support/Google/Chrome/NativeMessagingHosts
$HOME/Library/Application Support/Chromium/NativeMessagingHosts"
        ;;
    *)
        DOSSIERS="$HOME/.config/google-chrome/NativeMessagingHosts
$HOME/.config/chromium/NativeMessagingHosts"
        ;;
esac

if [ "$1" = "--desinstaller" ]; then
    printf '%s\n' "$DOSSIERS" | while IFS= read -r dossier; do
        rm -f "$dossier/$NOM.json"
    done
    echo "[+] Connecteur AIM2K26 retire."
    exit 0
fi

chmod +x "$HOTE"

# Declaration pour chaque navigateur present (dossier de profil existant)
printf '%s\n' "$DOSSIERS" | while IFS= read -r dossier; do
    [ -d "$(dirname "$dossier")" ] || continue
    mkdir -p "$dossier"
    cat > "$dossier/$NOM.json" <<EOF
{
  "name": "$NOM",
  "description": "Connecteur local AIM2K26",
  "path": "$HOTE",
  "type": "stdio",
  "allowed_origins": ["chrome-extension://$ID_EXTENSION/"]
}
EOF
    echo "[+] Declare dans : $dossier"
done

echo "[+] Connecteur installe. Rechargez l'extension dans chrome://extensions."
