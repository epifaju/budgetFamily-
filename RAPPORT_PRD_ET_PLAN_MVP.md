# Rapport de comparaison PRD vs application & Plan d'action MVP

**Projet :** budgetFamily (InvoiceAI mobile + invoice-ai-backend)  
**Référence :** [PRD_BUDGET-FAMILY.md](./PRD_BUDGET-FAMILY.md) v2.0  
**Date :** 3 juin 2026  
**Périmètre analysé :** monorepo `InvoiceAI/` + `invoice-ai-backend/`

---

## Partie 1 — Comparaison PRD vs application

### Synthèse

| Zone | Avancement vs PRD MVP | Commentaire |
|------|------------------------|-------------|
| **Boucle principale** (scan → OCR → review → save → sync → dashboard) | **~80 %** | Fonctionnelle et utilisable |
| **MVP global (Phase 1 PRD)** | **~55–60 %** | Beaucoup de détails PRD manquants ou simplifiés |
| **Post-MVP (Phase 2–3)** | **~5 %** | Presque rien (normal) |
| **Conformité stack technique** | **~75 %** | Bon alignement, écarts ML / sécurité |

Le projet couvre bien le **cœur produit** du MVP, mais reste en deçà du PRD sur la **ML embarquée**, la **sécurité avancée**, l'**UX scanner**, et plusieurs **fonctionnalités « MVP »** listées comme faites dans la roadmap PRD (§8.1) mais pas encore implémentées.

---

### 1. Vision & proposition de valeur

| Promesse PRD | Statut | Réalité app |
|--------------|--------|-------------|
| Scanner tickets avec OCR **offline** | ✅ | ML Kit on-device, parser local riche (Carrefour, Decathlon, La Halle, etc.) |
| Classification **IA embarquée** | 🟡 | **Règles par mots-clés** (~30 termes), pas de TensorFlow Lite / BERT |
| Insights / analyses automatiques | 🟡 | Camembert + courbe 6 mois **locaux** ; pas d'insights prédictifs |
| **Offline-first** | ✅ | SQLite + queue sync ; scan/analyse locale sans réseau |
| **Privacy-first / chiffrement local** | 🟡 | JWT chiffré (`encrypted-storage`) ; SQLite **non** SQLCipher ; images non chiffrées |
| Analyse IA serveur (fallback) | ✅+ | Au-delà du PRD offline pur : `POST /ocr/parse` + bouton « Réanalyser avec l'IA » |

---

### 2. Capture & OCR (§3.1 — MVP)

| Fonctionnalité PRD | Statut | Détail |
|--------------------|--------|--------|
| Mode caméra dédié | ✅ | `CameraScreen` + onglet Scanner |
| Guides visuels de cadrage | ❌ | Pas de overlay de cadrage |
| Détection auto des bords | ❌ | Pas de scan automatique de document |
| Correction perspective auto | 🟡 | Recadrage **manuel** (`ImagePreview` + crop picker) |
| Amélioration contraste temps réel | ❌ | Preprocessing **après** capture (resize/JPEG) |
| Flash auto | ❌ | Flash désactivé (`flash: 'off'`) |
| Batch mode (plusieurs tickets) | ❌ | Un ticket à la fois |
| ML Kit Text Recognition | ✅ | `@react-native-ml-kit/text-recognition` |
| Extraction merchant, date, items, totaux, confidence | ✅ | Parser local + écran Review éditable |
| `payment_method` | ❌ | Champ non rempli à la sauvegarde |
| Post-traitement (typos, validation montants) | 🟡 | Normalisation + alerte si somme ≠ total ; pas de dictionnaire typo complet |
| Fallback si confiance < 0,7 | ✅ | Alerte UI + fallback IA backend si connecté |
| Critères perf (< 2 s capture, > 90 % OCR) | 🟡 | Non mesurés formellement ; qualité variable selon enseigne |

---

### 3. Classification (§3.2 — MVP)

