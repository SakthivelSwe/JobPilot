# 📘 JobPilot — Complete Project Documentation (End to End)

> **Author context:** 2.8 yrs · Java / Spring Boot / Kafka / AWS / Angular
> **Built:** August 2026 · **Cost to run:** ₹0 (local-first)

---

## 1. What this project is

**JobPilot** is a **human-in-the-loop job-application assistant**. You paste a job's
URL + description; it scores the job against your resumes with a **deterministic ATS
engine**, tells you **which of your resumes fits best**, drafts a **cover letter** and
**screening answers**, and tracks every application in a **Kanban CRM** with
**analytics**. **You** click "Apply" — the system never scrapes or auto-applies.

### Why this design (the key decision)
The original brief described an aggressive bot that scrapes LinkedIn/Naukri and
auto-clicks Apply. That violates their Terms of Service and gets accounts banned.
JobPilot keeps every genuinely useful part (matching, ATS, resume selection,
application CRM, analytics) and **removes the risky automation**. The result is
legal, free, reliable, and a stronger portfolio piece.

| Original "bot" idea | What we built instead |
|---------------------|------------------------|
| Playwright scrapers + auto-apply | Manual job import (paste URL + JD) |
| Gemini required for scoring | **Deterministic** ATS; AI optional plugin |
| Postgres-only `TEXT[]` columns | Portable JSON columns (H2 **and** Postgres) |
| Session cookies, rate limits, stealth | None needed |

---

## 2. Architecture

```
┌───────────────────────────────┐
│  Angular 17 Frontend          │   Cloudflare Pages (prod)
│  Dashboard · Resumes · Criteria│   http://localhost:4200 (dev)
│  Jobs · Import · Kanban        │
└──────────────┬────────────────┘
               │ REST (JSON, CORS)
               ▼
┌───────────────────────────────┐
│  Spring Boot 3 · Java 17       │   Render Free (prod)
│  Resume/Criteria/Job/App CRUD  │   http://localhost:8080 (dev)
│  ATS engine · Application Pack │
│  Learning analytics · AI plug  │
└──────────────┬────────────────┘
               │ JPA
               ▼
┌───────────────────────────────┐
│  Database                      │
│  H2 in-memory (local/dev)      │
│  Supabase Postgres (prod)      │
└───────────────────────────────┘

        AI Layer (optional, off by default)
        NoOp (default) │ Ollama (local) │ Cloudflare Workers AI
```

**Two repos, one workspace:**
- `job-bot-backend/` — Spring Boot 3, Java 17
- `job-bot-frontend/` — Angular 17

---

## 3. Backend — Spring Boot 3 (Java 17)

### 3.1 Tech
- Spring Web (REST), Spring Data JPA, Bean Validation, Actuator, Lombok
- **H2** (runtime, local profile) · **PostgreSQL** driver (runtime, prod profile)
- Two Spring profiles: `local` (default, H2 in-memory) and `prod` (Supabase)

### 3.2 Package layout (`com.jobbot`)
```
JobBotApplication.java              app entry
common/
  ApiResponse.java                  { success, data, message, timestamp }
  StringListConverter.java          List<String> <-> JSON column (H2+PG portable)
  exception/JobBotException.java
  exception/GlobalExceptionHandler.java   @RestControllerAdvice (400/404/500)
config/
  WebConfig.java                    CORS for /api/**
  DataSeeder.java                   seeds demo data on first run (local only)
module/
  resume/       Resume, Repository, Service, Controller, dto/ResumeCreateDTO
  criteria/     JobCriteria, Repository, Service, Controller, dto/CriteriaCreateDTO
  job/          Job, Repository, Service, Controller, dto/JobImportDTO
  application/  Application, Repository, Service, Controller, dto/ApplicationCreateDTO
  ats/          AtsService, AtsController, dto/AtsResult, AtsAnalyzeRequest, ResumeMatch
  pack/         CoverLetterService, AnswersService, PackController
  ai/           AiProvider, NoOpAiProvider, OllamaAiProvider, CloudflareAiProvider
  dashboard/    DashboardController (stats + resume-performance)
```

