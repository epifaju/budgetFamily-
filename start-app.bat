@echo off
setlocal

echo.
echo ========================================
echo   InvoiceAI - Demarrage complet
echo ========================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-app.ps1" %*

if errorlevel 1 (
    echo.
    echo [ERREUR] Le demarrage a echoue.
    pause
    exit /b 1
)

endlocal