| Niveau PRD | Statut | Détail |
|------------|--------|--------|
| **Niveau 1 — Règles métier (1000+ termes)** | 🟡 | ~30 mots-clés par catégorie dans `classificationService.ts` |
| **Niveau 2 — TFLite / BERT Tiny** | ❌ | Pas de `@tensorflow/tfjs-react-native` dans `package.json` |
| **Niveau 3 — Raffinement serveur** | 🟡 | Feedback envoyé au backend ; pas de réentraînement / OTA modèle |
| 5 catégories PRD | ✅ | ALIMENTAIRE, SANTE, TRANSPORT, VETEMENTS, AUTRES |
| Feedback utilisateur (correction catégorie) | ✅ | Queue locale + `POST /items/{id}/feedback` après sync |
| Réentraînement mensuel / MLOps | ❌ | Pipeline Airflow / MLflow absent |
| Accuracy > 80 % | 🟡 | Non instrumentée en prod |

---

### 4. Visualisation & Analytics (§3.3 — MVP)

| Fonctionnalité PRD | Statut | Détail |
|--------------------|--------|--------|
| Dashboard total + % par catégorie | ✅ | `DashboardScreen` |
| Camembert interactif | 🟡 | Pie chart affiché ; pas de drill-down au tap |
| Graphique évolution | ✅ | Trend 6 mois (`TrendChart`) |
| Comparaison vs mois précédent (▲ +12 %) | ❌ | Pas de badge d'évolution |
| Timeline jour/semaine/mois/année | ❌ | Période fixe (analytics mensuelle implicite) |
| Liste chronologique factures | ✅ | `InvoiceListScreen` |
| Recherche full-text | 🟡 | Searchbar (merchant/items) ; pas de recherche backend `pg_trgm` |
| Filtres catégorie | ✅ | Chips par catégorie |
| Export CSV/PDF | ❌ | PRD = premium ; absent |
| Édition ticket (catégorie, montant, date) | ✅ | `InvoiceDetailScreen` + Review |
| Suppression ticket | ✅ | |
| Tags personnalisés | ❌ | |
| Re-scan | 🟡 | Reprendre photo ou « Réanalyser avec l'IA » |

**Backend :** `GET /analytics/summary` existe, mais le mobile calcule le dashboard **uniquement en local** (`localAnalyticsService`) — l'API analytics n'est pas consommée.

---

### 5. Analyses avancées & Budgets (§3.4 — POST-MVP)