### 3.3 Data model (entities)
| Entity | Key fields |
|--------|-----------|
| **Resume** | name, targetRoles[], targetSkills[], resumeText, experienceSummary, active |
| **JobCriteria** | name, resumeId, keywords[], locations[], experienceMin/Max, minMatchScore, active |
| **Job** | platform, title, company, location, description, url, matchScore, matchKeywords[], missingKeywords[], reasonToApply, status |
| **Application** | jobId, resumeId, criteriaId, company, title, status, atsScore, interviewDate, interviewRound, notes |

All `List<String>` fields are stored as JSON text via `StringListConverter`, so the
**same entities run on H2 and Postgres** (no Postgres-only `TEXT[]`).

### 3.4 The deterministic ATS engine (`AtsService`)
No AI required. Weighted score out of 100:
- **Technical 55%** — how many target skills appear in the JD
- **Role 15%** — how many of those also appear in the resume text
- **Experience 20%** — candidate range vs JD
- **Location 10%** — preferred location / remote match

Outputs: score, matched/missing keywords, `bestResumeAngle`, suggestions,
`shouldApply` (>=60), and a human-readable **"Reason to Apply"** block.

### 3.5 The "4-Resume Engine" (`AtsService.rankResumes`)
Scores a job against **all active resumes**, returns them ranked best-first, and flags
the top one as `recommended`. This is what tells you which resume to use per job.

### 3.6 Application Pack (`pack` module)
- **CoverLetterService** — generates a tailored, professional cover letter from
  resume + job + matched skills (deterministic; AI note appended if enabled).
- **AnswersService** — drafts answers to common screening questions (interest,
  notice period, CTC, relocation, skill experience, gap handling).

### 3.7 Learning engine (`DashboardController.resumePerformance`)
Groups applications by resume and computes applications → responses → interviews →
offers and **conversion rates**, so you learn which resume performs best.

### 3.8 Pluggable AI (`ai` module) — optional, off by default
`AiProvider` interface with three implementations selected by `app.ai.provider`:
- `noop` (default) — nothing, fully free/offline
- `ollama` — local LLM at `localhost:11434`
- `cloudflare` — Workers AI (10k neurons/day free)

The core scoring **never depends on AI**; providers only add an enrichment note.

### 3.9 Full REST API
```
# Resumes
POST/GET/PUT/DELETE  /api/resumes[/{id}]
# Criteria
POST/GET/PUT/DELETE  /api/criteria[/{id}]
PATCH                /api/criteria/{id}/toggle
# Jobs
POST                 /api/jobs/import           paste URL + JD
POST                 /api/jobs/{id}/score       score vs criteria/resume
GET                  /api/jobs                  paginated + filter
PUT                  /api/jobs/{id}/status
# ATS
POST                 /api/ats/analyze           { resumeId, jobDescription }
# Application Pack
POST                 /api/pack/best-resume      rank all resumes for a job
POST                 /api/pack/cover-letter     { jobId, resumeId }
POST                 /api/pack/answers          { jobId, resumeId }
# Applications (CRM)
GET/POST             /api/applications
GET                  /api/applications/kanban
PUT                  /api/applications/{id}/status
PUT                  /api/applications/{id}/interview
# Dashboard
GET                  /api/dashboard/stats
GET                  /api/dashboard/resume-performance
# Health
GET                  /actuator/health
```

---

## 4. Frontend — Angular 17

### 4.1 Tech
- **Standalone components**, lazy-loaded routes, typed models
- Native SCSS design system (Inter font, gradients, shadows, score rings) — **no
  heavy UI libraries**, so the build is fast and reliable
- **Native HTML5 drag-and-drop** Kanban (no CDK dependency)
- Signals-based **toast** notifications (no `alert()`)

### 4.2 Structure
```
core/
  models/index.ts             all TS interfaces (Resume, Job, ATS, etc.)
  services/                   api, resume, criteria, job, application, pack, toast
shared/toast/                 global toast host
features/
  dashboard/                  stat cards, bars, success rate, learning table
  resumes/                    list + form
  criteria/                   list + form
  jobs/                       list (search + score rings) + import (application pack)
  applications/               Kanban with detail drawer
app.component / routes / config
environments/                 environment.ts (dev) + environment.prod.ts
```

