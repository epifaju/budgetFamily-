#Requires -Version 5.1
<#
.SYNOPSIS
    Démarre l'application InvoiceAI complète (PostgreSQL, backend, Metro, app Android).

.PARAMETER BackendOnly
    Démarre uniquement PostgreSQL et le backend Spring Boot.

.PARAMETER MobileOnly
    Démarre uniquement Metro et l'app Android (le backend doit déjà tourner).

.PARAMETER SkipDocker
    Ne démarre pas PostgreSQL via Docker (PostgreSQL déjà lancé localement).

.PARAMETER SkipAndroid
    Démarre PostgreSQL, le backend et Metro sans lancer l'app Android.

.EXAMPLE
    .\start-app.ps1
    .\start-app.ps1 -BackendOnly
    .\start-app.ps1 -MobileOnly
#>

param(
    [switch]$BackendOnly,
    [switch]$MobileOnly,
    [switch]$SkipDocker,
    [switch]$SkipAndroid
)

$ErrorActionPreference = 'Stop'

$RootDir = $PSScriptRoot
$BackendDir = Join-Path $RootDir 'invoice-ai-backend'
$MobileDir = Join-Path $RootDir 'InvoiceAI'

$PostgresPort = 5435
$BackendPort = 8085
$MetroPort = 8081

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "    [OK] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "    [ATTENTION] $Message" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Message)
    Write-Host "    [ERREUR] $Message" -ForegroundColor Red
}

function Test-CommandExists {
    param([string]$Name)
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-External {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $Command 2>&1 | ForEach-Object { Write-Host $_ }
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }

    if ($null -eq $LASTEXITCODE) {
        return 0
    }

    return [int]$LASTEXITCODE
}

function Test-PortOpen {
    param(
        [int]$Port,
        [string]$HostName = '127.0.0.1'
    )

    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        $connected = $async.AsyncWaitHandle.WaitOne(1000, $false)
        if ($connected -and $client.Connected) {
            $client.Close()
            return $true
        }
        $client.Close()
        return $false
    }
    catch {
        return $false
    }
}

function Wait-ForPort {
    param(
        [int]$Port,
        [string]$Label,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortOpen -Port $Port) {
            Write-Ok "$Label disponible sur le port $Port"
            return $true
        }
        Start-Sleep -Seconds 2
        Write-Host "    En attente de $Label (port $Port)..." -ForegroundColor DarkGray
    }

    Write-Err "$Label indisponible après ${TimeoutSeconds}s (port $Port)"
    return $false
}

function Wait-ForBackend {
    param([int]$TimeoutSeconds = 180)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $healthUrl = "http://127.0.0.1:$BackendPort/actuator/health"

    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                Write-Ok "Backend prêt ($healthUrl)"
                return $true
            }
        }
        catch {
            # Backend pas encore démarré
        }

        Start-Sleep -Seconds 3
        Write-Host "    En attente du backend (port $BackendPort)..." -ForegroundColor DarkGray
    }

    Write-Err "Backend indisponible après ${TimeoutSeconds}s"
    return $false
}

function Get-LocalIPv4 {
    $addresses = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -notlike '127.*' -and
            $_.IPAddress -notlike '169.254.*' -and
            $_.PrefixOrigin -ne 'WellKnown'
        } |
        Sort-Object InterfaceMetric

    if ($addresses) {
        return $addresses[0].IPAddress
    }

    return $null
}

function Start-ProcessWindow {
    param(
        [string]$Title,
        [string]$WorkingDirectory,
        [string]$Command
    )

    $escapedDir = $WorkingDirectory -replace "'", "''"
    $escapedCmd = $Command -replace "'", "''"

    Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoExit',
        '-Command',
        "`$Host.UI.RawUI.WindowTitle = '$Title'; Set-Location '$escapedDir'; $escapedCmd"
    ) | Out-Null

    Write-Ok "Fenêtre '$Title' ouverte"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  InvoiceAI - Démarrage complet" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

