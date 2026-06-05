# Affiche les logs React Native lies a sync / upload (Ctrl+C pour arreter)
Write-Host "Surveillance sync/upload - effectuez le test sur le telephone." -ForegroundColor Cyan
Write-Host "Filtre: invoiceImage, imageUpload, syncService, Upload failed, Network Error" -ForegroundColor Gray
Write-Host "En ecoute (Ctrl+C pour arreter). Aucune ligne = pas encore d'activite sur l'app." -ForegroundColor DarkGray
if (Get-Command adb -ErrorAction SilentlyContinue) {
    adb reverse tcp:8085 tcp:8085 2>$null | Out-Null
    Write-Host "adb reverse tcp:8085 OK (appareil physique -> PC)" -ForegroundColor DarkGray
}
Write-Host "Apres modif API : secouer le tel. -> Reload dans Metro." -ForegroundColor DarkGray
Write-Host ""

adb logcat -c | Out-Null
$pattern = 'invoiceImage|imageUpload|syncService|Upload failed|Upload OK|Network Error|Sync batch|getApiUrl|Error syncing entry'

adb logcat -v brief -s ReactNativeJS:I | ForEach-Object {
    $line = $_.ToString()
    if ($line -match $pattern) {
        Write-Host $line -ForegroundColor Yellow
    }
}
