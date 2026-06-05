# Test bout en bout : auth + POST /api/v1/invoices/images + GET image publique
param(
    [string]$BaseUrl = "http://localhost:8085",
    [string]$TestEmail = "",
    [string]$TestPassword = "UploadTest1!"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Pass($msg) { Write-Host "    [OK] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "    [ECHEC] $msg" -ForegroundColor Red; exit 1 }

if (-not $TestEmail) {
    $TestEmail = "e2e-upload-{0}@invoiceai.test" -f (Get-Date -Format "yyyyMMddHHmmss")
}

$tempDir = Join-Path $env:TEMP "invoiceai-e2e-upload"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
$imagePath = Join-Path $tempDir "receipt-e2e.jpg"

Write-Step "Création image JPEG de test"
try {
    Add-Type -AssemblyName System.Drawing
    $bmp = New-Object System.Drawing.Bitmap 320, 480
    $graphics = [System.Drawing.Graphics]::FromImage($bmp)
    $graphics.Clear([System.Drawing.Color]::White)
    $font = New-Object System.Drawing.Font("Arial", 14)
    $graphics.DrawString("E2E Upload Test", $font, [System.Drawing.Brushes]::Black, 20, 40)
    $graphics.Dispose()
    $bmp.Save($imagePath, [System.Drawing.Imaging.ImageFormat]::Jpeg)
    $bmp.Dispose()
    Write-Pass "Image: $imagePath ($((Get-Item $imagePath).Length) bytes)"
} catch {
    Write-Fail "Impossible de créer l'image: $_"
}

Write-Step "Health $base/actuator/health"
try {
    $health = Invoke-RestMethod -Uri "$base/actuator/health" -Method Get -TimeoutSec 15
    if ($health.status -ne "UP") {
        Write-Fail "Health status = $($health.status)"
    }
    Write-Pass "status UP"
} catch {
    Write-Fail "Backend inaccessible sur $base - lancez start-app.ps1 -BackendOnly ou port 8085. Detail: $_"
}

Write-Step "Inscription $TestEmail"
$registerBody = @{
    email = $TestEmail
    name = "E2E Upload"
    password = $TestPassword
    acceptedPrivacyPolicy = $true
} | ConvertTo-Json

$accessToken = $null
try {
    $reg = Invoke-RestMethod -Uri "$base/api/v1/auth/register" -Method Post `
        -ContentType "application/json" -Body $registerBody -TimeoutSec 30
    $accessToken = $reg.accessToken
    Write-Pass "Compte créé, token reçu"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 409 -or $status -eq 400) {
        Write-Host "    Inscription ignorée ($status), tentative login..." -ForegroundColor Yellow
        $loginBody = @{ email = $TestEmail; password = $TestPassword } | ConvertTo-Json
        try {
            $login = Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method Post `
                -ContentType "application/json" -Body $loginBody -TimeoutSec 30
            $accessToken = $login.accessToken
            Write-Pass "Login OK"
        } catch {
            Write-Fail "Login après échec register: $_"
        }
    } else {
        Write-Fail "Register: $_"
    }
}

if (-not $accessToken) {
    Write-Fail "Pas de accessToken"
}

Write-Step "Upload multipart POST /api/v1/invoices/images"
$uploadUrl = "$base/api/v1/invoices/images"
$imageUrl = $null

if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
    Write-Fail "curl.exe requis pour multipart binaire (Windows 10+)"
}

$curlOut = & curl.exe -s -w "`nHTTP_CODE:%{http_code}" -X POST $uploadUrl `
    -H "Authorization: Bearer $accessToken" `
    -F "file=@$imagePath;type=image/jpeg"
$split = $curlOut -split "HTTP_CODE:"
$httpCode = $split[-1].Trim()
$jsonBody = $split[0].Trim()
if ($httpCode -notin @("200", "201")) {
    Write-Fail "Upload HTTP $httpCode - $jsonBody"
}
$uploadResp = $jsonBody | ConvertFrom-Json
$imageUrl = $uploadResp.imageUrl

if (-not $imageUrl) {
    Write-Fail "Réponse sans imageUrl"
}
Write-Pass "imageUrl = $imageUrl"

Write-Step "GET image (acces public /uploads)"
$getCode = (& curl.exe -s -o NUL -w "%{http_code}" --max-time 10 $imageUrl).Trim()
if ($getCode -ne "200") {
    Write-Fail "GET image HTTP $getCode - $imageUrl"
}
$downloaded = Join-Path $tempDir "downloaded.jpg"
& curl.exe -s --max-time 10 -o $downloaded $imageUrl | Out-Null
$len = (Get-Item $downloaded).Length
if ($len -lt 100) {
    Write-Fail "Image trop petite ($len bytes)"
}
Write-Pass "Image telechargee ($len bytes)"

Write-Step "Vérification fichier local (dev)"
$backendUploads = Join-Path $PSScriptRoot "..\invoice-ai-backend\uploads"
if (Test-Path $backendUploads) {
    $recent = Get-ChildItem -Path $backendUploads -Recurse -File -Filter "*.jpg" |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($recent) {
        Write-Pass "Dernier fichier disque: $($recent.FullName)"
    }
}

Write-Host ""
Write-Host "=== TEST UPLOAD E2E REUSSI ===" -ForegroundColor Green
Write-Host "Email test: $TestEmail"
Write-Host "URL image:  $imageUrl"
