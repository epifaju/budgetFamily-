# Vérifie qu'une API InvoiceAI prod répond (health + uploads config implicite via 401 sur route protégée).
param(
    [Parameter(Mandatory = $false)]
    [string]$BaseUrl = "http://localhost:8085"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

Write-Host "Health: $base/actuator/health"
try {
    $health = Invoke-RestMethod -Uri "$base/actuator/health" -Method Get -TimeoutSec 15
    $status = $health.status
    Write-Host "  status = $status" -ForegroundColor $(if ($status -eq "UP") { "Green" } else { "Yellow" })
} catch {
    Write-Error "Health check failed: $_"
    exit 1
}

Write-Host "Auth probe (attendu 400/401 sans body): $base/api/v1/auth/login"
try {
    Invoke-WebRequest -Uri "$base/api/v1/auth/login" -Method Post -ContentType "application/json" -Body "{}" -TimeoutSec 15 | Out-Null
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -in 400, 401, 422) {
        Write-Host "  HTTP $code (API joignable)" -ForegroundColor Green
    } else {
        Write-Warning "  Reponse inattendue: $code"
    }
}

Write-Host "OK — deployer APP_UPLOADS_PUBLIC_BASE_URL=$base (HTTPS en prod) et PRODUCTION_API_URL identique cote mobile."
