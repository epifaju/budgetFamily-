# Test manuel appareil — sync + photo ticket

Prérequis : `.\start-app.ps1` (ou backend + Metro), téléphone USB, `adb reverse tcp:8085 tcp:8085`.

## Checklist (cocher au fur et à mesure)

### A. Préparation (30 s)

- [ ] Téléphone connecté : `adb devices` → `device`
- [ ] Backend OK : `.\deploy\validate-prod.ps1 -BaseUrl "http://localhost:8085"`
- [ ] App InvoiceAI ouverte, **connecté** (pas invité)

### B. Nouveau ticket avec photo (2 min)

1. Onglet **Scanner** (icône caméra).
2. Prendre une photo (ou galerie si disponible) → **Recadrer** si proposé → **Continuer**.
3. Écran **Revue** : vérifier montant / marchand → **Enregistrer**.
4. Attendre le retour (liste ou confirmation).

### C. Synchronisation (1 min)

1. Onglet **Profil** (icône personne).
2. Section **Synchronisation** :
   - Badge **🟡 En attente** puis **🟢 À jour** après sync.
   - **En attente** doit repasser à **0**.
3. Si bloqué : bouton **Synchroniser** (spinner puis fin).
4. **Dernière sync** : date/heure récente.

### D. Affichage photo (30 s)

1. Onglet **Factures**.
2. Ouvrir la facture **que vous venez de créer** (en haut de liste).
3. **Critère de succès** : aperçu photo visible sous les infos (pas zone vide grise).

### E. Vérification serveur (optionnel, PC)

```powershell
Get-ChildItem invoice-ai-backend\uploads\invoices -Recurse -File |
  Sort-Object LastWriteTime -Descending | Select-Object -First 3 FullName, LastWriteTime
```

Un fichier `.jpg` récent avec horodatage = upload OK.

## Logs pendant le test

Terminal 1 (PC) :

```powershell
adb logcat -c
adb logcat -s ReactNativeJS:I | Select-String "invoiceImage|imageUpload|sync"
```

Succès attendu :

- `[invoiceImageApi] Upload start`
- pas de `[imageUploadService] Upload failed`

## Dépannage rapide

| Symptôme | Action |
|----------|--------|
| Network Error | `adb reverse tcp:8085 tcp:8085` ; reload Metro (API = `127.0.0.1:8085` sur Android physique) ; ou WiFi : `global.__INVOICE_AI_DEV_HOST__ = '192.168.x.x'` dans `index.js` |
| 🟴 erreurs sync | Profil → **Synchroniser** (reset failed) ; vérifier login |
| Photo vide, sync verte | Rouvrir la facture ; si URL `localhost` → rebuild app récente |
| Caméra crash | Fermer/réouvrir app, onglet Scanner |

## Scripts automatisés (API seule, sans app)

```powershell
.\deploy\test-upload-e2e.ps1
cd InvoiceAI; npm run test:upload:e2e
```