# --- Vérification des prérequis ---
Write-Step "Vérification des prérequis"

$requiredCommands = @()
if (-not $MobileOnly) {
    $requiredCommands += 'mvn'
    if (-not $SkipDocker) {
        $requiredCommands += 'docker'
    }
}
if (-not $BackendOnly) {
    $requiredCommands += 'node'
    $requiredCommands += 'npm'
}

foreach ($cmd in $requiredCommands) {
    if (-not (Test-CommandExists $cmd)) {
        Write-Err "Commande introuvable : $cmd"
        exit 1
    }
    Write-Ok "$cmd trouvé"
}

if (-not (Test-Path $BackendDir)) {
    Write-Err "Dossier backend introuvable : $BackendDir"
    exit 1
}

if (-not $BackendOnly -and -not (Test-Path $MobileDir)) {
    Write-Err "Dossier mobile introuvable : $MobileDir"
    exit 1
}

# --- PostgreSQL (Docker) ---
if (-not $MobileOnly -and -not $SkipDocker) {
    Write-Step "Démarrage de PostgreSQL (Docker)"

    Push-Location $BackendDir
    try {
        $previousErrorAction = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $containerRunning = (docker ps --filter "name=invoiceai-postgres" --filter "status=running" --format "{{.Names}}" | Out-String).Trim()
        $ErrorActionPreference = $previousErrorAction

        if ($containerRunning -eq 'invoiceai-postgres') {
            Write-Ok "Conteneur PostgreSQL déjà en cours d'exécution"
        }
        else {
            $composeExit = Invoke-External { docker compose up -d }
            if ($composeExit -ne 0) {
                $composeExit = Invoke-External { docker-compose up -d }
            }
            if ($composeExit -ne 0) {
                Write-Err "Impossible de démarrer PostgreSQL via Docker"
                exit 1
            }
            Write-Ok "Conteneur PostgreSQL démarré"
        }
    }
    finally {
        Pop-Location
    }

    if (-not (Wait-ForPort -Port $PostgresPort -Label 'PostgreSQL' -TimeoutSeconds 60)) {
        exit 1
    }
}
elseif (-not $MobileOnly -and $SkipDocker) {
    Write-Step "PostgreSQL (mode local)"
    if (-not (Test-PortOpen -Port $PostgresPort)) {
        Write-Warn "PostgreSQL ne répond pas sur le port $PostgresPort. Vérifiez que le service est démarré."
    }
    else {
        Write-Ok "PostgreSQL accessible sur le port $PostgresPort"
    }
}

# --- Backend Spring Boot ---
if (-not $MobileOnly) {
    Write-Step "Démarrage du backend Spring Boot"

    if (Test-PortOpen -Port $BackendPort) {
        Write-Warn "Le port $BackendPort est déjà utilisé"
        if (-not (Wait-ForBackend -TimeoutSeconds 5)) {
            Write-Err "Le port $BackendPort est occupé mais le backend ne répond pas"
            Write-Host "    Arrêtez le processus existant : netstat -ano | findstr :$BackendPort" -ForegroundColor DarkGray
            exit 1
        }
        Write-Ok "Backend déjà en cours d'exécution, démarrage ignoré"
    }
    else {
        Start-ProcessWindow -Title 'InvoiceAI Backend' -WorkingDirectory $BackendDir -Command 'mvn spring-boot:run'
        if (-not (Wait-ForBackend)) {
            exit 1
        }
    }

    Write-Host "    Swagger UI : http://localhost:$BackendPort/swagger-ui.html" -ForegroundColor DarkGray
}

if ($BackendOnly) {
    Write-Host ""
    Write-Host "Backend démarré." -ForegroundColor Green
    exit 0
}

# --- Dépendances npm ---
Write-Step "Vérification des dépendances mobile"

