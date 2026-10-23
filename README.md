# Shortly — URL Shortener

A URL-shortening service with a Spring Boot REST API and a React + TypeScript frontend. Short-link records live in PostgreSQL, Redis caches redirects, and the personal dashboard is stored locally in the current browser.

## Features

- Create short URLs with optional expiration.
- HTTP 302 redirects, PostgreSQL persistence, Redis cache-aside, and click counts.
- URL statistics through the existing API.
- Responsive browser-local dashboard with display names, search, active/expired filters, copy/open, rename, and local removal.
- Versioned dashboard storage and validated JSON import/export. Import replaces the current local dashboard after confirmation.
- Accessible loading and notification states, including delayed feedback for a sleeping free-host backend.
- GitHub Actions CI for backend and frontend.

**Privacy and persistence:** The server stores shortened URL records. The dashboard's names and list are stored only in this browser's local storage. Clearing browser data or switching devices does not transfer dashboard data. Removing an item from the dashboard does not delete its server-side URL. Import/export is a manual transfer mechanism, not synchronization.

## Architecture

```text
Browser (React + TypeScript + Vite)
  ├── POST /api/v1/urls ───────────────┐
  ├── GET  /api/v1/urls/{code}/stats ──┤
  └── localStorage (dashboard metadata)│
                                       ▼
                              Spring Boot REST API
                               ├── PostgreSQL (durable records)
                               └── Redis (redirect cache)
```

Detailed implementation notes and file-by-file responsibilities are maintained locally in `PROJECT_STRUCTURE.local.md`; it is intentionally excluded from version control.

## Prerequisites

- Java 25
- Docker Desktop (for PostgreSQL, Redis, and Testcontainers)
- Node.js 22 LTS or newer compatible with the Vite toolchain
- npm

## Run locally

### 1. Start the database and cache

From the repository root:

```powershell
docker compose up -d postgres redis
```

If your Compose file requires environment variables, create a local `.env` based on the configuration you already use. Never commit `.env` or credentials.

### 2. Start the backend

In a terminal at the repository root:

```powershell
.\gradlew.bat bootRun
```

The API defaults to `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`, and health at `http://localhost:8080/actuator/health`.

### 3. Start the frontend

In a second terminal:

```powershell
cd frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

Open the Vite URL printed in the terminal, normally `http://localhost:5173`. `VITE_API_BASE_URL` is the backend origin only, without `/api/v1/urls`. Vite environment variables are public client configuration; never put secrets in them.

## Validation

Backend, from the root:

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
```

Frontend, from `frontend/`:

```powershell
npm install
npm run lint
npm test
npm run build
```

The backend integration tests use Testcontainers, so Docker must be available. The frontend tests cover browser dashboard storage validation and export/import behavior.

## API contract

- `POST /api/v1/urls` — JSON body `{ "url": "https://example.com", "expiresAt": "2030-01-01T10:00:00Z" }`; `expiresAt` is optional. Returns `shortCode`, `shortUrl`, and optional `expiresAt`.
- `GET /{shortCode}` — redirects to the destination with HTTP 302; not-found and expired links are reported by the backend's error handling.
- `GET /api/v1/urls/{shortCode}/stats` — returns `shortCode`, `originalUrl`, `clickCount`, `createdAt`, and optional `expiresAt`.
- `GET /actuator/health` — health check.

## Deployment readiness: Render API + Vercel frontend

No deployment is performed by this repository's CI workflow. GitHub Actions validates code; Vercel and Render GitHub integrations deploy independently according to their own settings.

### Render backend

1. Create a Render Web Service from this repository.
2. Set the root directory to the repository root.
3. Use the project's Dockerfile if deploying the existing container setup, or configure a Gradle/Java build with the actual project commands.
4. Provision persistent PostgreSQL and Redis services. Do not store durable URL records in an ephemeral filesystem. Configure the service to reach those services over their provider URLs.
5. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, and `REDIS_PORT` using the provider's connection details.
6. Set `PUBLIC_BASE_URL` to the public backend origin, for example `https://your-api.onrender.com` (no trailing slash required).
7. Set `CORS_ALLOWED_ORIGINS` to the exact Vercel production origin, for example `https://your-app.vercel.app`. For local development, the default is `http://localhost:5173`. Multiple origins may be comma-separated.
8. Restrict production deploys to `main` in Render's settings. Confirm health at `/actuator/health` after deployment.

### Vercel frontend

1. Import this GitHub repository into Vercel.
2. Set the project Root Directory to `frontend`.
3. Framework preset: Vite. Build command: `npm run build`; output directory: `dist`; install command: `npm install`.
4. Add `VITE_API_BASE_URL` with the public backend origin, e.g. `https://your-api.onrender.com`. This is public configuration, not a secret.
5. Use Vercel preview deployments for pull requests if useful. Restrict production deployments to `main`.
6. Update Render's `CORS_ALLOWED_ORIGINS` to the actual production frontend origin. Add preview origins only if you deliberately want preview builds to call the API.

### CI and merge protection

The workflow runs on pull requests targeting `main` and pushes to `main`. It runs backend `./gradlew clean build` and frontend `npm install`, `npm run lint`, `npm test`, and `npm run build`. In GitHub repository settings, configure branch protection/rulesets for `main` and require the `Backend tests and build` and `Frontend lint, tests, and build` checks before merging if that feature is available on your plan.

## Known limitations

- Dashboard state is browser-local; no login, cross-device sync, or server-side rename/delete is implemented.
- The backend statistics endpoint returns current aggregate click count, not per-day analytics.
- The backend must be deployed with persistent PostgreSQL and a reachable Redis instance.
- Free hosting may sleep. The frontend displays a delayed “service may be waking up” status and a timeout message; it never automatically retries link-creation POST requests.
