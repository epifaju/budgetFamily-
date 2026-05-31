# Product Requirements Document (PRD) v2.0
## Application Mobile : Scan, Classification et Analyse Intelligente de Factures

---

## 1. Vision Produit

### 1.1 Problème à Résoudre
Les utilisateurs perdent le contrôle de leurs dépenses par manque d'outils simples pour capturer, catégoriser et analyser leurs achats quotidiens. Les solutions existantes nécessitent une saisie manuelle fastidieuse ou une connexion internet permanente.

### 1.2 Solution Proposée
Une application mobile intelligente permettant de :
- **Scanner instantanément** factures et tickets avec reconnaissance OCR offline
- **Classifier automatiquement** les achats par catégorie via IA embarquée
- **Visualiser et analyser** les dépenses avec insights personnalisés
- **Fonctionner offline** avec synchronisation intelligente en arrière-plan

### 1.3 Proposition de Valeur Unique
- ✅ **Offline-first** : Fonctionne sans connexion internet
- 🤖 **IA embarquée** : Classification intelligente sur appareil
- 📊 **Insights automatiques** : Analyses prédictives et recommandations
- 🔒 **Privacy-first** : Données chiffrées localement

---

## 2. Public Cible et Personas

### 2.1 Segments Prioritaires

#### Persona 1 : Marie, 32 ans - Gestionnaire Familiale
- **Contexte** : Mère de 2 enfants, gère budget familial serré
- **Besoins** : Contrôler dépenses alimentaires, détecter surconsommation
- **Comportement** : Smartphone Android milieu de gamme, connexion intermittente
- **Pain points** : Oublie de noter les achats, dépasse budget sans s'en rendre compte

#### Persona 2 : Thomas, 28 ans - Freelance
- **Contexte** : Consultant indépendant, doit justifier frais professionnels
- **Besoins** : Tracer dépenses déductibles, export comptable
- **Comportement** : iPhone, connexion stable, tech-savvy
- **Pain points** : Perd des tickets, oublie de classer dépenses perso/pro

#### Persona 3 : Amadou, 35 ans - Commerçant (Marché Émergent)
- **Contexte** : Petit commerce au Sénégal, connectivité limitée
- **Besoins** : Suivre achats fournisseurs, analyser rentabilité
- **Comportement** : Android entrée de gamme, 3G instable
- **Pain points** : Pas d'outils adaptés, tout en papier

### 2.2 Marchés Cibles
- **Primaire** : France, Belgique, Suisse (phase 1 - 6 mois)
- **Secondaire** : Afrique francophone (phase 2 - 12 mois)
- **Tertiaire** : Expansion internationale (phase 3 - 18 mois)

---

## 3. Fonctionnalités Détaillées

### 3.1 Capture et Reconnaissance (OCR) 🎯 MVP

#### 3.1.1 Capture Photo Optimisée
**User Story** : *"En tant qu'utilisateur, je veux scanner rapidement un ticket pour qu'il soit analysé automatiquement"*

**Fonctionnalités** :
- Mode caméra dédié avec guides visuels de cadrage
- Détection automatique des bords du document
- Correction perspective automatique
- Amélioration contraste/luminosité temps réel
- Support flash automatique en faible luminosité
- Batch mode : scanner plusieurs tickets d'affilée

**Critères d'Acceptation** :
- Temps de capture < 2 secondes
- Détection bords réussie > 95% des cas
- Fonctionne en conditions lumineuses variées

#### 3.1.2 Extraction OCR Offline
**Technologie Retenue** : **ML Kit Text Recognition v2** (Google)

**Justification** :
- ✅ Natif Android/iOS, performances optimales
- ✅ Modèle embarqué léger (~10 MB)
- ✅ Support 100+ langues
- ✅ Gratuit, bien maintenu
- ✅ Inférence < 1 seconde sur appareil entrée de gamme
- ❌ Alternative PaddleOCR : complexité intégration, maintenance lourde

**Données Extraites** :
```json
{
  "merchant": "Carrefour City",
  "date": "2025-11-13",
  "items": [
    {
      "name": "Lait demi-écrémé",
      "quantity": 2,
      "unit_price": 1.25,
      "total_price": 2.50
    }
  ],
  "subtotal": 45.30,
  "tax": 4.53,
  "total": 49.83,
  "payment_method": "CB",
  "confidence_score": 0.92
}
```

**Post-Traitement Intelligent** :
- Correction fautes typo fréquentes (dictionnaire contextuel)
- Validation format montants (regex, cohérence calculs)
- Détection anomalies (total ≠ somme articles → alerte)
- Gestion tickets multi-colonnes/formats complexes

**Critères de Succès** :
- ✅ **Précision OCR** : > 90% sur tickets standards
- ✅ **Taux d'extraction complète** : > 85% (tous champs remplis)
- ✅ **Latence** : < 3 secondes du scan à l'affichage
- ⚠️ **Fallback** : Mode saisie manuelle si score confiance < 0.7

---

### 3.2 Classification Intelligente 🤖 MVP

#### 3.2.1 Système de Classification Hybride

**Architecture à 3 Niveaux** :

**Niveau 1 : Règles Métier (Baseline)**
- Dictionnaire de mots-clés par catégorie (1000+ termes)
- Règles heuristiques (ex: "Carrefour" → alimentaire probable)
- Précision : ~75%, latence : < 10ms
- **Utilisation** : Classification immédiate, fallback si ML échoue

