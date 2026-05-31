# Configuration PostgreSQL pour InvoiceAI Backend

## Vue d'ensemble

| Paramètre        | Valeur par défaut                                      |
|------------------|--------------------------------------------------------|
| Base de données  | `invoiceai_db`                                         |
| Utilisateur      | `postgres`                                             |
| Port PostgreSQL  | **5435** (Docker) — évite les conflits avec un PostgreSQL local sur 5432 |
| Port backend     | **8085**                                               |
| Mot de passe     | Fichiers locaux `.env` + `application-local.yml` (non versionnés) |

## Secrets locaux (obligatoire)

Les identifiants ne doivent **pas** être modifiés dans `application.yml`. Configurez-les une fois :

```powershell
cd invoice-ai-backend

copy .env.example .env
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
```

Éditez les deux fichiers :

- **`.env`** → `POSTGRES_PASSWORD` (utilisé par Docker Compose)
- **`application-local.yml`** → `spring.datasource.password` (même valeur) + `jwt.secret`

Les mots de passe dans `.env` et `application-local.yml` **doivent correspondre**.

---

## Option 1 : Docker (recommandé)

Le fichier `docker-compose.yml` est déjà présent dans `invoice-ai-backend/`.

```powershell
cd invoice-ai-backend

# 1. Configurer .env (voir section ci-dessus)
# 2. Démarrer PostgreSQL
docker compose up -d
```

Vérifications :

```powershell
docker ps --filter name=invoiceai-postgres
docker compose logs postgres
```

Connexion depuis l'hôte :

- Hôte : `localhost`
- Port : **5435**
- Base : `invoiceai_db`
- Utilisateur / mot de passe : définis dans `.env`

URL JDBC utilisée par le backend :

```
jdbc:postgresql://localhost:5435/invoiceai_db
```

---

## Option 2 : PostgreSQL installé localement (sans Docker)

Si PostgreSQL tourne déjà sur votre machine (souvent port **5432**), deux possibilités :

### A. Changer le port côté backend

Définissez l'URL JDBC sans modifier les fichiers versionnés :

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/invoiceai_db"
mvn spring-boot:run
```

Ou ajoutez dans `application-local.yml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/invoiceai_db
    password: VOTRE_MOT_DE_PASSE
```

### B. Créer la base de données

```powershell
$env:PGPASSWORD = 'VOTRE_MOT_DE_PASSE'

psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE invoiceai_db;"
psql -U postgres -h localhost -p 5432 -c "\l" | Select-String "invoiceai"
```

Démarrer le service Windows si nécessaire :

```powershell
# En tant qu'administrateur — adapter le nom du service
Start-Service postgresql-x64-15
```

---

## Option 3 : H2 en mémoire (tests rapides uniquement)

Pour un essai sans PostgreSQL, modifiez **`application-local.yml`** (pas `application.yml`) :

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:invoiceai_db
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
      path: /h2-console
  flyway:
    enabled: false

jwt:
  secret: CHANGE_ME_JWT_SECRET_MIN_32_CHARS
```

Ajoutez la dépendance H2 dans `pom.xml` si elle n'y est pas déjà :

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

> Les données sont perdues à chaque redémarrage. Ne pas utiliser en production.

---

## Démarrer le backend après configuration

```powershell
cd invoice-ai-backend
mvn spring-boot:run
```

Contrôles :

- Log : `Started InvoiceAiApplication`
- Health : http://localhost:8085/actuator/health
- Swagger : http://localhost:8085/swagger-ui.html

Voir aussi : [DEMARRER_BACKEND.md](./DEMARRER_BACKEND.md)

---

## Dépannage

### Connexion refusée sur le port 5435

- Docker non démarré : `docker compose up -d`
- `.env` manquant ou sans `POSTGRES_PASSWORD`
- Conteneur arrêté : `docker compose ps`

### `FATAL: password authentication failed`

- Mot de passe différent entre `.env` et `application-local.yml`
- Conteneur créé avec un ancien mot de passe : supprimer le volume et recréer

```powershell
docker compose down -v
docker compose up -d
```

> ⚠️ `down -v` supprime toutes les données PostgreSQL du conteneur.

### Backend démarre mais Flyway échoue

Schéma incompatible avec les migrations. Réinitialiser la base :

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

### Changer le mot de passe PostgreSQL

1. Mettre à jour `.env` et `application-local.yml`
2. Si Docker avec ancien volume : `docker compose down -v` puis `docker compose up -d`
3. Si le dépôt a déjà été poussé avec d'anciens secrets, **rotater** le mot de passe (ne pas réutiliser l'ancien)
