# TOM — Tenant and Owner Management

TOM is a full-stack property and tenancy management application. Property owners
can manage properties, leases, tenants, rent, maintenance requests, and notices.
Tenants can view their lease and rent information, raise maintenance requests,
and read notices from their owner.

## Technology

- **Frontend:** React, TypeScript, Vite
- **Backend:** Java 17, Spring Boot, Spring Data JPA, Spring Security
- **Database:** Supabase PostgreSQL
- **Authentication:** JWT with BCrypt password hashing
- **Deployment:** One Docker container serving the frontend and backend API

## Run locally

Prerequisites: Java 17, Node.js 22 or newer, and npm.

Set the backend environment variables in PowerShell:

```powershell
$env:SUPABASE_JDBC_URL = "jdbc:postgresql://<host>:<port>/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME = "<Supabase pooler username>"
$env:SUPABASE_DB_PASSWORD = "<Supabase database password>"
$env:JWT_SECRET = "<Base64-encoded random secret of at least 32 bytes>"
```

Start the Spring Boot backend from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

In a second terminal, start the React development server:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to the backend at
`http://localhost:8080`.

## Build and test

```powershell
.\mvnw.cmd package
npm --prefix frontend run build
npm --prefix frontend run lint
```

## Docker

Build from the repository root:

```powershell
docker build -t tom-app .
```

Run the combined frontend/backend service after setting the environment
variables above:

```powershell
docker run --rm -p 8080:8080 `
  -e SUPABASE_JDBC_URL="$env:SUPABASE_JDBC_URL" `
  -e SUPABASE_DB_USERNAME="$env:SUPABASE_DB_USERNAME" `
  -e SUPABASE_DB_PASSWORD="$env:SUPABASE_DB_PASSWORD" `
  -e JWT_SECRET="$env:JWT_SECRET" `
  tom-app
```

Open `http://localhost:8080`.

## Deploy to Render

Create a Render **Web Service**, connect this repository, choose **Docker**, and
set `SUPABASE_JDBC_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`, and
`JWT_SECRET` in the service's environment settings. Set the Render health check
path to `/actuator/health`. Render provides `PORT` automatically.

The public `/actuator/health` endpoint returns the application health status and
checks database connectivity. It does not expose health details.

Never commit database credentials, JWT secrets, or `.env` files. The application
connects to Supabase from the backend; do not expose database credentials in the
frontend. Row Level Security policies should be configured before exposing the
tables through Supabase APIs.