**Niveau 2 : Modèle ML Embarqué (TensorFlow Lite)**
- Architecture : **BERT Tiny** fine-tuné (4.4M paramètres)
- Input : Embeddings texte (nom article + contexte magasin)
- Output : 5 catégories + score confiance

**Catégories** :
1. 🍎 Alimentaire
2. 💊 Santé & Pharmacie
3. 🚗 Transport & Déplacement
4. 👕 Vêtements & Accessoires
5. 🏠 Maison & Autres

**Niveau 3 : Modèle Serveur (Raffinement)**
- Synchronisation hebdomadaire : envoi articles ambigus (confiance < 0.8)
- Modèle BERT Large côté serveur
- Mise à jour modèle embarqué tous les 2 mois

#### 3.2.2 Feedback Loop et Amélioration Continue

**User Story** : *"Je veux corriger une classification incorrecte pour que l'app apprenne de mes préférences"*

**Flow** :
1. Utilisateur corrige catégorie → Stockage local + flag "corrected"
2. Synchronisation → Backend collecte corrections
3. Réentraînement mensuel avec données anonymisées
4. Déploiement nouveau modèle TFLite via OTA update

**Métriques Collectées** :
- Taux d'acceptation classification (% sans correction)
- Matrice de confusion par catégorie
- Latence d'inférence par modèle appareil

**Critères de Succès** :
- ✅ **Accuracy initiale** : > 80%
- ✅ **Accuracy après 3 mois feedback** : > 88%
- ✅ **Latence classification** : < 100ms
- ✅ **Taille modèle** : < 15 MB

---

### 3.3 Visualisation et Analytics 📊 MVP

#### 3.3.1 Dashboard Principal

**Écran d'Accueil** :
```
┌─────────────────────────────┐
│  Novembre 2025              │
│  Total dépensé : 1 247.50€  │
│  ▲ +12% vs mois dernier     │
├─────────────────────────────┤
│  🍎 Alimentaire    45%  561€│
│  🚗 Transport      28%  349€│
│  💊 Santé          15%  187€│
│  👕 Vêtements       8%  100€│
│  🏠 Autres          4%   51€│
├─────────────────────────────┤
│  [Diagramme Camembert]      │
│  [Graphique Évolution]      │
└─────────────────────────────┘
```

**Composants Interactifs** :
- **Camembert** : Tap catégorie → drill-down détails
- **Timeline** : Swipe période (jour/semaine/mois/année)
- **Comparaison** : Badge évolution vs période précédente

**Librairie** : `react-native-chart-kit` ou `victory-native`

#### 3.3.2 Historique et Recherche

**Fonctionnalités** :
- Liste chronologique avec grouping par date
- Recherche full-text (nom article, magasin)
- Filtres multi-critères (catégorie, montant, période)
- Export CSV/PDF (feature premium)

#### 3.3.3 Édition et Gestion

**Actions Utilisateur** :
- ✏️ Modifier catégorie, montant, date
- 🗑️ Supprimer ticket
- 📸 Ajouter photo supplémentaire
- 🏷️ Ajouter tags personnalisés
- 🔄 Re-scan si OCR raté

---

### 3.4 Analyses Avancées et Recommandations 🧠 POST-MVP

#### 3.4.1 Insights Automatiques (Backend ML)

**Détection d'Anomalies** :
- Algorithme : Isolation Forest
- Alertes : "Dépense inhabituelle de 250€ en Santé"
- Fréquence : Analyse hebdomadaire

**Prédiction Budgétaire** :
- Modèle : ARIMA ou Prophet (séries temporelles)
- Output : "Projection fin de mois : 1 450€ (dépassement 150€)"
- Mise à jour : Daily si données suffisantes

**Recommandations Personnalisées** :
- "Vous dépensez 30% de plus en alimentaire que la moyenne"
- "Astuce : Limitez achats impulsifs le samedi"
- "Objectif : Économisez 100€ ce mois en Transport"

**Critères d'Activation** :
- Minimum 30 tickets scannés
- Historique sur 3 mois minimum
- Opt-in utilisateur (RGPD)

#### 3.4.2 Goals et Budgets

**User Story** : *"Je veux définir un budget mensuel par catégorie et recevoir des alertes"*

**Fonctionnalités** :
- Définition budgets personnalisés par catégorie
- Progression temps réel avec jauge visuelle
- Notifications push si > 80% budget atteint
- Comparaison performances vs objectifs

---

### 3.5 Synchronisation et Mode Offline 🔄 MVP

#### 3.5.1 Architecture Offline-First

**Principe** : L'app fonctionne entièrement sans connexion, la synchro est un "bonus"

**Queue de Synchronisation** :
```javascript
// Structure données queue
{
  id: "uuid-local",
  action: "CREATE_INVOICE", // CREATE | UPDATE | DELETE
  payload: {...},
  timestamp: 1699876543,
  retry_count: 0,
  status: "PENDING" // PENDING | SYNCING | SUCCESS | FAILED
}
```

**Stratégie Sync** :
1. **Trigger immédiat** si connexion détectée
2. **Background sync** toutes les 15 min (iOS) / flexible (Android)
3. **Retry exponentiel** : 1min, 5min, 15min, 1h, 6h
4. **Conflit resolution** : Last-Write-Wins avec timestamp serveur

