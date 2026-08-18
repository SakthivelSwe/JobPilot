# 🚀 Deploying JobPilot ($0 stack)

JobPilot is a **safe, human-in-the-loop** career platform. It is **not** a bot — there is
no browser automation, no scraping behind auth, no credential/cookie storage, and no
automatic submission (spec §1/§26). Deployment is a clean **3-tier** stack:

```
        ┌────────────────────────┐
        │  Cloudflare Pages      │   Angular 18 SPA
        │  job-bot-frontend      │
        └───────────┬────────────┘
                    │ HTTPS + JWT
        ┌───────────▼────────────┐
        │  Render (free web svc) │   Spring Boot 3 / Java 21
        │  job-bot-backend       │   deterministic ATS · matching · discovery
        └───────────┬────────────┘
                    │ JDBC (sslmode=require)
        ┌───────────▼────────────┐
        │  Postgres (Supabase /  │   single shared database
        │  Neon free tier)       │
        └────────────────────────┘

  Discovery pulls only AUTHORIZED PUBLIC FEEDS (Greenhouse / Ashby public APIs).
  LinkedIn / Naukri are always MANUAL — surfaced in the in-app Manual Queue.
```

## 1. Database — Supabase or Neon (free Postgres)

1. Create a free Postgres project at [supabase.com](https://supabase.com) or
   [neon.tech](https://neon.tech).
2. Copy the JDBC connection string:
   `jdbc:postgresql://<host>/<db>?sslmode=require`
3. No manual SQL needed — the `prod` profile boots with `ddl-auto: update` and creates
   every table. All `List<String>` columns use `StringListConverter` (JSON-in-TEXT), so
   H2 local dev and Postgres prod share one portable schema (spec §50).
   > When the schema stabilizes, switch to Flyway migrations + `ddl-auto: validate` (§50).

## 2. job-bot-backend — Render (free web service)

1. Push `job-bot-backend/` to GitHub.
2. Render → New → Web Service:
   - **Build:** `mvn -B -DskipTests package`
   - **Start:** `java -jar target/job-bot-backend-0.0.1-SNAPSHOT.jar`
3. Environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `NEON_DATABASE_URL` — the JDBC URL from step 1 (works for Supabase or Neon)
   - `NEON_DB_USER`, `NEON_DB_PASSWORD`
   - `JWT_SECRET` — a random 40+ char string
   - `ADMIN_USERNAME=admin`
   - `ADMIN_PASSWORD_HASH` — bcrypt hash (see below)
   - `AI_PROVIDER=noop` (or `cloudflare` + `CF_ACCOUNT_ID`/`CF_API_TOKEN`, or `ollama`)
   - `APP_AI_DAILY_LIMIT=20` (optional — AI cost cap, spec §55)
   - Optional storage (resume files): `R2_BUCKET_URL`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`
4. Health check: `/actuator/health`. The frontend shows "Waking JobPilot…" on cold start
   (no artificial keep-alive is baked in — §53).

**Generate a bcrypt password hash** after first deploy:
```bash
curl -X POST https://<api-url>/api/auth/hash \
  -H "Content-Type: application/json" \
  -d '{"password":"your-new-password"}'
```
Put the returned hash in `ADMIN_PASSWORD_HASH`, redeploy, then log in at `/login`.

## 3. job-bot-frontend — Cloudflare Pages (free)

1. Push `job-bot-frontend/` to GitHub.
2. Cloudflare Pages → Create → connect repo:
   - **Framework preset:** Angular (or None)
   - **Build command:** `npm ci && npm run build`
   - **Build output directory:** `dist/job-bot-frontend/browser`
3. `src/_redirects` (`/* /index.html 200`) is bundled for SPA routing.
4. Set `src/environments/environment.prod.ts` → `apiUrl` = your Render backend URL.
5. First login: `admin` + the password whose bcrypt hash you configured in step 2.

## 4. Job sources (authorized public feeds)

Discovery reads only officially permitted, unauthenticated public endpoints:
- **Greenhouse** public job-board API (`boards-api.greenhouse.io`)
- **Ashby** public posting API (`api.ashbyhq.com/posting-api`)
- Manual URL / JD import for anything else.

Add companies under **Companies** (or seed a starter set). Set each company's `atsType`
(GREENHOUSE / ASHBY / MANUAL) and `atsToken` (Greenhouse board token or Ashby org slug).
Trigger discovery from the **Discovery** page or `POST /api/discovery/scan`.

> Greenhouse/Ashby application POSTs require an authorized API key and are **not** used —
> every discovered job is classified ASSISTED_APPLY or MANUAL_REQUIRED, and you submit the
> final application yourself (spec §24–27). LinkedIn/Naukri are always MANUAL_REQUIRED.

**Optional scheduled scan (§56):** set `DISCOVERY_SCAN_ENABLED=true` (default `false`) to
auto-scan the authorized public feeds on a cron (`DISCOVERY_SCAN_CRON`, default every 6h).
It never touches restricted platforms and never submits applications.

## 5. Local AI (optional, unlimited, free)

Run [Ollama](https://ollama.com):
```powershell
ollama pull llama3.1
```
Start the backend with `AI_PROVIDER=ollama`. The deterministic ATS + matching engines are
authoritative and work with AI off — AI only adds an optional enrichment note, capped by
the daily AI-usage limit (§54/§55).

## $0 reality
- **Render free:** sleeps when idle (~1 min cold start). The UI handles it gracefully.
- **Supabase / Neon free:** ample compute for a personal single-user setup.
- **Cloudflare Pages:** unlimited static hosting.
- **Cloudflare Workers AI:** ~10k neurons/day free (optional).

---

## Env-var cheat sheet

| Variable                    | Where            | Purpose                                          |
|-----------------------------|------------------|--------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`    | backend          | `prod`                                           |
| `NEON_DATABASE_URL`         | backend          | `jdbc:postgresql://…/…?sslmode=require`          |
| `NEON_DB_USER` / `NEON_DB_PASSWORD` | backend  | Postgres credentials (Supabase or Neon)          |
| `JWT_SECRET`                | backend          | HMAC signing key (≥32 bytes)                     |
| `ADMIN_USERNAME`            | backend          | Login name (default `admin`)                     |
| `ADMIN_PASSWORD_HASH`       | backend          | Bcrypt hash (use `POST /api/auth/hash`)          |
| `AI_PROVIDER`               | backend          | `noop` / `ollama` / `cloudflare`                 |
| `APP_AI_DAILY_LIMIT`        | backend          | Max AI calls/day (default 20, §55)               |
| `DISCOVERY_SCAN_ENABLED`    | backend          | Opt-in scheduled scan of public feeds (default `false`, §56) |
| `DISCOVERY_SCAN_CRON`       | backend          | Cron for the scheduled scan (default every 6h)   |
| `R2_*` (optional)           | backend          | Object storage for resume files (§51)            |
| `apiUrl` (environment.prod) | frontend         | Backend base URL                                 |

## What is intentionally NOT here
Per the non-negotiable safety rules (§1), JobPilot ships **no** `application-engine`,
**no** `chrome-extension`, **no** `job-engine` scraper, **no** `ENGINE_TOKEN`, and **no**
platform passwords or session cookies. Those were removed during the v2.0 safety
remediation (see `ARCHITECTURE_AUDIT.md` §6).
