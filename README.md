# Smylio — Tenant–Owner Management (TOM)

> **Renting Made Happier** · Full-Stack Property, Lease, Billing & Maintenance Operations Platform

Smylio (TOM) is an **owner-managed, closed operational platform** for landlords and tenants—**not** a public property marketplace. Property owners manage their properties, link verified tenants via non-guessable tenant codes, automate monthly arrears rent billing and late fines, approve cash claims, track maintenance threads, and publish broadcast or unit-targeted notices.

---

## Core Flow

```mermaid
flowchart TD
    T["Tenant Self-Registers"] -->|"Generates random code (TNT-8-char Crockford Base32)"| ACC["Tenant Account"]
    O["Owner Creates Property"] -->|"Title, Address, Rent, Amenities -> AVAILABLE"| P["Property"]
    ACC -.->|"Shares Tenant Code"| O
    O -->|"Rate-limited masked search + PESSIMISTIC_WRITE lock"| L["Active Lease (Rent, Anniversary Day, Grace Days, Fine/Day)"]
    P --- L
    L -->|"Property -> OCCUPIED"| PORTAL["Tenant Portal"]
    L -->|"Daily IST Cron + Manual Trigger"| RENT["Arrears Rent Bills & Accrued Fines"]
    PORTAL -->|"Pay Online (UUID Txn) / Cash Claim (Owner Approval)"| RENT
    PORTAL -->|"Maintenance Tickets & Comments / Scoped Notices"| OPS["Ongoing Operations"]
```

---

## Architecture & Engineering Highlights

Designed as a production-hardened Spring Boot + React monolith packaged into a single container:

- **Short-Lived JWT + Rotating Refresh Cookie Auth:**
  - `15m` (`900s`) HS256 Bearer JWT stored **in React memory only** (never `localStorage`).
  - `256`-bit opaque refresh tokens stored as SHA-256 hex hashes in `refresh_tokens`, delivered via an `HttpOnly; SameSite=Strict; Path=/api/auth` cookie.
  - Single-use token rotation on `POST /api/auth/refresh` with **reuse detection** (replaying an old token revokes the entire `family_id`), `POST /api/auth/logout` revocation, and single-flight Axios `401` retry.
- **Constant-Time Auth & Abuse Protection:**
  - BCrypt (cost 12) password hashing with no plaintext fallback and a dummy BCrypt verification when an email does not exist to mitigate timing-based account enumeration.
  - Bucket4j in-memory rate limiting on `POST /api/auth/login` (per IP & email) and `GET /api/tenants/search` (per owner), plus `@Valid` Bean Validation on all request DTOs.
  - Tenant registration generates random `TNT-` + 8 Crockford Base32 codes (`SecureRandom`), and tenant search exposes only `name`, `maskedPhone`, and `tenantCode`.
- **Concurrency-Safe Leases & Rent Payments:**
  - Lease creation acquires a `PESSIMISTIC_WRITE` lock on the parent `Property` row (`SELECT ... FOR UPDATE`) to serialize concurrent lease assignments.
  - All rent payment state transitions (`payOnline`, `payCash`, `approveCash`, `rejectCash`) execute atomic conditional `@Modifying` `UPDATE ... WHERE status IN (...)` queries (`0` rows updated → `409 Conflict`).
- **Deterministic IST Arrears Billing & Proration:**
  - Injects an `Asia/Kolkata` `Clock` bean across all services and schedules daily billing at `00:00 IST`.
  - Handles month-end clamping (`LocalDate.plusMonths`), mid-cycle lease-end proration (`monthlyRent × daysOccupied / daysInCycle`, `RoundingMode.HALF_UP`), unified fine freezing on payment/cash-claim, and restoring `LATE` + accrued fine when an overdue cash claim is rejected.
  - All monetary values use `BigDecimal` / `DECIMAL(10, 2)`.
- **Zero-Trust Data Isolation & Observability:**
  - Dual-layer security: Spring Security route/role matchers (`ROLE_OWNER` / `ROLE_TENANT`) + row-level ownership scoping in JPA repositories (`ownerId` / `tenantId` extracted exclusively from the verified JWT subject).
  - Domain enums (`@Enumerated(EnumType.STRING)`), `CorrelationIdFilter` (`X-Request-Id` in response headers and SLF4J MDC logs), centralized `GlobalExceptionHandler`, and CORS disabled in production (`dev` profile only).

