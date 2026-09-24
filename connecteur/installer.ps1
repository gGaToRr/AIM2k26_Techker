# ==============================================================================
# Installe le connecteur local AIM2K26 pour Chrome (Windows)
#
#   installer.bat                  installe
#   installer.bat -Desinstaller    retire la declaration
# ==============================================================================

param([switch]$Desinstaller)

$ErrorActionPreference = 'Stop'

$Nom = 'com.aim2k26.connecteur'
# Identifiant fixe de l'extension, derive de la cle "key" de extension/manifest.json
$IdExtension = 'hgikcaghkempfkljikcopdfncnhjkanl'

$Dossier = $PSScriptRoot
$Manifeste = Join-Path $Dossier "$Nom.json"
# Sous Windows, Chrome trouve le connecteur via le registre de l'utilisateur
$Cles = @(
    "HKCU:\Software\Google\Chrome\NativeMessagingHosts\$Nom",
    "HKCU:\Software\Chromium\NativeMessagingHosts\$Nom"
)

if ($Desinstaller) {
    foreach ($cle in $Cles) { Remove-Item -Path $cle -Force -ErrorAction SilentlyContinue }
    Remove-Item -Path $Manifeste -Force -ErrorAction SilentlyContinue
    Write-Host '[+] Connecteur AIM2K26 retire.'
    exit 0
}

@{
    name            = $Nom
    description     = 'Connecteur local AIM2K26'
    path            = (Join-Path $Dossier 'hote.bat')
    type            = 'stdio'
    allowed_origins = @("chrome-extension://$IdExtension/")
} | ConvertTo-Json | Set-Content -Path $Manifeste -Encoding UTF8

foreach ($cle in $Cles) {
    New-Item -Path $cle -Force | Out-Null
    Set-ItemProperty -Path $cle -Name '(Default)' -Value $Manifeste
}

Write-Host '[+] Connecteur installe. Rechargez l''extension dans chrome://extensions.'