#### 3.5.2 Indicateurs Visuels

- 🟢 Badge "Synchronisé" : Toutes données à jour
- 🟡 Badge "En attente" : X opérations en queue
- 🔴 Badge "Erreur sync" : Action requise (ex: conflit)

**Gestion Conflits** :
- Affichage modal : "Modification concurrente détectée"
- Options : Garder local / Garder serveur / Fusionner manuel

---

### 3.6 Authentification et Sécurité 🔒 MVP

#### 3.6.1 Système d'Authentification

**Flow JWT Standard** :
```
1. Login → Backend retourne access_token (15min) + refresh_token (30j)
2. Stockage Secure Storage chiffré
3. Requêtes API → Header "Authorization: Bearer {access_token}"
4. Si 401 → Refresh automatique → Retry request
5. Si refresh échoue → Logout + redirect login
```

**Options Login** :
- 📧 Email + Password (MVP)
- 📱 Biométrie (Face ID / Touch ID / Fingerprint) - POST-MVP
- 🔗 Social Login (Google/Apple) - POST-MVP

**Exigences Sécurité** :
- Password : min 8 caractères, 1 majuscule, 1 chiffre, 1 spécial
- Rate limiting : Max 5 tentatives / 15 min
- Blocage compte après 10 échecs

#### 3.6.2 Protection des Données

**Chiffrement Local** :
- SQLite Database : **SQLCipher** (AES-256)
- Clé dérivée depuis password utilisateur (PBKDF2)
- Images tickets : Chiffrement individuel avant stockage

**Transmission** :
- HTTPS/TLS 1.3 obligatoire
- Certificate Pinning (production)
- Request signing pour opérations sensibles

**Conformité RGPD** :
- Consentement explicite collecte données
- Export données personnelles (JSON)
- Suppression compte + données irréversible
- Anonymisation data training ML

---

## 4. Architecture Technique Détaillée

### 4.1 Frontend Mobile (React Native)

#### 4.1.1 Stack Technologique

```json
{
  "core": {
    "react-native": "0.73.2",
    "typescript": "5.3.3"
  },
  "navigation": {
    "react-navigation": "6.1.9"
  },
  "state": {
    "zustand": "4.4.7",
    "react-query": "5.17.9"
  },
  "ui": {
    "react-native-paper": "5.11.6",
    "react-native-chart-kit": "6.12.0",
    "react-native-vector-icons": "10.0.3"
  },
  "camera": {
    "react-native-vision-camera": "3.6.17",
    "react-native-image-crop-picker": "0.40.0"
  },
  "ml": {
    "@react-native-ml-kit/text-recognition": "1.2.0",
    "@tensorflow/tfjs-react-native": "0.8.0"
  },
  "storage": {
    "react-native-sqlite-storage": "6.0.1",
    "react-native-encrypted-storage": "4.0.3"
  },
  "network": {
    "axios": "1.6.5",
    "@react-native-community/netinfo": "11.1.0"
  }
}
```

#### 4.1.2 Architecture Modulaire

```
src/
├── core/
│   ├── navigation/
│   ├── theme/
│   └── i18n/
├── features/
│   ├── auth/
│   │   ├── screens/
│   │   ├── services/
│   │   └── store/
│   ├── scanner/
│   │   ├── screens/
│   │   ├── services/ (OCR, ML)
│   │   └── store/
│   ├── invoices/
│   │   ├── screens/
│   │   ├── components/
│   │   └── store/
│   └── analytics/
├── shared/
│   ├── components/
│   ├── hooks/
│   ├── utils/
│   └── types/
├── services/
│   ├── api/
│   ├── database/
│   ├── sync/
│   └── ml/
└── App.tsx
```

#### 4.1.3 Database Schema (SQLite)

```sql
-- Users (cache profil)
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  email TEXT NOT NULL,
  name TEXT,
  created_at INTEGER,
  synced_at INTEGER
);

-- Invoices
CREATE TABLE invoices (
  id TEXT PRIMARY KEY,
  user_id TEXT,
  merchant TEXT,
  date TEXT,
  total REAL,
  subtotal REAL,
  tax REAL,
  payment_method TEXT,
  image_uri TEXT,
  confidence_score REAL,
  status TEXT, -- 'PENDING' | 'SYNCED' | 'ERROR'
  created_at INTEGER,
  updated_at INTEGER,
  synced_at INTEGER,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Items
CREATE TABLE items (
  id TEXT PRIMARY KEY,
  invoice_id TEXT,
  name TEXT,
  quantity REAL,
  unit_price REAL,
  total_price REAL,
  category TEXT,
  confidence_score REAL,
  is_corrected INTEGER DEFAULT 0,
  created_at INTEGER,
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Sync Queue
CREATE TABLE sync_queue (
  id TEXT PRIMARY KEY,
  action TEXT, -- 'CREATE' | 'UPDATE' | 'DELETE'
  entity_type TEXT, -- 'invoice' | 'item'
  entity_id TEXT,
  payload TEXT, -- JSON
  retry_count INTEGER DEFAULT 0,
  status TEXT, -- 'PENDING' | 'SYNCING' | 'SUCCESS' | 'FAILED'
  error TEXT,
  created_at INTEGER,
  updated_at INTEGER
);

-- Indexes
CREATE INDEX idx_invoices_user_date ON invoices(user_id, date);
CREATE INDEX idx_items_invoice ON items(invoice_id);
CREATE INDEX idx_sync_queue_status ON sync_queue(status);
```