---

## System Design & Key Decisions

| Decision | Why This Choice (Trade-off) |
|---|---|
| **`leases` as the normalized link** | Properties and tenants never share a static FK; `leases` preserves multi-tenancy history, billing terms, and audit trails when a property flips back to `AVAILABLE`. |
| **Computed dues vs. stored `total_due`** | Avoids denormalized balance drift and concurrent update races; unpaid `rent_payments` rows (`PENDING`, `LATE`, `AWAITING_APPROVAL`) are the single source of truth summed server-side. |
| **Conditional `UPDATE`s & `FOR UPDATE` locks** | Payment transitions execute `UPDATE ... WHERE status IN (...)` (`0` rows → `409 Conflict`), and lease creation locks the `Property` row (`PESSIMISTIC_WRITE`) instead of fragile Java read-then-write checks. |
| **In-memory 15m JWT + SHA-256 refresh cookies** | Eliminates `localStorage` JWT XSS exposure; hashed refresh tokens allow server-side logout revocation and `family_id` replay detection (persisted with `@Transactional(noRollbackFor = ...)`). |
| **Injected `Asia/Kolkata` `Clock` & arrears model** | Bills per lease anniversary day with month-end clamping and mid-cycle proration; injecting `Clock` prevents UTC/IST date drift on cloud hosts and makes edge-case tests deterministic. |
| **Database-first `ddl-auto=validate`** | Fails fast at boot on schema/entity drift without letting Hibernate run uninspected DDL in production. |
| **Single-container SPA + `SpaController`** | Serves React static assets and `/api/**` from one origin so production runs with CORS disabled and `SameSite=Strict` cookies work cleanly without cross-origin friction. |
| **Normalized amenities & strict `BigDecimal`** | Uses `@ElementCollection` (`property_amenities`) instead of CSV strings, and `BigDecimal` / `DECIMAL(10, 2)` for all money to avoid floating-point rounding errors. |

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 17, Spring Boot 3.3.5, Spring Web, Spring Security 6 (OAuth2 Resource Server), Bean Validation, Spring Boot Actuator |
| **Data & ORM** | Hibernate 6, Spring Data JPA, HikariCP, Supabase PostgreSQL (`sslmode=require`, `ddl-auto=validate`) |
| **Security & Rate Limiting** | Nimbus HS256 JWT, `httpOnly` rotating refresh tokens, BCrypt (cost 12), Bucket4j 8.10.1 |
| **Frontend** | React 18, TypeScript 5.6, Vite 6, React Router 6, Axios, Lucide React, Custom CSS variables (Dark/Light theme) |
| **Build, CI/CD & Hosting** | Maven Wrapper, Multi-stage [Dockerfile](Dockerfile) (Node 22 + Maven/Temurin 17 + JRE 17 Alpine), GitHub Actions, Render |

---

## Features by Portal

### Owner Workspace (`/owner/**`)
- **Overview:** Active tenant count, total outstanding rent (including accrued fines & pending cash claims), open maintenance count, and cash payment approval inbox.
- **Properties:** CRUD with normalized/deduplicated amenities (`@ElementCollection`), `AVAILABLE`/`OCCUPIED` badges, and `409` delete protection when lease/ticket/notice history exists.
- **Tenants & Leases:** Rate-limited masked tenant code lookup, property-locked lease creation, active-only term updates, lease termination with automatic mid-cycle final billing, and per-tenant historical deep dive.
- **Rent & Notices:** Manual arrears billing trigger, per-lease payment logs, cash claim accept/reject, and broadcast or property-targeted notices.
- **Maintenance:** Paginated/searchable issue hub, `OPEN` → `IN_PROGRESS` → `RESOLVED` transitions, and comment threads.

