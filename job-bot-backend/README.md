# JobPilot — Backend (MVP, local-first)

Human-in-the-loop job application assistant. Spring Boot 3 + Java 17.
Runs **fully local on H2** with **zero external/paid services**. Deterministic ATS
engine (no AI required); AI is an optional plugin.

> Design choice: this MVP does **not** auto-apply or scrape LinkedIn/Naukri
> (against their ToS / ban risk). You copy a job's URL/JD, JobPilot analyzes it,
> selects the best resume, and you submit yourself. It then tracks the application.

## Run locally

```powershell
cd job-bot-backend
mvn spring-boot:run
```

- API: http://localhost:8080
- H2 console: http://localhost:8080/h2-console
  (JDBC URL `jdbc:h2:mem:jobpilot`, user `sa`, no password)
- Health: http://localhost:8080/actuator/health

Default profile is `local` (H2 in-memory). Switch to Supabase/Postgres later with
`-Dspring-boot.run.profiles=prod` and the `SUPABASE_DB_*` env vars.

## Core endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST/GET/PUT/DELETE | `/api/resumes` | Resume profiles (max ~4) |
| POST/GET/PUT/DELETE | `/api/criteria` | Search criteria per resume |
| POST | `/api/jobs/import` | Paste a job URL + JD text |
| POST | `/api/jobs/{id}/score` | Run deterministic ATS (`criteriaId` or `resumeId`) |
| GET | `/api/jobs` | List/filter jobs (paginated) |
| POST | `/api/ats/analyze` | Ad-hoc ATS: `{ resumeId, jobDescription }` |
| POST | `/api/pack/best-resume` | **4-resume engine** — rank all resumes for a job |
| POST | `/api/pack/cover-letter` | Generate a tailored cover letter (`jobId`, `resumeId`) |
| GET/POST/PUT | `/api/applications` | Application CRM |
| GET | `/api/applications/kanban` | Grouped by status |
| GET | `/api/dashboard/stats` | Aggregate analytics |
| GET | `/api/dashboard/resume-performance` | **Learning engine** — per-resume conversion |

## Demo data

On first run (local profile, empty DB) a `DataSeeder` inserts 3 resumes, 2 criteria,
3 jobs and 2 applications so the dashboard looks alive. Disable with
`app.seed.enabled=false`.

## Quick smoke test

```powershell
# 1. Create a resume
curl -X POST http://localhost:8080/api/resumes -H "Content-Type: application/json" -d '{
  "name":"Java Backend",
  "targetSkills":["Java","Spring Boot","Kafka","AWS","REST"],
  "resumeText":"2.8 years Java Spring Boot Kafka AWS REST microservices"
}'

# 2. Analyze a pasted JD against it (use the returned resume id)
curl -X POST http://localhost:8080/api/ats/analyze -H "Content-Type: application/json" -d '{
  "resumeId":"<RESUME_ID>",
  "jobDescription":"Looking for Java Spring Boot Kafka engineer in Chennai, REST APIs"
}'
```

## AI (optional, off by default)

`app.ai.provider=noop` (default) → fully deterministic, $0.
Set to `ollama` or `cloudflare` later to add an enrichment note. The core scoring
never depends on it.