---

### 4.2 Backend (Spring Boot)

#### 4.2.1 Stack Technologique

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.1</spring-boot.version>
</properties>

<dependencies>
    <!-- Core Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>

    <!-- Cache -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- Monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
</dependencies>
```

#### 4.2.2 Architecture Layers

```
com.invoiceai/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── CacheConfig.java
├── domain/
│   ├── User.java
│   ├── Invoice.java
│   ├── Item.java
│   └── Feedback.java
├── repository/
│   ├── UserRepository.java
│   ├── InvoiceRepository.java
│   └── ItemRepository.java
├── service/
│   ├── AuthService.java
│   ├── InvoiceService.java
│   ├── ClassificationService.java
│   ├── AnalyticsService.java
│   └── SyncService.java
├── controller/
│   ├── AuthController.java
│   ├── InvoiceController.java
│   ├── AnalyticsController.java
│   └── SyncController.java
├── dto/
│   ├── request/
│   └── response/
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── ml/
│   ├── ClassificationModel.java
│   ├── AnomalyDetector.java
│   └── PredictionService.java
└── InvoiceAiApplication.java
```

#### 4.2.3 API REST Endpoints

```yaml
# Authentication
POST   /api/v1/auth/register          # Inscription
POST   /api/v1/auth/login             # Login
POST   /api/v1/auth/refresh           # Refresh token
POST   /api/v1/auth/logout            # Logout

# Invoices
GET    /api/v1/invoices               # Liste factures (paginée)
GET    /api/v1/invoices/{id}          # Détail facture
POST   /api/v1/invoices               # Créer facture
PUT    /api/v1/invoices/{id}          # Modifier facture
DELETE /api/v1/invoices/{id}          # Supprimer facture
POST   /api/v1/invoices/batch         # Sync batch (mobile)

# Items
GET    /api/v1/items                  # Liste items
POST   /api/v1/items/{id}/feedback    # Feedback classification

# Analytics
GET    /api/v1/analytics/summary      # Résumé dépenses
GET    /api/v1/analytics/trends       # Tendances
GET    /api/v1/analytics/predictions  # Prédictions
GET    /api/v1/analytics/anomalies    # Détection anomalies

# User
GET    /api/v1/users/me               # Profil
PUT    /api/v1/users/me               # Modifier profil
DELETE /api/v1/users/me               # Supprimer compte
GET    /api/v1/users/me/export        # Export données RGPD
```

#### 4.2.4 Database Schema (PostgreSQL)

```sql
-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Full-text search

-- Users
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(255),
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  last_login TIMESTAMP
);

-- Invoices
CREATE TABLE invoices (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  merchant VARCHAR(255),
  date DATE NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2),
  tax DECIMAL(10,2),
  payment_method VARCHAR(50),
  image_url TEXT,
  confidence_score DECIMAL(3,2),
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  deleted_at TIMESTAMP
);

-- Items
CREATE TABLE items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  invoice_id UUID REFERENCES invoices(id) ON DELETE CASCADE,
  name VARCHAR(500) NOT NULL,
  quantity DECIMAL(10,3) DEFAULT 1,
  unit_price DECIMAL(10,2),
  total_price DECIMAL(10,2) NOT NULL,
  category VARCHAR(50) NOT NULL,
  confidence_score DECIMAL(3,2),
  is_corrected BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Feedback (pour ML retraining)