### Tenant Portal (`/tenant/**`)
- **My Home:** Non-guessable tenant ID copy/share card, active lease & amenities view, landlord contact details, current balance, and recent notices.
- **Rent & Payments:** Computed dues breakdown, simulated online payment (UUID transaction ID), cash payment claim submission, and complete payment history.
- **Maintenance & Notices:** Raise tickets on active leases, edit non-closed tickets, cancel `OPEN` tickets, discuss via comment thread, and browse paginated/filtered owner notices.

---

## Data Model & API Summary

**PostgreSQL Tables (9):** `users`, `refresh_tokens`, `properties`, `property_amenities`, `leases`, `rent_payments`, `maintenance_requests`, `maintenance_comments`, `notices`.

| Route Group | Purpose |
|---|---|
| `/api/auth/**` | Registration, rate-limited login, cookie-based token refresh (`POST /refresh`), and logout revocation (`POST /logout`) |
| `/api/profile` | View and edit authenticated user's contact profile |
| `/api/properties/**` | Owner property & amenity CRUD |
| `/api/tenants/**` | Rate-limited masked tenant search (`?code=...`) and owner-scoped tenant history |
| `/api/leases/**` | Lease creation, active-lease term updates, and termination with prorated final billing |
| `/api/me/**` | Tenant self-service active leases, rent summary/history, and visible notices |
| `/api/rents/**` | Arrears billing trigger, atomic online/cash payments, owner cash approvals/rejections, lease logs |
| `/api/maintenance/**` | Paginated tickets, status updates, edits/cancellations, and discussion comments |
| `/api/notices/**` | Owner broadcast/targeted notices CRUD and paginated feeds |
| `/actuator/health` | Sanitized health endpoint for Render readiness checks |

---

## Run Locally

**Prerequisites:** Java 17+, Node.js 22+, npm, and a Supabase PostgreSQL instance.

1. **Configure environment variables:** Copy [.env.example](.env.example) to `.env` (gitignored) and fill in your values (or export them in PowerShell):
   ```powershell
   $env:SUPABASE_JDBC_URL = "jdbc:postgresql://<host>:<port>/postgres?sslmode=require"
   $env:SUPABASE_DB_USERNAME = "<Supabase pooler username>"
   $env:SUPABASE_DB_PASSWORD = "<Supabase database password>"
   $env:JWT_SECRET = "<Base64-encoded random secret of at least 32 bytes>"
   ```
2. **Start the Spring Boot backend:**
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   *(If calling `:8080` directly from `:5173` instead of using Vite's proxy, pass `-Dspring-boot.run.profiles=dev` to enable dev CORS.)*
3. **Start the React dev server (second terminal):**
   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```
   Open `http://localhost:5173` — Vite proxies `/api/**` to `http://localhost:8080`.

---

## Build & Test

```powershell
# Backend unit test suite (excludes live-DB integration tests) + JAR package
.\mvnw.cmd -B "-Dtest=!TomApplicationTests,!SecurityIntegrationTest" verify

# Full backend suite including live-DB tests (requires DB env vars / .env)
.\mvnw.cmd test

# Frontend lint & production build
npm --prefix frontend run lint
npm --prefix frontend run build
```

---

## Docker & Render Deployment

The multi-stage [Dockerfile](Dockerfile) compiles the React bundle with Node 22, embeds it into `src/main/resources/static/`, packages the Spring Boot JAR, and runs it as a non-root user on port `8080` with `SpaController` forwarding React client routes (`/`, `/login`, `/register`, `/profile`, `/faq`, `/help`, `/owner/**`, `/tenant/**`).

```powershell
docker build -t tom-app .
docker run --rm -p 8080:8080 `
  -e SUPABASE_JDBC_URL="$env:SUPABASE_JDBC_URL" `
  -e SUPABASE_DB_USERNAME="$env:SUPABASE_DB_USERNAME" `
  -e SUPABASE_DB_PASSWORD="$env:SUPABASE_DB_PASSWORD" `
  -e JWT_SECRET="$env:JWT_SECRET" `
  tom-app
```

See [docs/GITHUB_ACTIONS_CICD.md](docs/GITHUB_ACTIONS_CICD.md) for the GitHub Actions CI pipeline and Render CI-gated deployment setup. Never commit `.env` files, database passwords, or JWT secrets.
