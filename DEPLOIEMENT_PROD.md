# Déploiement production — InvoiceAI

Guide pour héberger l’API Spring Boot + PostgreSQL avant la beta fermée.

## Prérequis

- Serveur Linux (VPS) avec Docker et Docker Compose v2
- Nom de domaine pointant vers le serveur (ex. `api.votredomaine.com`)
- Certificat TLS (Let's Encrypt recommandé)

## 1. Configuration

```bash
cp deploy/.env.prod.example deploy/.env.prod
# Éditer deploy/.env.prod : mots de passe, JWT_SECRET, APP_UPLOADS_PUBLIC_BASE_URL
```

`APP_UPLOADS_PUBLIC_BASE_URL` doit être l’URL **HTTPS** publique de l’API (ex. `https://api.votredomaine.com`). Les tickets synchronisés renverront des liens du type `https://api.votredomaine.com/uploads/invoices/...`.

## 2. Lancer la stack Docker

Depuis la racine du monorepo :

```bash
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build
```

Vérifier :

```bash
curl -s https://api.votredomaine.com/actuator/health
# ou http://IP:8085/actuator/health avant HTTPS
```

Sous Windows (API locale ou tunnel) :

```powershell
.\deploy\validate-prod.ps1 -BaseUrl "http://localhost:8085"
```

## 3. HTTPS (nginx)

1. Installer nginx + certbot sur le VPS.
2. Copier et adapter `deploy/nginx.conf.example` vers `/etc/nginx/sites-available/invoiceai`.
3. `nginx -t && systemctl reload nginx`
4. `certbot --nginx -d api.votredomaine.com`

L’API écoute en local sur le port `API_PORT` (8085 par défaut).

## 4. Variables Spring (profil `prod`)

| Variable | Obligatoire | Description |
|----------|-------------|-------------|
| `SPRING_DATASOURCE_URL` | Oui | JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Oui | |
| `SPRING_DATASOURCE_PASSWORD` | Oui | |
| `JWT_SECRET` | Oui | ≥ 32 caractères |
| `APP_UPLOADS_PUBLIC_BASE_URL` | Oui | URL HTTPS des images |
| `APP_UPLOADS_DIR` | Non | Défaut `/data/uploads` en Docker |
| `OCR_AI_*` | Non | Analyse IA serveur |

## 5. Mobile

Mettre à jour `InvoiceAI/src/config/apiConfig.ts` :

```typescript
export const PRODUCTION_API_URL = 'https://api.votredomaine.com';
```

Puis build beta : voir `InvoiceAI/BETA_PACKAGE.md`.

## 6. Sauvegardes

- Volume Docker `postgres_prod_data` : sauvegarder PostgreSQL régulièrement (`pg_dump`).
- Volume `uploads_prod_data` : sauvegarder les photos de tickets.

## 7. Mise à jour

```bash
git pull
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d --build
```
