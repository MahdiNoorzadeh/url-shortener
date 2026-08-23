# 🔗 URL Shortener

A production-oriented **URL Shortener REST API** built with **Spring Boot** and **Java 25**.

---

## ✨ Features

- ✂️ Create short URLs
- 🔄 **HTTP 302** redirects
- 🐘 PostgreSQL persistence
- ⚡ Redis caching
- 📦 Cache-Aside pattern
- ⏳ URL expiration
- ⏱️ Redis **TTL** based on URL expiration
- 🖱️ Click counter
- 📊 URL statistics
- 🛡️ Global exception handling
- ✅ Request validation
- 📖 OpenAPI / Swagger documentation
- 🐳 Docker & Docker Compose
- ❤️ Health checks
- 🧪 Unit and integration tests
- 📦 Testcontainers
- 🤖 GitHub Actions CI

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot |
| Database | PostgreSQL 17 |
| Cache | Redis 7 |
| Persistence | Spring Data JPA |
| Caching | Spring Data Redis |
| Migrations | Flyway |
| Build Tool | Gradle |
| Containerization | Docker & Docker Compose |
| Testing | JUnit, Mockito, Testcontainers |
| API Documentation | OpenAPI / Swagger |

---

## 🏗️ Architecture

The application follows a **Cache-Aside** caching strategy.

```text
Client
    │
    ▼
Spring Boot REST API
    │
    ├──► PostgreSQL
    │
    └──► Redis
          │
          ├──► Cache hit
          │
          └──► Cache miss
                    │
                    ▼
                PostgreSQL
                    │
                    ▼
                Redis Cache
```

### How Redirects Work

When a client requests a shortened URL:

1. The request first checks **Redis** for the short code.
2. If the URL is not found in Redis (**cache miss**), it is loaded from **PostgreSQL**.
3. The URL is then stored in Redis with an appropriate **TTL**.
4. The client receives an **HTTP 302 Found** response and is redirected to the original URL.

---

## 📡 API Endpoints

### 1️⃣ Create Short URL

```http
POST /api/v1/urls
```

**Request:**

```json
{
  "url": "https://example.com",
  "expiresAt": "2030-01-01T10:00:00Z"
}
```

**Response:**

```json
{
  "shortCode": "abc1234",
  "shortUrl": "http://localhost:8080/abc1234",
  "expiresAt": "2030-01-01T10:00:00Z"
}
```

> `expiresAt` is optional.

---

### 2️⃣ Redirect

```http
GET /{shortCode}
```

Returns an **HTTP 302 Found** response and redirects the client to the original URL.

---

### 3️⃣ URL Statistics

```http
GET /api/v1/urls/{shortCode}/stats
```

**Example Response:**

```json
{
  "shortCode": "abc1234",
  "originalUrl": "https://example.com",
  "clickCount": 42,
  "createdAt": "2026-08-17T10:00:00Z",
  "expiresAt": "2026-08-18T10:00:00Z"
}
```

---

### 4️⃣ Health Check

```http
GET /actuator/health
```

Used to check whether the application is running and healthy.

---

### 5️⃣ Swagger UI

After starting the application, visit:

`http://localhost:8080/swagger-ui/index.html`

Swagger UI provides interactive API documentation and allows you to test the available endpoints directly from your browser.

---

## ⚠️ Error Handling

The API uses a consistent error response format.

**Example:**

```json
{
  "status": 404,
  "errorCode": "URL_NOT_FOUND",
  "message": "Short URL not found: abc1234",
  "timestamp": "2026-08-24T02:00:00"
}
```

### Common Error Codes

| Error Code | Description |
|---|---|
| `URL_NOT_FOUND` | Short URL does not exist |
| `URL_EXPIRED` | Short URL has expired |
| `VALIDATION_ERROR` | Request validation failed |
| `INVALID_REQUEST` | Malformed JSON request |
| `INVALID_EXPIRATION_TIME` | Expiration time is invalid |

---

## 🚀 Running Locally

### Prerequisites

- Java 25
- Docker
- Docker Compose

### Start Infrastructure

Start PostgreSQL and Redis:

```bash
docker compose up -d postgres redis
```

### Run the Application

**Linux / macOS:**

```bash
./gradlew bootRun
```

**Windows:**

```bash
.\gradlew.bat bootRun
```

The application will be available at:

`http://localhost:8080`

---

## 🐳 Running with Docker

Build and start all services:

```bash
docker compose up --build
```

Stop the services:

```bash
docker compose down
```

PostgreSQL and Redis data are persisted through Docker volumes.

---

## 🧪 Testing

Run all tests:

**Linux / macOS:**

```bash
./gradlew test
```

**Windows:**

```bash
.\gradlew.bat test
```

Integration tests use **Testcontainers** to run PostgreSQL and Redis in isolated containers.

---

## 🤖 Continuous Integration (CI)

The project uses **GitHub Actions** to automatically:

- Set up Java 25
- Start PostgreSQL and Redis
- Run the test suite
- Build the application

Every push to `main` and every pull request targeting `main` triggers the CI workflow.

---

## 📌 Project Status

🟢 **Active development**