Push-Location $MobileDir
try {
    if (-not (Test-Path 'node_modules')) {
        Write-Host "    Installation des dépendances npm..." -ForegroundColor DarkGray
        $installExit = Invoke-External { npm install }
        if ($installExit -ne 0) {
            Write-Err "Échec de npm install"
            exit 1
        }
    }
    Write-Ok "Dépendances npm prêtes"
}
finally {
    Pop-Location
}

# --- Metro Bundler ---
Write-Step "Démarrage de Metro Bundler"

if (Test-PortOpen -Port $MetroPort) {
    Write-Warn "Metro semble déjà actif sur le port $MetroPort"
}
else {
    Start-ProcessWindow -Title 'InvoiceAI Metro' -WorkingDirectory $MobileDir -Command 'npm start'
    if (-not (Wait-ForPort -Port $MetroPort -Label 'Metro' -TimeoutSeconds 90)) {
        exit 1
    }
}

# --- Configuration ADB ---
Write-Step "Configuration ADB"

if (Test-CommandExists 'adb') {
    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $devices = adb devices 2>$null | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
    if ($devices) {
        adb reverse tcp:$MetroPort tcp:$MetroPort | Out-Null
        adb reverse tcp:$BackendPort tcp:$BackendPort | Out-Null
        Write-Ok "ADB reverse configuré (Metro:$MetroPort, Backend:$BackendPort)"
    }
    else {
        Write-Warn "Aucun appareil Android détecté. Branchez un téléphone ou lancez un émulateur."
    }
    $ErrorActionPreference = $previousErrorAction
}
else {
    Write-Warn "ADB introuvable. Installez Android SDK platform-tools si nécessaire."
}

# --- Rappel configuration API ---
Write-Step "Configuration API mobile"

$localIp = Get-LocalIPv4
Write-Host "    URLs API selon l'environnement :" -ForegroundColor DarkGray
Write-Host "      Émulateur Android : http://10.0.2.2:$BackendPort" -ForegroundColor DarkGray
Write-Host "      Simulateur iOS     : http://localhost:$BackendPort" -ForegroundColor DarkGray
if ($localIp) {
    Write-Host "      Appareil physique  : http://${localIp}:$BackendPort" -ForegroundColor DarkGray
    Write-Host "    Fichier à modifier : InvoiceAI\src\utils\constants.ts" -ForegroundColor DarkGray
}

# --- Lancement Android ---
if (-not $SkipAndroid) {
    Write-Step "Lancement de l'application Android"
    Write-Host "    Cette étape peut prendre plusieurs minutes..." -ForegroundColor DarkGray

    Push-Location $MobileDir
    try {
        $previousErrorAction = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $physicalDevices = adb devices 2>$null | Select-Object -Skip 1 | Where-Object {
            $_ -match '\tdevice$' -and $_ -notmatch '^emulator-'
        }
        $ErrorActionPreference = $previousErrorAction

        $androidCommand = if ($physicalDevices) { 'npm run android:device' } else { 'npm run android' }
        if ($physicalDevices) {
            Write-Host "    Appareil physique detecte, lancement cible..." -ForegroundColor DarkGray
        }

        $androidExit = Invoke-External { Invoke-Expression $androidCommand }
        if ($androidExit -ne 0) {
            Write-Err "Échec du lancement Android (code $androidExit)"
            exit $androidExit
        }
        Write-Ok "Application Android lancée"
    }
    finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Démarrage terminé" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Services actifs :" -ForegroundColor White
if (-not $MobileOnly) {
    Write-Host "  - Backend  : http://localhost:$BackendPort" -ForegroundColor White
    Write-Host "  - Swagger  : http://localhost:$BackendPort/swagger-ui.html" -ForegroundColor White
}
Write-Host "  - Metro    : http://localhost:$MetroPort" -ForegroundColor White
Write-Host ""
Write-Host "Fermez les fenêtres PowerShell Backend et Metro pour arrêter les services." -ForegroundColor DarkGray