CREATE TABLE feedback (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  item_id UUID REFERENCES items(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  original_category VARCHAR(50),
  corrected_category VARCHAR(50) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Categories (référentiel)
CREATE TABLE categories (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  icon VARCHAR(50),
  keywords TEXT[], -- Array de mots-clés
  created_at TIMESTAMP DEFAULT NOW()
);

-- User Budgets
CREATE TABLE budgets (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  category VARCHAR(50) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  period VARCHAR(20) NOT NULL, -- 'MONTHLY' | 'WEEKLY'
  start_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(user_id, category, period)
);

-- Indexes
CREATE INDEX idx_invoices_user_date ON invoices(user_id, date DESC);
CREATE INDEX idx_items_invoice ON items(invoice_id);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_feedback_item ON feedback(item_id);
CREATE INDEX idx_items_name_trgm ON items USING gin(name gin_trgm_ops);
```

---

### 4.3 Machine Learning Pipeline

#### 4.3.1 Classification Model Training

**Dataset Construction** :
```python
# Structure dataset
{
  "item_name": "Lait demi-écrémé 1L",
  "merchant": "Carrefour",
  "unit_price": 1.25,
  "category": "alimentaire",
  "language": "fr"
}

# Sources de données:
# 1. Dataset public OpenFoodFacts (300k produits alimentaires)
# 2. Scraping catalogues e-commerce (avec consentement)
# 3. Feedback utilisateurs (anonymisé)
# 4. Augmentation synthetic data (synonymes, fautes typo)
```

**Model Architecture** :
```python
# TensorFlow Keras
model = Sequential([
  Embedding(vocab_size=10000, output_dim=128),
  Bidirectional(LSTM(64, return_sequences=True)),
  GlobalMaxPooling1D(),
  Dense(128, activation='relu'),
  Dropout(0.5),
  Dense(5, activation='softmax')  # 5 catégories
])

# Compilation
model.compile(
  optimizer='adam',
  loss='sparse_categorical_crossentropy',
  metrics=['accuracy', 'top_k_categorical_accuracy']
)

# Training
history = model.fit(
  train_dataset,
  validation_data=val_dataset,
  epochs=50,
  batch_size=32,
  callbacks=[
    EarlyStopping(patience=5),
    ModelCheckpoint('best_model.h5'),
    ReduceLROnPlateau(factor=0.5, patience=3)
  ]
)
```

**Conversion TensorFlow Lite** :
```python
# Quantization pour réduire taille modèle
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]

tflite_model = converter.convert()

# Sauvegarde
with open('classification_model.tflite', 'wb') as f:
  f.write(tflite_model)

# Résultat: Modèle ~8MB, inference ~50ms
```

#### 4.3.2 Continuous Learning Pipeline

**Architecture MLOps** :
```
┌─────────────┐
│   Mobile    │ ──feedback──> ┌──────────────┐
│     App     │               │   Backend    │
└─────────────┘               │   (Spring)   │
                              └──────┬───────┘
                                     │
                              ┌──────▼───────┐
                              │  PostgreSQL  │
                              │  (Feedback   │
                              │   Storage)   │
                              └──────┬───────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │   ML Pipeline (Apache Airflow)  │
                    │   - Extract feedback monthly    │
                    │   - Preprocess & augment data   │
                    │   - Retrain model               │
                    │   - Validate performance        │
                    │   - Deploy if accuracy > 85%    │
                    └────────────────┬────────────────┘
                                     │
                              ┌──────▼───────┐
                              │  Model       │
                              │  Registry    │
                              │  (MLflow)    │
                              └──────┬───────┘
                                     │
                              ┌──────▼───────┐
                              │  CDN/S3      │
                              │  (TFLite     │
                              │   Models)    │
                              └──────────────┘
```

**Model Update Strategy** :
```javascript
// Mobile: Vérification version modèle au démarrage
const checkModelUpdate = async () => {
  const currentVersion = await AsyncStorage.getItem('model_version');
  const response = await api.get('/ml/model/latest');
  
  if (response.data.version > currentVersion) {
    // Download nouveau modèle en background
    await downloadModel(response.data.url);
    await AsyncStorage.setItem('model_version', response.data.version);
    // Reload modèle au prochain démarrage
  }
};
```

---

## 5. User Experience (UX) Détaillée

### 5.1 Onboarding Flow

**Écran 1 : Bienvenue**
```
┌─────────────────────────────┐
│     [Logo InvoiceAI]        │
│                             │
│  Suivez vos dépenses        │
│  en scannant vos tickets    │
│                             │
│     [Illustration]          │
│                             │
│   [Commencer] [Se connecter]│
└─────────────────────────────┘
```

**Écran 2 : Tutoriel Interactif (3 slides)**
1. **Scan** : "Photographiez votre ticket"
2. **Classification** : "L'IA classe automatiquement"
3. **Analyse** : "Visualisez vos dépenses"

**Écran 3 : Permissions**
- ✅ Caméra (obligatoire)
- ✅ Stockage (obligatoire)
- ⚪ Notifications (optionnel)

### 5.2 Navigation Architecture

**Bottom Tab Navigation** :
```
┌─────────────────────────────┐
│       [Screen Content]      │
│                             │
└─────────────────────────────┘
  🏠    📸    📊    👤
  Home  Scan  Stats Profile
```

**Screens Hierarchy** :
```
AuthStack
├── Login
├── Register
└── ForgotPassword

MainStack
├── Home (Dashboard)
├── Scanner
│   ├── Camera
│   ├── Review
│   └── Edit
├── Invoices
│   ├── List
│   └── Detail
├── Analytics
│   ├── Overview
│   ├── CategoryDetail
│   └── Budgets
└── Profile
    ├── Settings
    ├── Export
    └── About
```

### 5.3 Micro-interactions Clés

**Scan Success Animation** :
```
Camera → Capture (haptic feedback) 
→ Processing spinner (2s max)
→ Checkmark animation + confetti
→ Slide up card avec résultats
```

**Classification Confidence Indicator** :
```
High (>90%) : 🟢 Badge vert
Medium (70-90%) : 🟡 Badge jaune + suggestion
Low (<70%) : 🔴 Badge rouge + "Vérifiez la catégorie"
```

**Budget Alert** :
```
Notification: "⚠️ Budget Alimentaire à 85%"
→ Tap → Ouvre Analytics avec drill-down
→ Affiche détails + suggestions économies
```

---

## 6. Stratégie de Déploiement et DevOps

### 6.1 CI/CD Pipeline

**Mobile (GitHub Actions)** :
```yaml
# .github/workflows/mobile-ci.yml
name: React Native CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  test:
    runs-on: macos-latest
    steps:
      - Checkout code
      - Setup Node.js 18
      - Install dependencies
      - Run ESLint
      - Run TypeScript check
      - Run Jest tests
      - Build Android APK
      - Build iOS (TestFlight)
  
  deploy:
    if: github.ref == 'refs/heads/main'
    needs: test
    steps:
      - Deploy to Google Play (Internal Testing)
      - Deploy to TestFlight
```

**Backend (GitLab CI)** :
```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy

build:
  stage: build
  script:
    - ./mvnw clean package
  artifacts:
    paths:
      - target/*.jar

test:
  stage: test
  script:
    - ./mvnw test
    - ./mvnw jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'

deploy-staging:
  stage: deploy
  script:
    - docker build -t invoice-api:$CI_COMMIT_SHA .
    - docker push registry.gitlab.com/project/invoice-api:$CI_COMMIT_SHA
    - kubectl set image deployment/invoice-api app=invoice-api:$CI_COMMIT_SHA
  only:
    - develop

deploy-prod:
  stage: deploy
  script:
    - # Production deployment
  only:
    - main
  when: manual
```

### 6.2 Infrastructure (Backend)

**Architecture Cloud (AWS)** :
```
┌──────────────────────────────────────────┐
│          CloudFront (CDN)                │
│          + S3 (Images tickets)           │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────▼───────────────────────┐
│     Application Load Balancer (ALB)     │
└──────────────────┬───────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
┌───────▼────────┐   ┌───────▼────────┐
│  ECS Fargate   │   │  ECS Fargate   │
│  Spring Boot   │   │  Spring Boot   │
│  (Instance 1)  │   │  (Instance 2)  │
└───────┬────────┘   └───────┬────────┘
        │                     │
        └──────────┬──────────┘
                   │
┌──────────────────▼───────────────────────┐
│          RDS PostgreSQL                  │
│          (Multi-AZ, auto-backup)         │
└──────────────────┬───────────────────────┘
                   │
┌──────────────────▼───────────────────────┐
│     ElastiCache Redis (Sessions)        │
└──────────────────────────────────────────┘
```

**Coûts Estimés (Démarrage)** :
- ECS Fargate (2 instances t3.small) : ~$60/mois
- RDS PostgreSQL (db.t3.micro) : ~$15/mois
- S3 + CloudFront : ~$10/mois
- ElastiCache (t3.micro) : ~$15/mois
**Total** : ~$100-120/mois

### 6.3 Monitoring et Observabilité

**Stack de Monitoring** :
- **APM** : New Relic ou Datadog
- **Logs** : CloudWatch Logs → Elasticsearch
- **Metrics** : Prometheus + Grafana
- **Alertes** : PagerDuty

**Dashboards Clés** :
1. **Backend Health**
   - Request latency (p50, p95, p99)
   - Error rate
   - Throughput (req/s)
   - Database connections pool

2. **Mobile Analytics**
   - Crash-free rate (Firebase Crashlytics)
   - Scan success rate
   - Classification accuracy (feedback rate)
   - Sync queue length

3. **Business Metrics**
   - DAU/MAU
   - Scans per user
   - Retention (D1, D7, D30)
   - Conversion rate (free → premium)

---

## 7. Monétisation et Business Model

### 7.1 Freemium Model

**Free Tier** :
- ✅ 50 scans/mois
- ✅ Classification automatique
- ✅ Visualisations basiques (camembert, liste)
- ✅ Historique 6 mois
- ✅ 1 device

**Premium Tier (4.99€/mois ou 49.99€/an)** :
- ✅ Scans illimités
- ✅ Analyses prédictives et insights IA
- ✅ Budgets et alertes personnalisés
- ✅ Export CSV/PDF/Excel
- ✅ Historique illimité
- ✅ Multi-device sync
- ✅ Support prioritaire

**Business Tier (14.99€/mois)** :
- ✅ Tout Premium +
- ✅ Multi-utilisateurs (équipe 5 personnes)
- ✅ Catégories personnalisées
- ✅ API access
- ✅ Intégrations comptables (Quickbooks, Sage)
- ✅ Dashboard web

### 7.2 Projections Financières (Année 1)

**Hypothèses** :
- Lancement : M3
- Acquisition : 50 users/mois (M3-M6), 200/mois (M7-M12)
- Conversion Premium : 5% (conservateur)
- Churn : 10%/mois

**Revenus Projetés (M12)** :
- Users totaux : ~1,500
- Premium users : ~75
- MRR : 75 × 4.99€ = 374€
- ARR : ~4,500€

**Coûts Mensuels** :
- Infrastructure : 120€
- Tools (Firebase, monitoring) : 50€
- Marketing : 500€ (Google/Meta Ads)
**Total** : 670€/mois

**Break-even** : ~135 premium users (M18-M24)

### 7.3 Stratégie Acquisition

**Canaux** :
1. **SEO/Content Marketing** : Blog (gestion budget, astuces épargne)
2. **App Store Optimization (ASO)** : Keywords optimization
3. **Social Media** : TikTok/Instagram (tutoriels courts)
4. **Partnerships** : Blogs finance personnelle, influenceurs
5. **Referral Program** : 1 mois premium offert/parrainage

---

## 8. Roadmap Détaillée

### 8.1 Phase 1 : MVP (M1-M4)

**M1 : Fondations**
- ✅ Setup projets (React Native + Spring Boot)
- ✅ Architecture CI/CD
- ✅ Authentification JWT
- ✅ Base de données (SQLite mobile, PostgreSQL backend)

**M2 : Core Features**
- ✅ Capture photo + OCR (ML Kit)
- ✅ Parsing factures basique
- ✅ CRUD invoices/items
- ✅ Sync offline-first

**M3 : Intelligence**
- ✅ Classification ML (règles + TFLite)
- ✅ Feedback loop
- ✅ Visualisation camembert

**M4 : Polish & Launch**
- ✅ UX/UI refinement
- ✅ Tests utilisateurs (20 beta testers)
- ✅ Security audit
- ✅ **🚀 Lancement App Stores**

### 8.2 Phase 2 : Growth (M5-M8)

**M5 : Analytics Avancées**
- 📊 Détection anomalies
- 📈 Prédictions budgétaires
- 🎯 Système de budgets

**M6 : Premium Features**
- 💎 Paywall implémentation (RevenueCat)
- 📤 Export données (CSV/PDF)
- 🔄 Multi-device sync

**M7 : Engagement**
- 🔔 Notifications intelligentes
- 🏆 Gamification (badges, challenges)
- 📧 Email reports hebdomadaires

**M8 : Optimisation**
- ⚡ Performance tuning
- 🤖 Amélioration modèles ML (accuracy +5%)
- 🌍 Support langues additionnelles (EN, ES)

### 8.3 Phase 3 : Scale (M9-M12)

**M9 : Intégrations**
- 🏦 Open Banking (connexion comptes bancaires)
- 📱 Widget iOS/Android
- 💻 Web dashboard (React)

**M10 : Business Features**
- 👥 Multi-utilisateurs
- 🔌 API publique
- 🧾 Intégrations comptables

**M11 : IA Avancée**
- 🧠 Recommandations personnalisées
- 🗣️ Assistant vocal (scan par voix)
- 📷 Scan automatique (détection ticket dans galerie)

**M12 : Expansion**
- 🌍 Lancement internationaux (UK, DE, IT)
- 📱 Version tablette
- 🤝 Partnerships stratégiques

---

## 9. Risques et Mitigation

### 9.1 Risques Techniques

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| **Précision OCR insuffisante** | Élevé | Moyen | Fallback saisie manuelle, amélioration preprocessing, tests sur 1000+ tickets réels |
| **Classification imprécise** | Moyen | Moyen | Approche hybride règles+ML, feedback loop agressif, baseline 75% acceptable |
| **Performances mobile faibles** | Élevé | Faible | Profiling continu, optimisation TFLite, tests sur devices entrée de gamme |
| **Problèmes synchronisation** | Moyen | Moyen | Queue retry robuste, tests réseau instable, logs détaillés |
| **Complexité maintenance ML** | Moyen | Élevé | Documentation pipeline, automatisation max, versioning modèles |

### 9.2 Risques Produit

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| **Faible adoption** | Élevé | Moyen | Landing page + waitlist pré-lancement, beta test 100 users, itérations rapides |
| **Churn élevé** | Élevé | Moyen | Onboarding soigné, notifications engagement, feature discovery in-app |
| **Concurrence** | Moyen | Élevé | Focus offline-first + IA embarquée (différenciateur), vitesse d'exécution |
| **Conversion premium faible** | Moyen | Moyen | A/B testing paywall, valeur premium claire, trial gratuit 14j |

### 9.3 Risques Légaux/Sécurité

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| **Non-conformité RGPD** | Critique | Faible | Audit RGPD par expert, DPO externe, privacy by design |
| **Breach données** | Critique | Faible | Chiffrement E2E, pen-testing annuel, bug bounty program |
| **Propriété intellectuelle** | Moyen | Faible | Audit brevets, datasets open-source, CGU claires |

---

## 10. Métriques de Succès (KPIs)

### 10.1 Acquisition

- **Downloads** : 5,000 (M6), 15,000 (M12)
- **Coût acquisition (CAC)** : < 5€
- **Conversion install → signup** : > 40%

### 10.2 Engagement

- **DAU/MAU ratio** : > 25% (M12)
- **Scans per user/month** : > 8 (M6), > 15 (M12)
- **Retention D1/D7/D30** : 60%/30%/15% (M12)
- **Session duration** : > 3 min

### 10.3 Monétisation

- **Conversion free → premium** : > 5% (M12)
- **Churn premium** : < 10%/mois
- **LTV/CAC ratio** : > 3 (M18)

### 10.4 Technique

- **OCR accuracy** : > 90% (M6), > 92% (M12)
- **Classification accuracy** : > 80% (M6), > 85% (M12)
- **Crash-free rate** : > 99.5%
- **API latency p95** : < 500ms
- **Sync success rate** : > 98%

### 10.5 Qualité

- **App Store rating** : > 4.5/5
- **NPS Score** : > 50
- **Support tickets/user** : < 0.1

---

## 11. Équipe et Organisation

### 11.1 Composition Équipe (Phase MVP)

**Core Team (4 personnes)** :

1. **Tech Lead / Backend** (1 FTE)
   - Architecture Spring Boot
   - API REST, sécurité
   - Pipeline ML backend
   - DevOps/Infrastructure

2. **Mobile Developer** (1 FTE)
   - React Native
   - Intégration OCR/ML
   - Sync offline
   - Performance optimization

3. **ML Engineer** (0.5 FTE)
   - Training modèles classification
   - Pipeline MLOps
   - Optimisation TFLite
   - A/B testing modèles

4. **Product Designer** (0.5 FTE)
   - UX/UI design
   - User research
   - Prototyping
   - Design system

**Support externe** :
- Legal (RGPD consultant) : Ponctuel
- Marketing : Freelance (phase lancement)

### 11.2 Méthodologie

**Agile Scrum** :
- Sprints : 2 semaines
- Daily standups : 15 min
- Sprint planning : Lundi matin
- Retrospective : Vendredi PM
- Tools : Jira + Confluence + Slack

**Rituals** :
- Demo stakeholders : Bi-mensuel
- Tech debt review : Mensuel
- User testing : Bi-mensuel (5-10 users)

---

## 12. Annexes

### 12.1 Glossaire Technique

- **TFLite** : TensorFlow Lite, version optimisée pour mobile
- **OCR** : Optical Character Recognition
- **JWT** : JSON Web Token, standard authentification
- **RGPD** : Règlement Général sur la Protection des Données
- **MLOps** : Machine Learning Operations, automatisation ML
- **E2E** : End-to-End, chiffrement bout en bout
- **CAC** : Customer Acquisition Cost
- **LTV** : Lifetime Value

### 12.2 Références et Inspirations

**Apps Concurrentes** :
- Expensify (focus pro, lourd)
- Fintect (France, pas d'IA)
- Toshl Finance (UX excellente)

**Différenciation** :
- ✅ Offline-first (unique)
- ✅ IA embarquée (pas cloud-only)
- ✅ Gratuit sans limite fonctionnelle core

### 12.3 Ressources Utiles

**Documentation** :
- React Native : https://reactnative.dev
- TensorFlow Lite : https://tensorflow.org/lite
- Spring Boot : https://spring.io/projects/spring-boot
- ML Kit : https://developers.google.com/ml-kit

**Datasets ML** :
- OpenFoodFacts : https://world.openfoodfacts.org
- Google Product Taxonomy : https://support.google.com/merchants

**Communities** :
- r/reactnative (Reddit)
- TensorFlow Forum
- Spring Community

---

## 13. Validation et Approbation

### 13.1 Critères de Validation MVP

**Avant lancement App Stores** :
- [ ] ✅ 20 beta testers complètent onboarding
- [ ] ✅ 80% testeurs scannent >3 tickets
- [ ] ✅ OCR accuracy >85% sur 100 tickets test
- [ ] ✅ Classification accuracy >75%
- [ ] ✅ 0 crash critique en 1 semaine testing
- [ ] ✅ Security audit passé (OWASP Top 10)
- [ ] ✅ RGPD compliance validée
- [ ] ✅ App Store guidelines respectées

### 13.2 Go/No-Go Decision Points

**M3 (Pre-Launch)** :
- Si accuracy OCR < 80% → Delay 1 mois
- Si crash rate > 5% → Delay 2 semaines
- Si beta testers NPS < 30 → Pivot features

**M6 (Post-Launch)** :
- Si retention D30 < 10% → Revue UX majeure
- Si downloads < 1000 → Intensifier marketing
- Si CAC > 10€ → Optimiser acquisition

**M12 (Scale Decision)** :
- Si Premium users < 50 → Revoir pricing/value
- Si LTV/CAC < 2 → Optimiser retention
- Si rating < 4.0 → Focus qualité

---

## 14. Conclusion et Prochaines Étapes

### 14.1 Synthèse

Ce PRD définit une **application mobile intelligente de gestion de dépenses** avec :
- 🎯 **Vision claire** : Simplifier suivi dépenses via IA
- 💡 **Innovation** : Offline-first + ML embarqué
- 🚀 **Faisabilité** : Stack mature, roadmap réaliste
- 💰 **Viabilité** : Business model freemium éprouvé

### 14.2 Actions Immédiates (Next 2 weeks)

**Semaine 1** :
1. ✅ Validation stakeholders sur PRD
2. ✅ Setup repos GitHub (mobile + backend)
3. ✅ Création designs mockups Figma (5 écrans clés)
4. ✅ Provisionner infrastructure AWS (staging)
5. ✅ Kick-off meeting équipe

**Semaine 2** :
1. ✅ Développement authentification backend
2. ✅ Setup React Native project + navigation
3. ✅ POC OCR avec ML Kit (10 tickets test)
4. ✅ Création dataset initial classification (500 items)
5. ✅ Sprint planning M1

### 14.3 Points de Décision Critiques

**Choix technologiques à valider** :
- [ ] OCR : ML Kit vs PaddleOCR (recommandation : ML Kit)
- [ ] State management : Zustand vs Redux Toolkit
- [ ] Backend ML : Java vs Python microservice
- [ ] Infrastructure : AWS vs Google Cloud

**Contacts et Ressources** :
- Product Owner : [À définir]
- Tech Lead : [À définir]
- Budget Phase 1 : 50-70k€ (4 FTE × 4 mois)
- Slack channel : #invoice-ai
- Jira board : INVOICE-*

---

**Version** : 2.0  
**Dernière mise à jour** : 13 novembre 2025  
**Auteur** : Équipe Produit InvoiceAI  
**Statut** : ✅ Prêt pour validation stakeholders

---

*Ce document est vivant et sera mis à jour régulièrement selon les apprentissages et feedback utilisateurs.*