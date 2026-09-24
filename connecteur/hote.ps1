# ==============================================================================
# Connecteur local AIM2K26 (Windows) - hote Native Messaging de Chrome
#
# Meme protocole que hote.sh : 4 octets de longueur (little-endian) + JSON UTF-8
# sur stdin/stdout. PowerShell est present sur tous les Windows 10 et 11.
# ==============================================================================

$ErrorActionPreference = 'Stop'
# La sortie de Java (forcee en UTF-8) doit etre relue en UTF-8, pas dans la page de code de la console
[Console]::OutputEncoding = [Text.Encoding]::UTF8
# Et ce que PowerShell envoie a Java par un pipe doit partir en UTF-8 (sans BOM)
$OutputEncoding = New-Object Text.UTF8Encoding $false

function Lire-Message {
    $entree = [Console]::OpenStandardInput()
    $tete = New-Object byte[] 4
    if ($entree.Read($tete, 0, 4) -lt 4) { return $null }
    $longueur = [BitConverter]::ToInt32($tete, 0)
    $corps = New-Object byte[] $longueur
    $lus = 0
    while ($lus -lt $longueur) {
        $n = $entree.Read($corps, $lus, $longueur - $lus)
        if ($n -le 0) { break }
        $lus += $n
    }
    return [Text.Encoding]::UTF8.GetString($corps, 0, $lus) | ConvertFrom-Json
}

function Ecrire-Message($objet) {
    Ecrire-Json ($objet | ConvertTo-Json -Compress -Depth 8)
}

# Ecrit un JSON deja serialise (lignes de progression de l'outil Java, transmises telles quelles)
function Ecrire-Json([string]$json) {
    $octets = [Text.Encoding]::UTF8.GetBytes($json)
    $sortie = [Console]::OpenStandardOutput()
    $sortie.Write([BitConverter]::GetBytes([int]$octets.Length), 0, 4)
    $sortie.Write($octets, 0, $octets.Length)
    $sortie.Flush()
}

# Trouve seulement si la commande existe ET reussit
function Tester-Commande($nom) {
    if (-not (Get-Command $nom -ErrorAction SilentlyContinue)) {
        return @{ trouve = $false; sortie = 'commande introuvable' }
    }
    # cmd /c : la sortie de "-version" part sur stderr, que PowerShell 5 transforme sinon en erreurs
    $texte = (cmd /c "$nom -version 2>&1") -join "`n"
    return @{ trouve = ($LASTEXITCODE -eq 0); sortie = $texte }
}

# Le connecteur vit dans <projet>\connecteur : l'outil Java est juste au-dessus
$Racine = Split-Path $PSScriptRoot -Parent

Set-Location $Racine
$Java = 'java -Dstdout.encoding=UTF-8 -cp "bin;src/lib/*" Main'

# Compile l'outil Java s'il ne l'a jamais ete. Renvoie $null si tout va bien, sinon l'erreur.
function Preparer-Outil {
    if (Test-Path 'bin\Main.class') { return $null }
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        return @{ ok = $false; sortie = "Outil non compile et javac introuvable : lancez d'abord le test Java." }
    }
    New-Item -ItemType Directory -Force -Path bin | Out-Null
    $compilation = (cmd /c 'javac -sourcepath ".;src" -cp ".;src/lib/*" -d bin Main.java src/menu/*.java src/cli/*.java src/nlp/*.java src/util/*.java src/gen/*.java src/llm/*.java 2>&1') -join "`n"
    if ($LASTEXITCODE -ne 0) {
        return @{ ok = $false; sortie = "Echec de compilation : $compilation" }
    }
    return $null
}

# Lance l'outil Java avec -o json ; sa sortie est renvoyee sous "donnees"
function Outil-Json([string]$arguments) {
    $erreur = Preparer-Outil
    if ($erreur) { return $erreur }
    $texte = (cmd /c "$Java $arguments -o json 2>&1") -join "`n"
    if ($texte.TrimStart().StartsWith('{')) {
        return @{ ok = $true; donnees = ($texte | ConvertFrom-Json) }
    }
    return @{ ok = $false; sortie = $texte }
}

# Delegue a l'outil Java (--models-check) : integrite SHA-256 et test de generation
function Tester-Modeles {
    $resultat = Outil-Json '--models-check'
    if ($resultat.ok) { return @{ ok = $true; verification = $resultat.donnees } }
    return $resultat
}

$message = Lire-Message
# Identifiant de modele : lettres, chiffres et tirets seulement (il finit en argument de commande)
$Modele = ([string]$message.modele) -replace '[^a-z0-9-]', ''

switch ($message.action) {
    'liste-modeles' {
        Ecrire-Message @{ action = 'liste-modeles'; resultat = (Outil-Json '--models-list') }
    }
    'supprimer-modele' {
        Ecrire-Message @{ action = 'supprimer-modele'; resultat = (Outil-Json "--models-delete $Modele --yes") }
    }
    'telecharger-modele' {
        # Plusieurs messages : un par ligne JSON de l'outil (progression), puis la fin
        $erreur = Preparer-Outil
        if ($erreur) {
            Ecrire-Message @{ type = 'fin'; ok = $false; message = $erreur.sortie }
            break
        }
        # "moteur" : le moteur llama.cpp qui execute les modeles, telecharge comme eux
        $Commande = if ($Modele -eq 'moteur') { '--runtime-install' } else { "--models-install $Modele" }
        cmd /c "$Java $Commande -o json 2>nul" | ForEach-Object {
            if ($_.StartsWith('{')) { Ecrire-Json $_ }
        }
    }
    'ameliorer' {
        # Le message entier passe a Java sur stdin : le prompt peut contenir n'importe quel caractere
        $erreur = Preparer-Outil
        if ($erreur) {
            Ecrire-Message @{ action = 'ameliorer'; resultat = @{ ok = $false; message = $erreur.sortie } }
            break
        }
        $reponse = (($message | ConvertTo-Json -Compress) | cmd /c "$Java --improve-json 2>nul") -join ''
        if ($reponse.StartsWith('{')) {
            Ecrire-Message @{ action = 'ameliorer'; resultat = ($reponse | ConvertFrom-Json) }
        } else {
            Ecrire-Message @{ action = 'ameliorer'; resultat = @{ ok = $false; message = "reponse illisible de l'outil" } }
        }
    }
    'test-modeles' {
        Ecrire-Message @{ action = 'test-modeles'; resultat = (Tester-Modeles) }
    }
    'test-java' {
        Ecrire-Message @{ action = 'test-java'; java = (Tester-Commande 'java'); javac = (Tester-Commande 'javac') }
    }
    default {
        Ecrire-Message @{ erreur = "action inconnue : $($message.action)" }
    }
}