### 4.3 Pages
| Route | What it does |
|-------|--------------|
| `/dashboard` | 4 stat cards, platform/status bars, success-rate bar, **Learning Engine** table |
| `/resumes` | card grid + create/edit form |
| `/criteria` | list (activate/deactivate) + form |
| `/jobs` | searchable table, ATS **score rings**, mark applied |
| `/jobs/import` | paste JD → score → **best-resume ranking** → **cover letter** → **screening answers** |
| `/applications` | 6-column **Kanban**, drag to change status, click for detail drawer |

---

## 5. How the daily workflow feels

```
1. Find a job on LinkedIn/Naukri (as a human)
2. Copy the URL + JD → JobPilot "Import a job"
3. Click "Find best resume"  → e.g. "Full Stack: 91% (recommended)"
4. Click "Generate"          → tailored cover letter
5. Click "Generate" answers  → screening Q&A drafts
6. YOU submit on the real site, then click "Applied ✓"
7. Track it on the Kanban; Dashboard shows which resume converts best
```

---

## 6. Running locally

```powershell
# Backend  (H2, no setup)
cd job-bot-backend
mvn spring-boot:run           # http://localhost:8080

# Frontend (new terminal)
cd job-bot-frontend
npm start                     # http://localhost:4200
```
First run auto-seeds 3 resumes, 2 criteria, 3 jobs, 2 applications so the UI looks
alive. H2 console: http://localhost:8080/h2-console (`jdbc:h2:mem:jobpilot`, `sa`).

Optional local AI:
```powershell
ollama pull llama3.1
$env:AI_PROVIDER="ollama"; mvn spring-boot:run
```

---

## 7. Deployment ($0)

- **Frontend → Cloudflare Pages**: build `npm run build`, output
  `dist/job-bot-frontend/browser`, `_redirects` bundled for SPA routing.
- **Backend → Render Free**: `render.yaml` provided; `prod` profile auto-creates
  tables in **Supabase Postgres** (`ddl-auto: update`).
- **Keep-alive**: cron-job.org ping to `/actuator/health` every 10 min.

See `DEPLOY.md` for full steps.

---

## 8. What was validated (end-to-end)

| Check | Result |
|-------|--------|
| Backend compiles (Maven) | ✅ |
| Backend boots on H2 | ✅ UP |
| Seed data on first run | ✅ 3 resumes / 3 jobs |
| Job import + deterministic ATS | ✅ 96% "STRONG APPLY" |
| Ad-hoc ATS analyze | ✅ 80 / shouldApply |
| 4-resume ranking | ✅ Full Stack 91% recommended |
| Cover letter generation | ✅ tailored to company |
| Screening answers | ✅ 5 tailored Q&A |
| Learning engine | ✅ 50% interview rate row |
| AI providers compile + boot on noop | ✅ |
| Frontend production build | ✅ all lazy chunks |
| Frontend serves | ✅ 200 |
| Cross-origin API (CORS) | ✅ ACAO: localhost:4200 |

---

## 9. Deliverables checklist

- [x] Spring Boot backend (40+ classes, 10 modules)
- [x] Deterministic ATS + 4-resume engine
- [x] Application Pack (cover letter + screening answers)
- [x] Learning-engine analytics
- [x] Pluggable AI (NoOp / Ollama / Cloudflare)
- [x] Demo-data seeder
- [x] Angular 17 frontend (6 pages, premium UI, Kanban, toasts)
- [x] H2 local + Supabase prod (portable entities)
- [x] `render.yaml`, `DEPLOY.md`, `_redirects`, per-repo READMEs
- [x] End-to-end validated + running locally

---

## 10. Possible next steps (not yet built)

1. Resume **PDF upload** to Supabase Storage (prod).
2. **AI status indicator** in the UI (which provider is active).
3. Auth (single-user token) before public deploy.
4. Export application history to CSV.
5. Kafka/Redis — only if/when async processing is actually needed.
```


