# Comment démarrer le backend

## Ports utilisés

| Service    | Port | Remarque                                      |
|------------|------|-----------------------------------------------|
| Backend    | **8085** | 8080 est souvent occupé par Docker          |
| PostgreSQL | **5435** | mappé depuis 5432 dans le conteneur Docker  |

## Prérequis

1. **Java 17+** et **Maven** (ou `mvnw.cmd` inclus)
2. **PostgreSQL** accessible (Docker recommandé, voir ci-dessous)
3. **Fichiers secrets locaux** configurés (obligatoire)

## Configuration des secrets (première fois)

Les mots de passe et clés JWT ne sont **plus** dans les fichiers versionnés. Créez vos fichiers locaux à partir des exemples :

```powershell
cd invoice-ai-backend

copy .env.example .env
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
```

Éditez ensuite :

- **`.env`** — mot de passe PostgreSQL pour Docker Compose (`POSTGRES_PASSWORD`)
- **`application-local.yml`** — même mot de passe + clé JWT (≥ 32 caractères)

Ces fichiers sont listés dans `.gitignore` et ne doivent **jamais** être commités.

> **Production** : définir `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` et `SERVER_PORT` via variables d'environnement (profil `prod`).

## Démarrer PostgreSQL (Docker — recommandé)

```powershell
cd invoice-ai-backend
docker compose up -d
```

Le conteneur `invoiceai-postgres` expose PostgreSQL sur `localhost:5435`.  
La base `invoiceai_db` est créée automatiquement si `.env` est configuré.

Vérifier que le conteneur tourne :

```powershell
docker ps --filter name=invoiceai-postgres
```

## Démarrer le backend

### Option 1 : Script racine (backend + mobile)

Depuis la racine du monorepo :

```powershell
.\start-app.ps1 -BackendOnly
```

### Option 2 : Maven Wrapper

```powershell
cd invoice-ai-backend
.\mvnw.cmd spring-boot:run
```

### Option 3 : Maven

```powershell
cd invoice-ai-backend
mvn spring-boot:run
```

### Option 4 : IDE (IntelliJ, Eclipse)

1. Ouvrir le projet `invoice-ai-backend`
2. Lancer la classe `InvoiceAiApplication.java`

## Vérifier que le backend est démarré

Console attendue :

```
Started InvoiceAiApplication in X.XXX seconds
```

URLs :

- Backend : `http://localhost:8085`
- Swagger UI : `http://localhost:8085/swagger-ui.html`
- Health : `http://localhost:8085/actuator/health`
- Réseau local (app mobile) : `http://<IP_LAN>:8085`

### Appareil Android (USB)

Si l'app mobile est connectée en USB :

```powershell
adb reverse tcp:8085 tcp:8085
adb reverse tcp:8081 tcp:8081
```

## Tester l'API

```powershell
# Health check
curl http://localhost:8085/actuator/health

# Swagger (navigateur)
start http://localhost:8085/swagger-ui.html
```

## Problèmes courants

### Erreur : Port 8085 déjà utilisé

```powershell
netstat -ano | findstr :8085
taskkill /PID <PID> /F
```

### Erreur : `POSTGRES_PASSWORD` manquant (Docker)

Créez `.env` à partir de `.env.example` et définissez `POSTGRES_PASSWORD`.

### Erreur : Connexion à PostgreSQL échouée

- Vérifier que PostgreSQL écoute sur le port **5435** (`docker compose ps`)
- Vérifier que le mot de passe dans `application-local.yml` correspond à `.env`
- URL par défaut : `jdbc:postgresql://localhost:5435/invoiceai_db`

### Erreur : JWT / datasource au démarrage

- Vérifier que `application-local.yml` existe (copié depuis `.example`)
- Vérifier que `jwt.secret` fait au moins 32 caractères

### Erreur : Flyway migration failed

- Base incompatible avec les migrations existantes
- Réinitialiser (⚠️ supprime toutes les données) :

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

### App mobile : Network Error au login

- Backend démarré sur le port 8085
- `adb reverse tcp:8085 tcp:8085` exécuté
- IP correcte dans `InvoiceAI/src/utils/constants.ts` (appareil physique)
