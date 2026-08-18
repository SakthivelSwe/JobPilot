# JobPilot — Complete Project Reference

> **The definitive document.** Everything about JobPilot: what it is, every feature,
> how each part behaves, the full technology stack, every API, the data model, the
> UI/UX system, and step-by-step working guides for all four components.
>
> **Version:** 2.0 · **Date:** August 2026 · **Cost to run:** ₹0 (free-tier stack)

---

## Table of contents

1. [What JobPilot is](#1-what-jobpilot-is)
2. [The four-component architecture](#2-the-four-component-architecture)
3. [Technology stack (every layer)](#3-technology-stack)
4. [The complete feature list](#4-the-complete-feature-list)
5. [How the whole thing works end-to-end](#5-end-to-end-flow)
6. [Backend — modules, entities, engines](#6-backend-deep-dive)
7. [Complete REST API reference](#7-complete-rest-api-reference)
8. [Data model (every entity & field)](#8-data-model)
9. [Discovery & the search adapters](#9-discovery--adapters)
10. [The queue & auto-apply state machine](#10-queue--auto-apply-state-machine)
11. [application-engine (Naukri/Indeed automation)](#11-application-engine)
12. [chrome-extension (LinkedIn automation)](#12-chrome-extension)
13. [Frontend — pages, design system, behaviors](#13-frontend)
14. [Security & authentication](#14-security--authentication)
15. [Configuration & environment variables](#15-configuration--environment)
16. [Running everything locally](#16-running-locally)
17. [Deployment ($0 stack)](#17-deployment)
18. [Testing](#18-testing)
19. [Safety model & rate limits](#19-safety-model--rate-limits)
20. [File & directory map](#20-file--directory-map)

---

## 1. What JobPilot is

**JobPilot is a personal career operating system** for a Java Full-Stack developer
(~2.8 years' experience) searching in the Indian market (Chennai / Bangalore /
remote, ₹8–12 LPA target).

It does the full loop:

```
Discover jobs → Score them against your résumé → Pick the best résumé per job →
Prepare the application pack → Auto-apply where possible → Track everything →
Learn what's working → Improve
```

**Two truths that define the product:**

1. **It's intelligent, not just a tracker.** A deterministic (AI-free) engine scores
   every job on 8 factors, tells you *why* it fits, and recommends what to do.
2. **You stay in control.** Nothing is submitted until a job sits in your **Queue**
   and you approve it. Auto-apply is rate-limited and always reviewable.

**Market focus:** Naukri, LinkedIn and Indeed (India), not US ATS boards.

---

## 2. The four-component architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│  1. job-bot-frontend   — Angular 18 SPA (the UI)                         │
│     Cloudflare Pages · http://localhost:4200 (dev)                       │
└────────────────────────────────┬───────────────────────────────────────┘
                                  │ REST + JWT
┌────────────────────────────────▼───────────────────────────────────────┐
│  2. job-bot-backend    — Spring Boot 3 / Java 21 (the brain)             │
│     Render Free · http://localhost:8080 (dev)                            │
│     Discovery · ATS · 8-factor match · Queue · CRM · Analytics · Auth    │
└──────────────┬───────────────────────────────────┬──────────────────────┘
     approved   │ Naukri/Indeed jobs                │ approved LinkedIn jobs
                ▼                                   ▼
┌───────────────────────────────┐   ┌──────────────────────────────────────┐
│  3. application-engine         │   │  4. chrome-extension                 │
│     Node 20 + Playwright       │   │     Chrome MV3                        │
│     YOUR laptop (visible)      │   │     YOUR browser                     │
│     Auto-applies Naukri/Indeed │   │     Easy-Apply on LinkedIn           │
└───────────────────────────────┘   └──────────────────────────────────────┘

  Shared database: PostgreSQL (Supabase/Neon) in prod · H2 in-memory locally.
```

**Why split the apply step?** LinkedIn's Terms forbid outside programs logging into
your account, so LinkedIn applies happen **inside your own browser** (the extension).
Naukri/Indeed applies run in a **separate visible browser** the engine controls. This
keeps automation transparent and within each platform's constraints.

---

## 3. Technology stack

### Backend (`job-bot-backend`)
| Concern | Technology |
|---|---|
| Language | **Java 21** (virtual threads enabled) |
| Framework | **Spring Boot 3.2** (Web, Data JPA, Validation, Actuator, Security) |
| Auth | Spring Security + **JWT** (jjwt 0.12) — access + refresh tokens |
| HTML scraping | **Jsoup 1.17.2** (Naukri/LinkedIn search parsing) |
| Résumé parsing | **PDFBox 2.0** (PDF), **Apache POI 5.2** (DOC/DOCX) |
| Local DB | **H2** (in-memory) |
| Prod DB | **PostgreSQL** driver (Supabase / Neon) |
| Utilities | Lombok, Jackson (jsr310) |
| Testing | JUnit 5, Mockito, Spring Boot Test, **Testcontainers** (Postgres) |
| Build | **Maven** |

### Frontend (`job-bot-frontend`)
| Concern | Technology |
|---|---|
| Framework | **Angular 18** (standalone components, **Signals**) |
| Routing | Lazy-loaded routes + `authGuard` |
| HTTP | `HttpClient` + JWT interceptor (auto-refresh on 401) |
| Styling | Native **SCSS** design system, CSS custom-property tokens |
| State | Angular Signals (no NgRx) |
| Build | Angular CLI (esbuild) |

### application-engine (`application-engine`)
| Concern | Technology |
|---|---|
| Runtime | **Node.js 20 + TypeScript** |
| Browser automation | **Playwright** + `playwright-extra` + `puppeteer-extra-plugin-stealth` |
| HTTP | Axios |
| Server | Express (health endpoint) |
| Logging | Winston |

### chrome-extension (`chrome-extension`)
| Concern | Technology |
|---|---|
| Platform | **Chrome Manifest V3** |
| Parts | Service worker (background) · content script · popup |
| APIs | `chrome.alarms`, `chrome.tabs`, `chrome.storage`, `fetch` |

---

## 4. The complete feature list

### Candidate & résumé
- **Résumé upload & parsing** — PDF / DOC / DOCX / TXT → auto-extract name, email,
  phone, skills, experience, projects, education.
- **Skill normalization** — aliases collapse to canonical (`k8s`→Kubernetes,
  `springboot`→Spring Boot, `message broker`→Kafka, `AWS Cloud`→AWS).
- **Candidate profile** — verified source of truth (years, notice period, last
  working date, expected/min salary, preferred locations & work modes, target/
  excluded roles, preferred/excluded companies).
- **Proficiency + evidence** — every skill carries a proficiency
  (LEARNING/BEGINNER/WORKING/STRONG/EXPERT/UNKNOWN) with evidence; **never auto-marks
  EXPERT**, never invents experience.
- **Four résumé variants** — Java Backend / Full Stack / Microservices / Cloud, all
  derived from one master profile.
- **Automatic résumé selection** — best variant chosen per job.
- **Résumé tailoring** — reorders/emphasizes only from verified facts (no fabrication).

### Search strategy
- **Target roles** — prioritized list with required/preferred/excluded skills,
  experience range, locations, salary, notice tolerance.
- **Job criteria** — keywords, locations, experience, salary, min-match threshold.
- **Boolean criteria** — `Java AND (Kafka OR Microservices) AND NOT Intern`
  (precedence NOT > AND > OR, parentheses, multi-word phrases).

### Discovery
- **Naukri / LinkedIn / Indeed** search-based discovery (Jsoup).
- **Normalization** — cities (Bangalore→Bengaluru), work mode ("work from anywhere"→
  REMOTE), experience ranges, employment type.
- **Deduplication** — by (source+externalId), then a cross-source SHA-256 hash.
- **Source health & coverage** stats.
- **Scheduled scan** (opt-in) every N hours.

### Matching & recommendation
- **8-factor deterministic match**: Technical 35 · Experience 20 · Role 10 ·
  Location 10 · Work-mode 5 · Notice 10 · Salary 5 · Company 5 = 100.
- **Recommendation**: STRONG_APPLY / APPLY / REVIEW / LOW_PRIORITY / SKIP with **hard
  filters** (excluded company → SKIP; experience far off → cap; missing all required → cap).
- **Location engine** & **notice-period engine** (never auto-reject).
- **"Why this job"** — matched vs missing skills surfaced everywhere.

### ATS
- **Deterministic ATS score** (0–100) — technical/role/experience/location coverage,
  matched/missing keywords, suggestions, "reason to apply". Works with AI off.

### Application queue & auto-apply
- **Queue** — every discovered job becomes a review item; approve / skip / send to
  manual / bulk "approve all ≥ N".
- **Auto-apply** — the local engine (Naukri/Indeed) and the extension (LinkedIn) poll
  approved jobs and apply, rate-limited.
- **Manual queue** — jobs the bot can't submit (LinkedIn, CAPTCHA, complex forms).
- **CRM / pipeline** — Applied → Screening → Interview → Offer → Closed, with notes,
  interview dates, and per-application editing.

### Application pack
- **Cover letter** generator (deterministic, tailored).
- **Screening answers** for common questions (notice, CTC, relocation, skill years).

### Analytics & learning
- **Overview** — discovered / matched / strong / applications / manual /
  response·interview·offer rates / avg ATS / avg match.
- **Role / source / location performance**.
- **Learning engine** — after ≥20 applications, emits "JobPilot Recommendation"
  suggestions; never changes preferences silently.
- **AI usage** — optional AI (NoOp/Ollama/Cloudflare) with a **daily cap**.

### Data ownership
- **Export** applications/jobs as CSV, full backup as JSON.
- **Reset** personal data; delete résumé files.

### Platform experience (JobPilot 2.0 UI)
- **Command palette** (⌘K / Ctrl-K) — quick actions + navigation.
- **Editorial "Today"** briefing, **Discover** feed, **Pipeline** progression map,
  **Settings** workspace, warm paper design system.

---

## 5. End-to-end flow

```
1. Upload résumé            → parsed → verified candidate profile
2. Define target roles      → titles + skill requirements
3. Set criteria             → keywords, locations, experience, threshold
4. Run discovery            → Naukri/LinkedIn/Indeed searched via Jsoup
5. Normalize + dedupe       → clean JobPosting records
6. Score (8-factor match)   → matchScore + recommendation per posting
7. Auto-enqueue             → strong jobs → Queue as PENDING_REVIEW
8. You review               → Approve / Skip / Manual
9. Engine applies           → Naukri/Indeed (local engine) · LinkedIn (extension)
10. Report back             → APPLIED creates a CRM application (autoApplied=true)
11. Track                   → Pipeline progression + interview dates + notes
12. Learn                   → Analytics + learning recommendations
```

---

## 6. Backend deep dive

Package root: `com.jobbot`

```
common/          ApiResponse<T>, StringListConverter (JSON-in-TEXT), exceptions
config/          WebConfig(CORS), DataSeeder, CompanySeeder, PlatformSeeder
security/        SecurityConfig, JwtService, JwtAuthFilter, LoginRateLimiter, AuthController
module/
  candidate/     Profile + parsing (PDFBox/POI), SkillNormalizer, evidence graph
  role/          TargetRole engine (§7)
  criteria/      JobCriteria + boolean expression parser (§8)
  matching/      LocationEngine, NoticePeriodEngine, JobMatchService (8-factor),
                 RecommendationEngine
  company/       Company registry
  discovery/     JobPosting, JobNormalizer, DeduplicationService,
                 ApplicationCapabilityService, SourceHealthService,
                 JobDiscoveryService, DiscoveryScheduler,
                 adapter/{SearchBasedAdapter, NaukriDiscoveryAdapter,
                          LinkedInDiscoveryAdapter, JobSourceAdapter}
  queue/         JobQueueEntry, JobQueueStatus, JobQueueService,
                 JobQueueController, EngineController
  ats/           AtsService (deterministic), AtsController
  resume/        Resume CRUD + variant/ (ResumeVariant, selection, tailoring)
  pack/          CoverLetterService, AnswersService
  manualqueue/   ManualQueueEntry + service (compliant manual fallback)
  application/   Application (CRM) + kanban endpoints
  analytics/     AnalyticsService (overview/roles/sources/locations/learning)
  ai/            AiProvider (NoOp/Ollama/Cloudflare) + usage/ (tracker, daily cap)
  platform/      PlatformConfig (enable/limit/delay/pause — no credentials)
  account/       ExportService (CSV/JSON), reset
  storage/       StorageService + LocalStorageService (metadata only)
  job/           Legacy manual Job import (kept for compatibility)
  dashboard/     Legacy stats + resume-performance
```

### Key engines (all deterministic — AI never overrides)
- **SkillNormalizer** — alias→canonical mapping, word-boundary matching (`java` ≠ `javascript`).
- **LocationEngine** — city aliases + work-mode parsing; ambiguous → UNKNOWN.
- **NoticePeriodEngine** — parses "Immediate/15 days/1 month/Any"; prefers
  lastWorkingDate; COMPATIBLE / RECRUITER_APPROVAL / MAJOR_MISMATCH / UNKNOWN.
- **BooleanQuery** — sealed `BoolExpr` (Term/Not/And/Or) + recursive-descent parser.
- **JobMatchService** — the 8-factor weighted score.
- **RecommendationEngine** — maps score + hard filters to a recommendation.
- **AtsService** — deterministic ATS coverage score; AI enrichment optional + capped.

---

## 7. Complete REST API reference

All under `/api`, JWT-protected except `/api/auth/**` and `/actuator/health`.

**Auth**
```
POST /api/auth/login            {username,password} → {token, refreshToken}
POST /api/auth/refresh          {refresh} → new pair
POST /api/auth/hash             {password} → bcrypt hash (dev helper)
```

**Candidate / résumé parsing**
```
POST /api/candidate/resume/parse    multipart → unsaved parsed preview
POST /api/candidate/profile/confirm confirmed profile (verified=true)
GET  /api/candidate/profile
PUT  /api/candidate/profile
GET  /api/candidate/skills
```

**Target roles**
```
GET/POST/PUT/DELETE /api/target-roles[/{id}]
POST /api/target-roles/reorder
```

**Criteria**
```
GET/POST/PUT/DELETE /api/criteria[/{id}]
PATCH /api/criteria/{id}/toggle
POST  /api/criteria/validate-query   {query,sample} → {valid, matches?, error?}
```

**Résumés & engine**
```
GET/POST/PUT/DELETE /api/resumes[/{id}]
GET /api/resume-engine/variants
GET /api/resume-engine/select/{postingId}
GET /api/resume-engine/tailor/{postingId}
```

**Companies**
```
GET/POST/PUT/DELETE /api/companies[/{id}]
```

**Discovery**
```
POST /api/discovery/scan
GET  /api/discovery/sources
GET  /api/discovery/coverage
GET  /api/discovery/postings?page&size&status
```

**Match**
```
GET  /api/match/posting/{id}
GET  /api/match/top?limit&scanSize
POST /api/match/rescore?max
```

**Queue (review workbench)**
```
GET  /api/queue/pending?page&size
GET  /api/queue/auto-applying?page&size
GET  /api/queue/manual?page&size
GET  /api/queue/stats
POST /api/queue/{id}/approve
POST /api/queue/{id}/skip
POST /api/queue/{id}/send-to-manual
POST /api/queue/{id}/mark-applied
POST /api/queue/approve-all-above?threshold=80
```

**Engine (polled by application-engine + extension)**
```
GET  /api/engine/pending?platform=NAUKRI|LINKEDIN|INDEED   → 200 job | 204 none
POST /api/engine/report   {jobQueueId, success, failureReason}
```

**Manual queue**
```
GET  /api/manual-queue[?status]
GET  /api/manual-queue/stats
POST /api/manual-queue/add/{postingId}
POST /api/manual-queue/{id}/open
POST /api/manual-queue/{id}/mark-applied
POST /api/manual-queue/{id}/skip
```

**Applications (CRM)**
```
GET  /api/applications
GET  /api/applications/kanban
GET  /api/applications/{id}
POST /api/applications
PUT  /api/applications/{id}/status
POST /api/applications/{id}/notes
PUT  /api/applications/{id}/interview
DELETE /api/applications/{id}
```

**Application pack / ATS / jobs**
```
POST /api/pack/best-resume | /cover-letter | /answers
POST /api/ats/analyze   {resumeId, jobDescription}
GET/POST/PUT/DELETE /api/jobs[/{id}]   (+ /import, /{id}/score, /{id}/status)
```

**Analytics / dashboard / AI / platform / account**
```
GET /api/analytics/{overview,roles,sources,locations,learning}
GET /api/dashboard/{stats,resume-performance}
GET /api/ai/usage
GET /api/platform-config[/{platform}]
PUT /api/platform-config/{platform}
POST /api/platform-config/{platform}/{pause|resume|reset-count}
GET /api/account/export/{applications.csv,jobs.csv,data.json}
POST /api/account/reset
DELETE /api/account/resume-files
```

**Ops**
```
GET /actuator/health
```

---

## 8. Data model

All `List<String>` fields use `StringListConverter` (JSON-in-TEXT) → identical schema
on H2 and PostgreSQL. No Postgres-only `TEXT[]`.

| Entity | Key fields |
|---|---|
| **CandidateProfile** | name, email, phone, currentLocation, preferredLocations[], preferredWorkModes[], yearsOfExperience, noticePeriodDays, lastWorkingDate, expectedSalary, minimumSalary, relocationPreference, remotePreference, workAuthorization, targetRoles[], excludedRoles[], preferredCompanies[], excludedCompanies[], verified |
| **CandidateSkill / SkillEvidence** | canonicalName, proficiency, category, evidence items |
| **WorkExperience / Project / Education / Certification / Achievement** | evidence graph |
| **ResumeSourceDocument** | storagePath, fileName, mimeType, size, checksum |
| **Resume** | name, targetRoles[], targetSkills[], resumeText, experienceSummary, active |
| **ResumeVariant** (enum) | JAVA_BACKEND / JAVA_FULLSTACK / JAVA_MICROSERVICES / JAVA_CLOUD + prioritySkills + roleTarget |
| **TargetRole** | roleTitle, priority, requiredSkills[], preferredSkills[], excludedSkills[], min/maxExperience, locations[], remotePreference, salary min/max, noticeToleranceDays, active |
| **JobCriteria** | name, keywords[], locations[], experienceMin/Max, salary, jobType, excludeCompanies[], minMatchScore, **booleanQuery**, active |
| **Company** | name, domain, careersUrl, country, industry, companyType, atsType, atsToken, sourceStatus, lastChecked, active |
| **JobPosting** | source, externalId, title, company, location, remoteType, employmentType, description, salary, min/maxExperience, requiredSkills[], preferredSkills[], normalizedHash, sourcesSeen[], applicationCapability, matchScore, recommendation, status |
| **JobQueueEntry** | jobPostingId, externalId, platform, title, company, location, jobUrl, atsScore, matchScore, recommendation, matchedKeywords[], missingKeywords[], resumeVariant, criteriaId, status, failureReason, appliedAt, reviewedAt |
| **ManualQueueEntry** | postingId, company, role, source, jobUrl, capability, reason, matchScore, recommendedVariant, status, applicationId |
| **Application** | jobId, resumeId, criteriaId, platform, company, title, status, atsScore, matchedKeywords[], missingKeywords[], interviewDate, interviewRound, offerCtcLpa, notes, **autoApplied**, jobQueueId |
| **PlatformConfig** | platformName, enabled, dailyLimit, minDelaySeconds, currentCountToday, lastResetDate, paused *(no credentials)* |
| **AiUsage** | date, provider, feature, request-count |

---

## 9. Discovery & adapters

**`SearchBasedAdapter`** interface → `discover(TargetRole, JobCriteria)`.

**NaukriDiscoveryAdapter**
- Builds `https://www.naukri.com/{role-hyphenated}-jobs-in-{city}?experience={min}-{max}`
  (remote → `?jobtype=remote`).
- Jsoup: honest User-Agent, 10 s timeout, `ignoreHttpErrors`.
- Parses `article.jobTuple` / `[data-job-id]` cards → title, company, experience,
  salary, location, URL, externalId.
- **3-page cap**, 2–3.5 s randomised sleep between pages.
- Capability: **ASSISTED_APPLY**.

**LinkedInDiscoveryAdapter**
- Public **guest** endpoint `/jobs-guest/jobs/api/seeMoreJobPostings/search` with
  `keywords`, `location`, `f_E` (experience level), `start`.
- Parses `data-entity-urn` job IDs + card fields (no JD from guest endpoint).
- **2 pages × 25**, 1.5–2.5 s sleep.
- Capability: **MANUAL_REQUIRED** server-side (extension applies).

**JobDiscoveryService** — per scan: for each active TargetRole × each adapter →
fetch → `JobNormalizer` → `DeduplicationService` → `ApplicationCapabilityService` →
score with `JobMatchService` → persist `JobPosting` → auto-`enqueueFromPosting`.

---

## 10. Queue & auto-apply state machine

```
                 ┌──────────────► SKIPPED
                 │
PENDING_REVIEW ──┼──► APPROVED ──► AUTO_APPLYING ──► APPLIED
                 │        (engine picks it up)   │
                 │                               ├─► FAILED_APPLY   (generic error)
                 └──► MANUAL_APPLY ◄─────────────┘  (CAPTCHA/BLOCKED/LOGIN/2FA)

FILTERED_OUT = score < threshold OR recommendation = SKIP (kept for the record)
```

- **`enqueueFromPosting`** dedupes by (externalId, platform); filters out below
  threshold / SKIP.
- **`pickNextApproved(platform)`** — pessimistic-locked, checks `PlatformConfig`
  rate limit, atomically flips APPROVED → AUTO_APPLYING (prevents double-pick).
- **`markAutoApplied`** — creates `Application{autoApplied=true}` + increments the
  platform's `currentCountToday`.
- **`markFailed`** — CAPTCHA/BLOCKED/LOGIN/2FA → MANUAL_APPLY; else FAILED_APPLY.

---

## 11. application-engine

**Purpose:** auto-apply to **Naukri & Indeed** in a **visible** Chromium on your laptop.

```
src/
  index.ts                 poll loop (per platform) + Express :3001/health
  api/JobBotApiClient.ts    login + 401→refresh; getNextPending / report / platform config
  rate/PlatformRateChecker  client-side rate check before each apply
  automation/
    BrowserManager.ts       playwright-extra + stealth, headless:false, persistent profile
    HumanBehavior.ts        randomised delays, per-char typing (7% typo), mouse-to-box
    NaukriApplicator.ts     Already-Applied/CAPTCHA/no-button guards, login wall, screenshots
    IndeedApplicator.ts     Easy Apply modal walker
```

**Loop:** every 30 s, for each of `PLATFORMS` (default NAUKRI,INDEED) →
rate-check → `GET /api/engine/pending` → apply → `POST /api/engine/report` →
sleep **5–8 min** on success (1–2 min on failure).

**Guarantees:** never headless; never stores credentials except in your local `.env`;
CAPTCHA/login-wall → reported so the backend routes to your Manual queue.

**Config (`.env`):** `API_BASE_URL`, `API_USERNAME`/`API_PASSWORD` (or `API_TOKEN`),
`POLL_INTERVAL_MS`, `MIN/MAX_INTER_APPLY_MS`, `NAUKRI_EMAIL`/`NAUKRI_PASSWORD`,
`CHROME_PROFILE_DIR`, `PLATFORMS`.

---

## 12. chrome-extension

**Purpose:** Easy-Apply on **LinkedIn** inside your own logged-in Chrome (MV3).

```
manifest.json            host_permissions: linkedin.com, localhost, *.onrender.com
background/worker.js      chrome.alarms every 30s → GET /api/engine/pending?platform=LINKEDIN
                          → open/reuse LinkedIn tab → message content script → POST report
content/linkedin.js       clicks "Easy Apply", walks Continue/Review/Submit up to 12 steps,
                          detects required-field errors → reports REQUIRES_MANUAL_INPUT
popup/popup.html/.js      API base + JWT token, today's count, pause/resume, poll-now
```

**Setup:** `chrome://extensions` → Developer mode → Load unpacked → paste backend URL
+ JWT in the popup → keep a LinkedIn Jobs tab open. Never stores LinkedIn
credentials/cookies.

---

## 13. Frontend

### Routes (all `authGuard`-protected except `/login`)
`/login · /dashboard(Today) · /discovery(Discover) · /queue(Review) · /manual ·
/applications(Pipeline) · /resumes[/new|/:id] · /criteria[/new|/:id] · /jobs ·
/jobs/import · /jobs/posting/:id · /analytics(Insights) · /settings`

### Design system — "The Desk" (JobPilot 2.0)
- **Warm paper canvas** (`#F4F1EA`), ink text, hairline dividers — no dark navy.
- **Terracotta** (`#B4532A`) is the single accent; semantic forest/ochre/brick/slate.
- **No gradients, no glow, no neon, no glassmorphism.**
- **Typography:** serif display (`--font-display`) for titles, Inter for UI, **mono
  numerals** for all numbers.
- Tokens in `styles.scss`; legacy variable names aliased so every page re-themes.
- Primitives: `.agenda-row`, `.stage-rail`, `.momentum`, `.section-head`, `.pf-badge`
  (underline-coded platforms), `.score`, `.skeleton`, `.chip`, editorial `table.data`.

### Shell
- **Top workspace bar** (no sidebar), nav grouped by workflow
  (Today · Discover/Review/Manual · Pipeline · Résumés/Criteria/Insights/Settings),
  live badges on Review (pending) and Manual.
- **Command palette** (⌘K/Ctrl-K) — quick actions + navigation, full keyboard control.
- Real mobile menu (not a squished sidebar).

### Pages & behaviors
| Page | Structure & behavior |
|---|---|
| **Today** | Editorial masthead (greeting + computed status), **"Needs your attention" agenda** (interviews, review, strong matches, follow-ups, manual), **pipeline stage rail**, inline **momentum** line, **insight** sentence, résumé-performance table. |
| **Discover** | Sticky **filter rail** (search, min-match slider, source/recommendation toggles, live source dots) + **vertical opportunity feed**; each row has a match badge, "Why it fits" chips, Review/Save/Open; strong (≥80) get a terracotta spine; skeleton loading; actionable empty/error states. |
| **Review (Queue)** | Two tabs — Pending Review (score ring, platform tag, matched/missing chips, Approve/Manual/Skip/Open, "Approve all 80+") and Auto-Applying (status dots + Send-to-Manual on failures). |
| **Manual** | Match score, reason, recommended variant, Open/Applied✓/Skip; status filter. |
| **Pipeline** | **Career progression map** — vertical connected stages (Applied→Screening→Interview→Offer→Closed), expandable rows with company, age, next action; edit drawer (status/interview/notes/delete). |
| **Résumés / Criteria / Insights** | Inherit the editorial system; Insights shows overview + learning recommendations + role/source tables. |
| **Settings** | **Configuration workspace** — section nav (Job sources · AI · Data & privacy) + focused panels: source switches + inline limits, AI provider + usage meter, export/reset. |
| **Job Detail** | Left: requirements + description + tailored résumé. Right sticky: overall match, **8-factor breakdown bars**, risk factors, recommended variant, capability, Prepare/Open/Manual actions. |

### Data behaviors
- Signals for reactive state; graceful degradation if any endpoint returns nothing.
- JWT interceptor: attaches access token, on **401 → refresh → retry once → logout**.

---

## 14. Security & authentication

- **JWT**: short-lived **access** token (60 min) + long-lived **refresh** token (14 days).
  Refresh tokens are **rejected as bearer** — only `type=access` authenticates.
- `POST /api/auth/refresh` rotates the pair.
- **Login brute-force protection**: 5 attempts / 15-min lock (configurable).
- **Bcrypt** password hashing; default `admin` / `changeme` (rotate via `/api/auth/hash`).
- **CORS** for `/api/**`; stateless session policy.
- **PII** endpoints (candidate, résumés, applications, exports) all authenticated.
- **No external-platform credentials** stored server-side (only in the local engine's `.env`).

---

## 15. Configuration & environment

**Backend `application.yml`** — profiles `local` (H2) and `prod` (Postgres).

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` in deployment |
| `NEON_DATABASE_URL` / `NEON_DB_USER` / `NEON_DB_PASSWORD` | Postgres |
| `JWT_SECRET` | HMAC signing key (≥32 bytes) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD_HASH` | login (bcrypt) |
| `AI_PROVIDER` | `noop` / `ollama` / `cloudflare` |
| `APP_AI_DAILY_LIMIT` | AI call cap (default 20) |
| `DISCOVERY_SCAN_ENABLED` / `DISCOVERY_SCAN_CRON` | opt-in scheduled scan |
| `R2_*` | optional object storage for résumé files |

**Frontend:** `environment.prod.ts` → `apiUrl`.
**Engine:** `.env` (see §11). **Extension:** popup fields.

---

## 16. Running locally

```powershell
# 1) Backend  (H2, no setup)
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
cd "C:\AI projects\dummby-project\job-bot-backend"; mvn spring-boot:run
# → http://localhost:8080/actuator/health  ⇒ {"status":"UP"}

# 2) Frontend  (new terminal)
cd "C:\AI projects\dummby-project\job-bot-frontend"; npm start
# → http://localhost:4200   (login admin / changeme)

# 3) Engine — optional, for Naukri/Indeed auto-apply (new terminal)
cd "C:\AI projects\dummby-project\application-engine"
npm install; npx playwright install chromium; copy .env.example .env
npm run dev            # health → http://localhost:3001/health

# 4) Extension — optional, for LinkedIn
#    chrome://extensions → Developer mode → Load unpacked → chrome-extension/
```

**Addresses:** app `:4200` · backend `:8080` · engine `:3001` · H2 console
`/h2-console` (`jdbc:h2:mem:jobpilot`, `sa`). **Stop:** `Get-Process java,node | Stop-Process -Force`.

> Local DB is **in-memory** — data resets on backend restart (expected for dev).
> Press **Ctrl-K** anywhere for the command palette.

---

## 17. Deployment ($0 stack)

```
Angular 18  → Cloudflare Pages   (dist/job-bot-frontend/browser, _redirects bundled)
Spring Boot → Render Free        (Java 21 jar; SPRING_PROFILES_ACTIVE=prod)
PostgreSQL  → Supabase / Neon     (NEON_DATABASE_URL, sslmode=require)
Engine      → your laptop         (never deployed)
Extension   → your Chrome         (load unpacked)
Optional AI → Ollama (local) or Cloudflare Workers AI (free tier)
```

- Render is ephemeral → no local-FS persistence; all durable data in Postgres.
- UI shows "Waking JobPilot…" on cold start (no artificial keep-alive baked in).
- Full steps + env-var cheat-sheet in `DEPLOY.md`.

---

## 18. Testing

**Backend: 99 tests (98 pass + 1 Docker-gated Postgres IT).**
Coverage: résumé extraction, skill normalization, boolean criteria, location, notice,
ATS, 8-factor matching, recommendation, résumé selection + **no-fabrication tailoring**,
Naukri/LinkedIn adapter parsing, normalization, dedupe, capability, source health,
analytics/learning, AI usage cap, CSV export, and `PostgresSchemaTest` (Testcontainers,
skips cleanly without Docker).

**Frontend:** production build green; type-checked against all endpoints.
**Engine:** `tsc --noEmit` → 0 diagnostics.

Run backend tests: `mvn test`. Build frontend: `npm run build`.

---

## 19. Safety model & rate limits

**Rate limits (Settings → Job sources):** Naukri 30/day · LinkedIn 15/day ·
Indeed 20/day, min 5 min between applies. Enforced **twice** — client-side in the
engine/extension and server-side in `pickNextApproved`.

**Guarantees:**
- Nothing applies without a job being **approved by you** in the Queue.
- The Naukri/Indeed browser is **always visible** (never headless).
- **No** LinkedIn/Naukri passwords or session cookies stored server-side.
- CAPTCHA / login-wall / complex forms → routed to your **Manual** queue.
- LinkedIn applies happen only inside your own browser session (extension).

**Good practice:** review the queue rather than always bulk-approving; run the engine
only while at your desk; check the Manual page daily (the best LinkedIn roles live there);
keep LinkedIn ≤15/day.

---

## 20. File & directory map

```
dummby-project/
├── ARCHITECTURE_AUDIT.md      full audit + phase-by-phase progress log
├── DEPLOY.md                  $0 deployment guide + env cheat-sheet
├── USER_GUIDE.md              plain-English how-to
├── PROJECT_OVERVIEW.md        original v1 overview
├── COMPLETE_PROJECT_REFERENCE.md   ← this document
├── README.md
│
├── job-bot-backend/           Spring Boot 3 · Java 21
│   ├── pom.xml · render.yaml
│   └── src/main/java/com/jobbot/…   (modules per §6)
│   └── src/main/resources/application.yml
│   └── src/test/java/…              (99 tests)
│
├── job-bot-frontend/          Angular 18
│   ├── angular.json · package.json
│   └── src/
│       ├── styles.scss              design system tokens
│       └── app/
│           ├── app.component.*      top-bar shell + command palette
│           ├── app.routes.ts
│           ├── core/{guards,models,services}
│           ├── shared/{toast, command-palette}
│           └── features/{dashboard, discovery, queue, manual-queue,
│                          applications, resumes, criteria, analytics,
│                          settings, job-detail, jobs, auth}
│
├── application-engine/        Node 20 + TS + Playwright (local)
│   └── src/{index, api, rate, automation, logger, types}
│
└── chrome-extension/          Chrome MV3 (local)
    └── {manifest.json, background/, content/, popup/}
```

---

## Quick reference card

| | |
|---|---|
| **App** | http://localhost:4200 · login `admin` / `changeme` |
| **Backend health** | http://localhost:8080/actuator/health |
| **Engine health** | http://localhost:3001/health |
| **Command palette** | Ctrl-K / ⌘K |
| **Daily routine** | Add résumé → set criteria → target roles → Scan → Review queue → Approve → engine applies → track Pipeline |
| **Stop all** | `Get-Process java,node \| Stop-Process -Force` |

---

*JobPilot 2.0 — a personal career operating system. It finds the right jobs,
explains why they fit, picks your best résumé, applies where it safely can, and keeps
you in control the whole way.*