| Fonctionnalité | Statut |
|----------------|--------|
| Détection anomalies | ❌ (pas d'endpoint `/analytics/anomalies`) |
| Prédictions budgétaires | ❌ |
| Recommandations personnalisées | ❌ |
| Budgets par catégorie + alertes | ❌ (table `budgets` absente côté backend) |
| Notifications push budget | ❌ |

Conforme au statut **POST-MVP** du PRD — non implémenté, comme prévu.

---

### 6. Sync & Offline (§3.5 — MVP)

| Fonctionnalité PRD | Statut | Détail |
|--------------------|--------|--------|
| Architecture offline-first | ✅ | |
| Queue sync (PENDING/SUCCESS/FAILED) | ✅ | `sync_queue` SQLite |
| Trigger immédiat si réseau | ✅ | NetInfo + foreground |
| Sync périodique 15 min | ✅ | `SYNC_INTERVAL_MS` |
| Retry exponentiel (1m, 5m, 15m…) | 🟡 | Retry par compteur (max 5), pas d'intervalle exponentiel exact |
| Last-Write-Wins | 🟡 | Pull + skip si pending local ; pas de modal conflit |
| Badges sync (vert/jaune/rouge) | 🟡 | `SyncStatusChip` sur factures ; pas de badge global vert « tout sync » |
| Modal résolution conflit | ❌ | |
| Images tickets synchronisées | ❌ | `imageUrl` envoyé seulement si URL `http` ; photos locales restent sur l'appareil |

---

### 7. Authentification & Sécurité (§3.6 — MVP)

| Fonctionnalité PRD | Statut | Détail |
|--------------------|--------|--------|
| Email + mot de passe | ✅ | Login / Register |
| JWT access + refresh | ✅ | Refresh auto axios |
| Stockage sécurisé tokens | ✅ | `encrypted-storage` |
| Biométrie / Social login | ❌ | POST-MVP dans le PRD |
| Mot de passe oublié | ❌ | Écran `ForgotPassword` absent |
| Rate limiting (5 tentatives / 15 min) | ❌ | |
| Blocage après 10 échecs | ❌ | |
| SQLCipher SQLite | ❌ | SQLite standard |
| Chiffrement images tickets | ❌ | |
| Certificate pinning | ❌ | |
| Export RGPD (`GET /users/me/export`) | ❌ | Endpoint absent |
| Suppression compte | ✅ | Mobile + `DELETE /users/me` |
| Logout serveur (blacklist JWT) | 🟡 | Appel API ; blacklist **non implémentée** côté backend |

---

### 8. Architecture & Stack (§4)

| Élément PRD | Statut |
|-------------|--------|
| React Native 0.73, TS, Zustand, React Query | ✅ |
| react-navigation 6, Paper, chart-kit | ✅ |
| vision-camera, image-crop-picker, ML Kit | ✅ |
| `@tensorflow/tfjs-react-native` | ❌ absent |
| Structure `features/` modulaire | ❌ structure plate `src/screens`, `src/services` |
| Schéma SQLite mobile | ✅ aligné (+ `classification_feedback_queue`) |
| Spring Boot 3.2, PostgreSQL, Flyway, JWT | ✅ |
| Swagger, Actuator health | ✅ |
| Prometheus / Micrometer | ❌ |
| Cache Caffeine | ❌ (dépendance PRD non visible) |
| Package `ml/` backend (AnomalyDetector…) | ❌ ; seul `ClassificationService` + `OcrAiService` |

---

### 9. API REST (§4.2.3)

| Endpoint PRD | Statut |
|--------------|--------|
| Auth (register, login, refresh, logout) | ✅ |
| Invoices CRUD + batch | ✅ |
| Items feedback | ✅ |
| Items PUT | 🟡 backend oui, mobile non |
| Analytics summary | 🟡 backend oui, mobile non |
| Analytics trends / predictions / anomalies | ❌ |
| Users me / update / delete | ✅ |
| Users me **export** | ❌ |
| OCR parse | ✅ (ajout récent, hors PRD initial offline-only) |

---

### 10. UX & Navigation (§5)

| Élément PRD | Statut |
|-------------|--------|
| Onboarding 3 slides | ✅ |
| Permissions caméra | 🟡 demandées à l'ouverture Scanner, pas à l'onboarding |
| Bottom tabs Home / Scan / Stats / Profile | 🟡 4 onglets : Accueil, Scanner, **Factures**, Profil (pas d'onglet Stats séparé) |
| ForgotPassword | ❌ |
| Analytics → CategoryDetail / Budgets | ❌ |
| Profile → Export / About | ❌ |
| Animations scan (confetti, haptic) | ❌ |
| Indicateur confiance couleur | ✅ chips vert/orange/rouge sur Review |

---

### 11. Roadmap PRD §8.1 vs réalité

Le PRD marque beaucoup d'items Phase 1 (M1–M4) comme ✅ alors que le code montre :

| Item PRD « fait » | Réalité |
|-------------------|---------|
| CI/CD | 🟡 scripts locaux (`start-app.ps1`) ; pas de GitHub Actions visible |
| Classification ML (règles + TFLite) | 🟡 règles seulement |
| Feedback loop | ✅ partiel (collecte, pas réentraînement) |
| Security audit / RGPD | ❌ |
| Lancement App Stores | ❌ |

---

### 12. Écarts notables par rapport au nom « budgetFamily »

Le PRD et le code visent une app **individuelle** (InvoiceAI) :

- Pas de **budget familial partagé**, multi-utilisateurs, ou comptes liés
- Pas de gestion de **budgets** mensuels
- Persona « Marie, gestionnaire familiale » : besoin de **contrôle budget alimentaire** non couvert par des alertes/objectifs

---

### 13. Points forts (alignés PRD)

1. **Boucle scan complète** : caméra → preview/crop → OCR → review → sauvegarde
2. **Offline-first crédible** : DB locale + sync en arrière-plan
3. **Parser OCR avancé** pour tickets français retail (La Halle, Decathlon, etc.)
4. **Dashboard visuel** avec graphiques
5. **Auth JWT bout en bout** + profil éditable + suppression compte
6. **Stack technique** très proche du PRD (RN 0.73, Spring Boot 3.2, ML Kit)

---

### 14. Inventaire technique (état au 3 juin 2026)

#### Navigation mobile

```
RootStack
├── AuthStack (unauthenticated)
│   ├── Onboarding
│   ├── Login
│   └── Register
└── AppTabs (authenticated)
    ├── Dashboard
    ├── Scanner → Camera → ImagePreview → Review
    ├── Invoices → InvoiceList → InvoiceDetail
    └── Profile → ProfileHome → EditProfile | ChangePassword | DeleteAccount
```

#### Backend API principale

| Prefix | Endpoints |
|--------|-----------|
| `/api/v1/auth` | register, login, refresh, logout |
| `/api/v1/invoices` | list, get, create, update, delete, batch |
| `/api/v1/sync` | batch sync |
| `/api/v1/ocr` | parse (AI structuring) |
| `/api/v1/analytics` | summary |
| `/api/v1/users` | me (get/update/password/delete) |
| `/api/v1/items` | update, feedback |

#### Stockage

| Couche | Technologie |
|--------|-------------|
| Backend | PostgreSQL + Flyway |
| Mobile | SQLite (`react-native-sqlite-storage`) |
| Secrets mobile | `react-native-encrypted-storage` (JWT) |
| État | Zustand + TanStack Query |

---

## Partie 2 — Plan d'action MVP (2 à 4 semaines)

**Objectif :** combler les écarts **MVP** les plus impactants identifiés dans le PRD, sans entrer dans le Post-MVP (budgets, anomalies, premium, notifications).

---

### Semaine 1 — Fiabiliser le cœur produit ✅ (implémenté le 3 juin 2026)

**Priorité :** ce que l'utilisateur touche à chaque scan.

| # | Tâche | Statut | Détail implémentation |
|---|--------|--------|----------------------|
| 1.1 | **Élargir la classification** | ✅ | `classificationKeywords.ts` (~240 mots-clés) + règles enseignes ; `classificationService` avec hint marchand |
| 1.2 | **Capturer `payment_method`** | ✅ | `paymentMethodParser.ts` + menu sur Review + sauvegarde SQLite/sync |
| 1.3 | **Comparer au mois précédent** | ✅ | `monthComparison.ts` + badge ▲/▼ sur `DashboardScreen` |
| 1.4 | **Guides visuels caméra** | ✅ | Cadre + texte d'aide sur `CameraScreen` |
| 1.5 | **Permission caméra à l'onboarding** | ✅ | Demande à la fin de l'onboarding (`OnboardingScreen`) |

**Livrable S1 :** scan plus fiable, dashboard plus utile, moins de friction au 1er usage.

**Tests ajoutés :** `classificationService.test.ts`, `paymentMethodParser.test.ts`, `monthComparison.test.ts`

---

### Semaine 2 — Sync & données complètes ✅

**Priorité :** ne plus perdre d'information entre appareil et serveur.

| # | Tâche | Statut | Détail |
|---|--------|--------|--------|
| 2.1 | **Upload images tickets** | ✅ | Backend `POST /api/v1/invoices/images`, stockage `./uploads`, `ReceiptImageStorageService` ; mobile `invoiceImageApi` + `imageUploadService` avant CREATE/UPDATE sync |
| 2.2 | **Badges sync** (vert / jaune / rouge) | ✅ | `syncStore` + `buildSyncHealth` + `SyncHealthChip` sur Profil ; bannière sur Dashboard |
| 2.3 | **Retry exponentiel** | ✅ | `syncRetryUtils` (1m → 5m → 15m → 1h → 6h), filtre `isReadyForRetry` dans `syncService` |
| 2.4 | **Analytics backend** | ⏭️ reporté | Dashboard reste **local-only** volontairement (S4 ou post-MVP) |

**Livrable S2 :** tickets avec `imageUrl` HTTP côté serveur, sync visible (🟢/🟡/🔴), reprises automatiques espacées.

**Tests ajoutés :** `syncRetryUtils.test.ts`, `syncHealth.test.ts`

**Config dev upload :** `app.uploads` dans `application.yml` ; pour téléphone USB, `adb reverse` + URLs `localhost:8085` ; WiFi → `APP_UPLOADS_PUBLIC_BASE_URL` dans `application-local.yml`.

---

### Semaine 3 — Auth, sécurité & conformité minimale ✅

**Priorité :** confiance utilisateur + exigences PRD MVP.

| # | Tâche | Statut | Détail |
|---|--------|--------|--------|
| 3.1 | **Mot de passe oublié** | ✅ | `POST /auth/forgot-password`, `POST /auth/reset-password`, écrans Forgot / Reset ; code loggé côté backend en dev |
| 3.2 | **Validation mot de passe PRD** | ✅ | `PasswordPolicy` Java + `passwordValidation.ts` (8 car., maj, chiffre, spécial) |
| 3.3 | **Rate limiting login** | ✅ | `LoginRateLimiterService` — 5 échecs / 15 min par email |
| 3.4 | **Export RGPD** | ✅ | `GET /users/me/export` JSON + bouton Profil (partage) |
| 3.5 | **Consentement / politique** | ✅ | Checkbox inscription + `PrivacyPolicyScreen` ; `privacy_consent_at` en base |

**Livrable S3 :** parcours compte crédible pour une beta publique.

**Migration :** `V6__password_reset_and_privacy.sql`

**Tests ajoutés :** `passwordValidation.test.ts`

---

### Semaine 4 — Polish MVP & préparation beta ✅

**Priorité :** fermer les trous UX et mesurer la qualité.

| # | Tâche | Statut | Détail |
|---|--------|--------|--------|
| 4.1 | **Drill-down dashboard** | ✅ | Tap catégorie → onglet Factures filtré (`InvoiceList` + params) |
| 4.2 | **Indicateurs confiance** | ✅ | `confidenceUtils` + `ConfidenceBadge` (Review + détail facture) |
| 4.3 | **Benchmark OCR** | ✅ | 23 fixtures + `npm run test:benchmark` (~98 % champs, 87 % utilisables) |
| 4.4 | **Checklist E2E beta** | ✅ | `docs/BETA_E2E_CHECKLIST.md` |
| 4.5 | **Config prod mobile** | ✅ | `src/config/apiConfig.ts` + `CONFIGURATION_PROD.md` |

**Livrable S4 :** app prête pour beta fermée (~20 testeurs).

**Commande benchmark :** `npm run test:benchmark`

---

### Ce qu'on repousse volontairement (Post-MVP)

| Fonctionnalité | Raison |
|----------------|--------|
| TensorFlow Lite / BERT | Gros investissement ; règles enrichies suffisent pour beta |
| Budgets + alertes push | Phase 2 PRD |
| Anomalies / prédictions | Phase 2 PRD |
| Export CSV/PDF premium | Phase 2 PRD |
| SQLCipher / chiffrement images | Important mais lourd ; après beta si audit le demande |
| Biométrie / social login | Post-MVP PRD |
| Budget familial multi-utilisateurs | Hors scope InvoiceAI actuel |

---

### Ordre recommandé (1 développeur)

```
S1.1 → S1.3 → S1.4 → S2.1 → S3.1 → S4.3 → S4.5
```

Les items **1.1, 2.1, 3.1, 4.3** sont les plus structurants ; le reste peut glisser dans la marge des 4 semaines.

---

### Critères de succès fin semaine 4 (alignés PRD §13.1)

- [ ] 20 testeurs complètent l'onboarding
- [ ] > 80 % scannent ≥ 3 tickets
- [ ] OCR > 85 % sur échantillon de 100 tickets tests
- [ ] Classification > 75 % sans correction (mesuré via feedback)
- [ ] 0 crash critique sur 1 semaine
- [x] Export RGPD fonctionnel
- [x] Sync images + statuts visibles

---

### Prochaine étape concrète

**MVP plan 4 semaines terminé.** Poursuite recommandée :

1. Déployer backend HTTPS + mettre à jour `PRODUCTION_API_URL`
2. Recruter ~20 beta testeurs (`docs/BETA_E2E_CHECKLIST.md`)
3. Enrichir fixtures OCR avec photos réelles scannées

---

*Document généré à partir de l'analyse du code et du PRD v2.0. À mettre à jour au fil des sprints.*
