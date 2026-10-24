# Shortly — Full-Stack URL Shortener

A full-stack URL shortener built with **Java, Spring Boot, PostgreSQL, Redis, React, and TypeScript**. Create shareable short links, redirect visitors with HTTP 302, track click counts, and manage links through a responsive dashboard.

**[Live Demo](https://url-shortener-frontend-five-gray.vercel.app/) · [Backend API](https://url-shortener-g8iv.onrender.com/) · [API Health](https://url-shortener-g8iv.onrender.com/actuator/health) · [GitHub Repository](https://github.com/MahdiNoorzadeh/url-shortener)**

## Features

- **URL shortening:** Generate compact short codes for long URLs, with optional expiration times.
- **HTTP redirects:** Resolve short codes to their original destinations using HTTP 302 redirects.
- **Persistent storage:** Store URL records and click counts in PostgreSQL.
- **Redis caching:** Cache redirect lookups using a cache-aside strategy to reduce repeated database queries.
- **Expiration handling:** Support expiring links and Redis cache TTLs.
- **Click statistics:** Retrieve the original URL, creation time, expiration time, and aggregate click count.
- **Browser dashboard:** Search, filter, rename, copy, open, and locally remove saved links.
- **Import and export:** Transfer dashboard data through validated JSON files.
- **Resilient frontend:** Handle API errors, loading states, timeouts, and slow service startup.
- **Automated validation:** Backend and frontend checks run through GitHub Actions.

## Live deployment

| Component | Technology | Deployment |
|---|---|---|
| Frontend | React, TypeScript, Vite | [Vercel](https://url-shortener-frontend-five-gray.vercel.app/) |
| Backend API | Java, Spring Boot | [Render](https://url-shortener-g8iv.onrender.com/) |
| Relational database | PostgreSQL | Neon |
| Cache | Redis | Upstash |
| CI | GitHub Actions | [Workflow configuration](https://github.com/MahdiNoorzadeh/url-shortener/tree/main/.github/workflows) |

The frontend communicates with the deployed backend through a configurable API base URL. PostgreSQL stores the durable URL records, while Redis accelerates redirect lookups.

> **Hosting note:** The backend runs on a free hosting tier and may take longer to respond when waking from inactivity.

## Architecture

```text
┌───────────────────────────────┐
│ Browser                       │
│ React + TypeScript + Vite     │
│                               │
│ URL form and dashboard        │
│ Dashboard metadata:           │
│ browser localStorage          │
└───────────────┬───────────────┘
                │ HTTPS / REST
                ▼
┌───────────────────────────────┐
│ Spring Boot REST API          │
│                               │
│ Create short URLs              │
│ Resolve redirects              │
│ Track click counts             │
│ Retrieve URL statistics        │
└──────────┬───────────┬────────┘
           │           │
           ▼           ▼
┌────────────────┐ ┌────────────────┐
│ PostgreSQL     │ │ Redis          │
│ Durable URL    │ │ Cached URL     │
│ records        │ │ mappings       │
└────────────────┘ └────────────────┘
```

### Redirect caching flow

1. A visitor opens a short URL.
2. The backend checks Redis for the short-code mapping.
3. On a cache hit, the cached destination is used.
4. On a cache miss, the backend looks up the URL record in PostgreSQL and caches the mapping when applicable.
5. The backend returns an HTTP 302 redirect and tracks the click according to the application logic.

PostgreSQL remains the durable source of URL records; Redis is used as a cache rather than the primary datastore.

## Technology stack

**Backend**
- Java 25
- Spring Boot
- Spring Data JPA
- Spring Data Redis
- PostgreSQL
- Redis
- Flyway database migrations
- Gradle

**Frontend**
- React 19
- TypeScript
- Vite
- Lucide React
- Vitest
- ESLint

**Testing and infrastructure**
- JUnit and Mockito
- Testcontainers
- Docker and Docker Compose
- GitHub Actions
- Render, Neon, Upstash, and Vercel

## API reference

Base URL: `https://url-shortener-g8iv.onrender.com`

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/urls` | Create a short URL |
| `GET` | `/{shortCode}` | Redirect to the original URL |
| `GET` | `/api/v1/urls/{shortCode}/stats` | Retrieve URL statistics |
| `GET` | `/actuator/health` | Check backend health |

### Create a short URL

Request:

```http
POST /api/v1/urls
Content-Type: application/json
```

```json
{
  "url": "https://example.com",
  "expiresAt": "2030-01-01T10:00:00Z"
}
```

The `expiresAt` field is optional.

Example response:

```json
{
  "shortCode": "f2fd9d7",
  "shortUrl": "https://url-shortener-g8iv.onrender.com/f2fd9d7",
  "expiresAt": null
}
```

The response above illustrates the response shape; generated short codes and expiration values depend on the request.

### Redirect

```http
GET /{shortCode}
```

A valid short code redirects to its destination with HTTP `302 Found`. Missing or expired links are handled by the backend's error handling.

### Retrieve statistics

```http
GET /api/v1/urls/{shortCode}/stats
```

Example response:

```json
{
  "shortCode": "f2fd9d7",
  "originalUrl": "https://example.com",
  "clickCount": 1,
  "createdAt": "2026-10-24T12:00:00Z",
  "expiresAt": null
}
```

Values are illustrative. The API returns the actual record and aggregate click count.

## Run locally

### Prerequisites

- Java 25
- Node.js 22 or a compatible newer version
- npm
- Docker Desktop with Docker Compose

### 1. Clone the repository

```powershell
git clone https://github.com/MahdiNoorzadeh/url-shortener.git
Set-Location url-shortener
```

### 2. Start PostgreSQL and Redis

From the repository root:

```powershell
docker compose up -d postgres redis
```

Configure any required local environment variables using the project's existing configuration. Never commit credentials or production secrets.

### 3. Start the backend

```powershell
.\gradlew.bat bootRun
```

The backend defaults to `http://localhost:8080`.

- Health: `http://localhost:8080/actuator/health`
- Swagger UI, if enabled: `http://localhost:8080/swagger-ui/index.html`

### 4. Configure and start the frontend

Open a second terminal:

```powershell
Set-Location frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

Ensure `frontend/.env.local` contains:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Open the local Vite address printed in the terminal, normally `http://localhost:5173`.

The API base URL must be the backend origin only; do not append `/api/v1/urls`.

### 5. Run validation

Backend, from the repository root:

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
```

Frontend, from `frontend/`:

```powershell
npm run lint
npm test
npm run build
```

Backend integration tests use Testcontainers and require Docker to be available.

## Deployment configuration

### Backend — Render

Configure the backend's environment variables using the database and cache provider connection details:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_USERNAME`
- `REDIS_PASSWORD`
- `REDIS_SSL_ENABLED`
- `PUBLIC_BASE_URL`
- `CORS_ALLOWED_ORIGINS`

Set `PUBLIC_BASE_URL` to the public Render backend origin. Set `CORS_ALLOWED_ORIGINS` to the permitted frontend origins, including the production Vercel origin.

Never commit production credentials.

### Frontend — Vercel

Configure the project with:

| Setting | Value |
|---|---|
| Root directory | `frontend` |
| Framework | Vite |
| Build command | `npm run build` |
| Output directory | `dist` |
| Environment variable | `VITE_API_BASE_URL` |
| Production API value | `https://url-shortener-g8iv.onrender.com` |

Vite environment variables prefixed with `VITE_` are exposed to client-side code and must not contain secrets.

## Design decisions and limitations

- **Cache-aside:** Redis serves cached redirect mappings; PostgreSQL stores durable URL records.
- **Browser-local dashboard:** Saved dashboard entries and display names are stored in the current browser's local storage.
- **No account system:** Authentication and cross-device dashboard synchronization are not implemented.
- **Local removal:** Removing a link from the dashboard does not delete the server-side URL record.
- **Manual transfer:** JSON import/export allows dashboard data to be transferred manually; it does not provide synchronization.
- **Aggregate analytics:** Statistics provide a total click count, not a per-day or per-visitor analytics breakdown.
- **Free-tier latency:** Hosting services may sleep after inactivity, increasing initial response times.

## License

No license has been specified for this repository. Add a `LICENSE` file if you intend to distribute the project under a particular open-source license.
