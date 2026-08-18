# PRODUCT_BEHAVIOR_SPEC.md — JobPilot authoritative functional contract

> **This document is the single source of truth for how JobPilot behaves today.**
> It is derived from the **actual current source code** (not older docs). Where any
> other document disagrees with this file, **this file wins** and the other document
> must be corrected.
>
> **Verified:** 2026-08-18 against the live repository (backend controllers + Angular
> services + entity code + config).

---

## 0. Current-state truth (resolves the documentation contradiction)

JobPilot's automation history had two turns:

1. **2026-08-17** — the aggressive/stealth automation was **removed** (safety remediation).
2. **2026-08-18** — a **deliberate, scoped automation was restored** for the Indian
   market ("India-first pivot"): Naukri/Indeed via a **local, visible** Playwright
   engine, and LinkedIn via a **Chrome extension running in the user's own session**.

**Therefore the authoritative current truth is:**

- The **four-component architecture is real and present today**: `job-bot-frontend`,
  `job-bot-backend`, `application-engine/`, `chrome-extension/`.
- The backend **does** expose `/api/engine/pending` and `/api/engine/report`, a
  `queue` module with the `AUTO_APPLYING` state, and Naukri/LinkedIn/Indeed
  discovery adapters. These are verified to exist in code.
- `COMPLETE_PROJECT_REFERENCE.md` and `USER_GUIDE.md` accurately describe this
  current state. `ARCHITECTURE_AUDIT.md` contains both the remove and the restore
  entries in its progress log; a "CURRENT STATE" header has been added there to
  prevent misreading the mid-history "deleted" note as the final state.

> The **old stealth behaviour is NOT restored**: no fingerprint spoofing, no headless
> operation, no server-side LinkedIn login, no session-cookie storage. See §7.

---

## 1. What is real vs. what is a gap (honest summary)

| Area | Backend | API | Frontend UI | Notes |
|---|---|---|---|---|
| Auth (JWT + refresh) | ✅ | ✅ | ✅ | Login page + interceptor |
| Résumé (flat) CRUD | ✅ | ✅ | ✅ | `/resumes` page |
| **Candidate profile + résumé parsing** | ✅ | ✅ | **❌ no page** | `/api/candidate/**` has no SPA screen yet |
| **Target roles** | ✅ | ✅ | **❌ no page** | Drives discovery; no create UI yet |
| Criteria (basic) | ✅ | ✅ | ✅ | `/criteria` page |
| **Boolean criteria (`validate-query`)** | ✅ | ✅ | **❌ not surfaced** | Backend parser exists; no builder UI |
| **Company registry** | ✅ | ✅ | **❌ no page** | `/api/companies` unused by SPA |
| Discovery (scan/sources/coverage/postings) | ✅ | ✅ | ✅ | `/discovery` |
| 8-factor match + recommendation | ✅ | ✅ | ✅ | Discovery + Job Detail |
| ATS score | ✅ | ✅ | ✅ | Job import / detail |
| Queue + auto-apply state machine | ✅ | ✅ | ✅ | `/queue` (Review) |
| Engine endpoints (`/api/engine/*`) | ✅ | ✅ | n/a (SPA) | Consumed by engine + extension |
| Manual queue | ✅ | ✅ | ✅ | `/manual` |
| Application CRM / pipeline | ✅ | ✅ | ✅ | `/applications` progression map |
| Application pack (cover letter/answers) | ✅ | ✅ | ✅ | Job import |
| Résumé variants + tailoring | ✅ | ✅ | ✅ (partial) | Surfaced in Job Detail |
| Analytics + learning gate (≥20) | ✅ | ✅ | ✅ | `/analytics` (Insights) |
| AI usage cap | ✅ | ✅ | ✅ | Settings |
| Platform config (limits) | ✅ | ✅ | ✅ | Settings |
| Data export / reset | ✅ | ✅ | ✅ | Settings |
| **Interview center** | ⚠️ partial (interview date/round on Application) | via `/applications/{id}/interview` | ⚠️ drawer only | No dedicated prep workspace yet |
| **Company view / Activity feed / global search** | ❌ | ❌ | ❌ | Not built (see §12 roadmap) |

**Rule:** the UI must not present the ❌ rows as if they exist. Interview/company/
activity/search are **planned**, not shipped, and must be labelled as such or omitted.

---

## 2. Application capability model (truthful)

Every `JobPosting` carries a capability from `ApplicationCapabilityService` — never
inferred from the platform name in the UI; always read from the backend field.

| Capability | Meaning today | Who submits |
|---|---|---|
| `AUTO_ELIGIBLE` | Only if an authorized integration + user opt-in + supported source all hold. **None ship enabled.** | (none) |
| `ASSISTED_APPLY` | JobPilot prepares the pack; the **local application-engine** can auto-fill Naukri/Indeed within rate limits, but a visible browser is used and the user can supervise. | Local engine (supervised) |
| `MANUAL_REQUIRED` | No server-side submission. **LinkedIn is always this** — the Chrome extension applies inside the user's own browser. | User / extension in own session |
| `UNAVAILABLE` | Closed / duplicate / broken. | (none) |

**Current source → capability mapping:**
- `NAUKRI`, `INDEED` → `ASSISTED_APPLY`
- `LINKEDIN` → `MANUAL_REQUIRED` (extension handles apply; server never does)
- `GREENHOUSE/ASHBY/LEVER/WORKABLE` → `ASSISTED_APPLY` (only if a company is seeded)
- everything else / null → `MANUAL_REQUIRED`

The capability **reason** must be visible in the UI (e.g. "LinkedIn Easy Apply is
handled by the browser extension, not the server").

---

## 3. Canonical state machines

### 3.1 Job queue (`JobQueueStatus`)
```
                 ┌──────────────► SKIPPED
                 │
PENDING_REVIEW ──┼──► APPROVED ──► AUTO_APPLYING ──► APPLIED
                 │        (engine/extension picks)  │
                 │                                   ├─► FAILED_APPLY   (generic error)
                 └──► MANUAL_APPLY ◄─────────────────┘  (CAPTCHA/BLOCKED/LOGIN/2FA)

FILTERED_OUT = enqueued below threshold OR recommendation = SKIP (kept for the record)
```
Rules enforced in `JobQueueService`:
- Enqueue dedupes by `(externalId, platform)`.
- `pickNextApproved` is **pessimistic-locked**, checks the platform rate limit, and
  atomically flips `APPROVED → AUTO_APPLYING` (prevents double-pick).
- `markAutoApplied` creates one `Application{autoApplied=true}` **and** increments
  `PlatformConfig.currentCountToday`.
- `markFailed` routes CAPTCHA/BLOCKED/LOGIN/2FA → `MANUAL_APPLY`, else `FAILED_APPLY`.

### 3.2 Application CRM (`Application.status`)
```
applied → viewed/shortlisted (screening) → interview → offer → (closed)
terminal: rejected · withdrawn
```
The Pipeline UI groups these into: **Applied · Screening · Interview · Offer · Closed**.
Status changes go through `PUT /api/applications/{id}/status` (server-validated, then
UI refresh). No UI-only optimistic status change is treated as truth.

**Known gap (rule 19/20):** the backend does not yet reject *arbitrary* transitions
(e.g. `offer → applied`); it currently accepts any status value. This is documented as
a hardening item, not silently assumed to be enforced.

---

## 4. Scoring — deterministic, backend-authoritative

Three distinct numbers; never conflate them in the UI:

| Number | Source | Meaning |
|---|---|---|
| **ATS score** | `AtsService` | Résumé-to-JD content alignment (keyword coverage). |
| **Match score** | `JobMatchService` (8 factors) | Overall fit including experience/location/notice/salary/company. |
| **Recommendation** | `RecommendationEngine` | Business rules + hard filters on top of the match score. |

**8-factor weights (authoritative, backend):**
`Technical 35 · Experience 20 · Role 10 · Location 10 · WorkMode 5 · Notice 10 ·
Salary 5 · Company 5 = 100`.

The **frontend must not recompute** any of these — it displays backend results only.
`GET /api/match/posting/{id}` returns every sub-score for the explainable breakdown.

**Hard filters (visible reasons required):** excluded company → `SKIP`; experience far
outside range → capped to `LOW_PRIORITY`; missing all required skills → capped to
`REVIEW`.

---

## 5. Centralized thresholds (rule 68) — and current inconsistencies to fix

| Threshold | Value | Where | Note |
|---|---|---|---|
| Strong-match cutoff | **80** | `JobPilotThresholds.STRONG_MATCH_SCORE` + `/api/config/thresholds` | FE reads it via `ConfigService` — no literal |
| Criteria `minMatchScore` default | **65** | `JobPilotThresholds.DEFAULT_MIN_MATCH_SCORE` | JobService/CriteriaService/JobDiscoveryService aligned (was 60 in two spots) |
| Learning gate | **20 applications** | `JobPilotThresholds.LEARNING_MIN_APPLICATIONS` | AnalyticsService references the constant |
| Follow-up age | **5 days** | `JobPilotThresholds.FOLLOW_UP_DAYS` + config endpoint | FE default mirrors it |
| AI daily cap | **20** (`app.ai.daily-limit`) | backend config | Correct |
| Rate limits | Naukri 30 / LinkedIn 15 / Indeed 20 per day; min delay 300–350s | `PlatformConfig` seed | Correct |

**Resolved:** thresholds now have **one backend definition** (`common/JobPilotThresholds`)
exposed at `/api/config/thresholds`, and **one frontend definition**
(`core/config/thresholds.ts`) that syncs from it. The old 65-vs-60 inconsistency and the
frontend's hard-coded `80` are gone.

---

## 6. Analytics formulas (rule 47) — as currently computed

All derived from persisted `Application` / `JobPosting` rows (no random values).

| Metric | Formula | Zero/low-data behavior |
|---|---|---|
| Jobs discovered | count(JobPosting) | 0 |
| Strong matches | count(JobPosting where matchScore ≥ 80) | 0 |
| Applications | count(Application) | 0 |
| Manual applications | count(Application where autoApplied=false) | 0 |
| Response rate | responded ÷ applications | should read **"—/insufficient"** when applications = 0 |
| Interview rate | interviews ÷ applications | same |
| Offer rate | offers ÷ applications | same |
| Average ATS / match | avg over non-null | null when none |
| Learning recommendations | only when applications ≥ 20 | "Insufficient data" below 20 |

**UI accuracy rule (rule 18):** when a denominator is 0, show **"— Not enough data"**,
not "0%". *(Current frontend shows numeric rates from the backend; the "not enough
data" semantic state is a UI hardening item recorded here.)*

**Career Momentum (rule 48):** ✅ **implemented, deterministic & explainable.**
`GET /api/analytics/momentum` (`MomentumService`) computes a 0–100 score over the last
7 days from real rows, with a **documented formula** (each factor capped, total capped 100):
```
applicationsThisWeek × 6, cap 5  → up to 30   (Application.appliedAt in last 7d)
interviewsThisWeek   × 15, cap 2 → up to 30   (Application.interviewDate in last 7d)
responsesThisWeek    × 5,  cap 4 → up to 20   (response-stage, lastUpdated in last 7d)
activeDays           × 3,  cap 7 → up to 20   (distinct days from the activity log)
```
The response returns the score, a band label (Strong/Building/Quiet), and every
**contributing factor with its raw value + points**, so the user can answer "why is my
momentum 55?". When there's no activity it returns `available=false` with a
"Not enough activity" message — never a fabricated number. Surfaced as an explainable
ring + factor breakdown on Today. 3 unit tests.

---

## 7. AI behaviour (rules 14–15)

- Providers: `NoOpAiProvider` (default), `OllamaAiProvider`, `CloudflareAiProvider`,
  selected by `AI_PROVIDER`.
- Deterministic engines (match, ATS, recommendation, résumé selection) are **always
  authoritative** and run with AI off.
- `AiUsageTracker` enforces a daily cap (`app.ai.daily-limit`, default 20); usage is
  recorded per successful call and surfaced at `GET /api/ai/usage`.
- If AI is unavailable, the app continues; the UI should state "AI enrichment
  unavailable — deterministic scoring still active."

---

## 8. Persistence & demo-data truth (rules 39–40, 59)

- **Local profile:** **file-based H2** (`jdbc:h2:file:./data/jobpilot`) → **data now
  survives a backend restart.** Delete `./data/jobpilot.mv.db` to start fresh, or run
  the **`local-mem`** profile for a throwaway in-memory DB.
- **`DataSeeder`** is `@Profile("local")` + `@ConditionalOnProperty(app.seed.enabled,
  matchIfMissing=true)`, and only seeds when the résumé table is empty (so it seeds once
  on a fresh file DB, then your real edits persist).
- **Production** (`prod` profile, Postgres) **does not seed** — `DataSeeder` is
  local-only. Seeded demo applications/analytics **cannot** appear in production.
- To run local dev without demo data: set `app.seed.enabled=false` (or use `local-mem`).

---

## 9. Security truth (rule 58)

- JWT: access (60 min) + refresh (14 d); refresh tokens rejected as bearer; rotation
  via `/api/auth/refresh`.
- Login brute-force lock: 5 attempts / 15 min.
- Bcrypt password; default `admin`/`changeme` — **must be rotated before any public
  deploy** (`/api/auth/hash`).
- All PII endpoints (`/api/candidate/**`, résumés, applications, exports) require auth.
- **No external-platform credentials stored server-side.** Naukri credentials, if used,
  live only in the local engine's `.env`. LinkedIn: no credentials/cookies stored
  anywhere; the extension uses the user's own live session.

---

## 10. API contract report (rules 44–45, 82)

**Every frontend `/api/*` call maps to a real backend endpoint — zero orphans.**
Verified by scanning all Angular services against all backend controllers.

Frontend-consumed groups (all exist server-side): `auth`, `account`, `analytics`,
`ai/usage`, `applications`, `dashboard`, `criteria` (list/get/create/update/toggle),
`resumes`, `resume-engine`, `platform-config`, `match`, `manual-queue`, `jobs`,
`ats`, `queue`, `discovery`, `pack`.

**Backend endpoints that exist but are NOT yet used by the SPA (gap, not orphan):**
- `/api/candidate/**` (résumé parse / profile / skills) — **no UI**
- `/api/target-roles/**` — **no UI** (but discovery depends on target roles!)
- `/api/companies/**` — **no UI**
- `/api/criteria/validate-query` — boolean builder **not surfaced**
- `/api/match/rescore` — defined in service, not wired to a visible control

**Consumed only by non-SPA clients (correct):** `/api/engine/pending`,
`/api/engine/report` (application-engine + Chrome extension).

**Highest-priority product consequence:** because target roles have no UI, a fresh
user cannot configure what discovery searches for from the app alone. Building the
**Candidate Profile** + **Target Roles** screens is the top functional gap to close
(ahead of further visual polish).

---

## 11. Feature matrix (rule 81)

| Feature | Backend | API | Frontend | Persistent | Deterministic | AI optional | User action | Status |
|---|---|---|---|---|---|---|---|---|
| Résumé parsing / candidate profile | ✅ | ✅ | ✅ | ✅ | ✅ | no | upload+confirm | shipped |
| Target roles | ✅ | ✅ | ✅ | ✅ | ✅ | no | create | shipped |
| Criteria (basic) | ✅ | ✅ | ✅ | ✅ | ✅ | no | create | shipped |
| Boolean criteria | ✅ | ✅ | ❌ | ✅ | ✅ | no | build | **backend-only** |
| Discovery (Naukri/LinkedIn/Indeed) | ✅ | ✅ | ✅ | ✅ | ✅ | no | scan | shipped |
| 8-factor match + recommendation | ✅ | ✅ | ✅ | ✅ | ✅ | no | view | shipped |
| ATS score | ✅ | ✅ | ✅ | ✅ | ✅ | enrich | analyze | shipped |
| Queue + auto-apply | ✅ | ✅ | ✅ | ✅ | ✅ | no | approve | shipped |
| Local engine (Naukri/Indeed) | ✅ (app-engine) | ✅ | n/a | ✅ | ✅ | no | run locally | shipped |
| LinkedIn extension | ✅ (ext) | ✅ | n/a | ✅ | ✅ | no | run in Chrome | shipped |
| Manual queue | ✅ | ✅ | ✅ | ✅ | ✅ | no | mark applied | shipped |
| Pipeline / CRM | ✅ | ✅ | ✅ | ✅ | ✅ | no | edit | shipped |
| Application pack | ✅ | ✅ | ✅ | ✅ | ✅ | enrich | generate | shipped |
| Résumé variants + tailoring | ✅ | ✅ | ✅ | ✅ | ✅ | no | select | shipped |
| Analytics + learning (≥20) | ✅ | ✅ | ✅ | ✅ | ✅ | no | view | shipped |
| AI usage cap | ✅ | ✅ | ✅ | ✅ | ✅ | — | configure | shipped |
| Company view | ✅ | ✅ | ✅ (/companies/:name) | ✅ | ✅ | no | view | shipped |
| Interview center | ✅ | ✅ | ✅ (/interviews) | ✅ (stage+date) | ✅ | suggest | prep | shipped |
| Activity feed | ✅ | ✅ | ✅ (Today timeline) | ✅ | ✅ | no | view | shipped |
| Global search | ✅ | ✅ | ✅ (⌘K palette) | ✅ | ✅ | no | search | shipped |
| Momentum metric | ✅ | ✅ | ✅ (Today) | ✅ | ✅ | no | view | shipped |

---

## 12. Prioritized correction backlog (audit → fix → then UI)

**P0 — accuracy/behaviour (do before more UI polish):**
1. ✅ **Reconcile docs** — "current state" header added to `ARCHITECTURE_AUDIT.md`;
   `COMPLETE_PROJECT_REFERENCE` / `USER_GUIDE` match code.
2. ✅ **Candidate Profile + Target Roles screens built** — `/profile` (upload → verify →
   save) and `/target-roles` (priority + skills + ranges) now in the SPA + nav + palette.
   Discovery is now configurable from the app.
3. ✅ **Centralize thresholds** — one backend source (`common/JobPilotThresholds`) +
   `GET /api/config/thresholds`; frontend `core/config/thresholds.ts` (`ConfigService`)
   syncs it on login. `minMatchScore` fallbacks aligned to **65** everywhere
   (JobService 60→65, CriteriaService, JobDiscoveryService); Discovery reads
   `strongMatchScore` from config instead of the literal `80`.
4. ✅ **Zero-data semantics** — Insights + Today show **"— / Not enough data"** for
   interview/response rates when `applications = 0`, instead of a misleading `0%`.
5. ✅ **Application transition validation** — `ApplicationService.validateTransition`
   rejects backward active moves (e.g. `offer → applied`); 6 unit tests.
6. ✅ **Idempotent "mark applied"** — `JobQueueService`/`ManualQueueService.markApplied`
   early-return when already `APPLIED`; no duplicate `Application` rows.

**P1 — product depth:**
7. ✅ Boolean-criteria builder UI — the criteria form now has an advanced boolean rule
   field with live `/api/criteria/validate-query` feedback, AND/OR/NOT/() insert chips,
   and a sample-text tester (MATCH / no-match). Save is blocked on an invalid rule.
8. ✅ **Interview center** — `module/interview`: `GET /api/interviews` (interview/offer
   stage applications, upcoming first) + `GET /api/interviews/{applicationId}/prep`
   (deterministic prep from matched skills + linked résumé — topics, likely/behavioural
   questions, questions-to-ask, checklist). Explicitly labelled "suggested preparation"
   (rule 35), no new entity, no fabrication. Page at `/interviews` with a checklist
   (persisted locally). Company view; global search; activity feed all shipped (below).
   ✅ **Activity feed** — real, persisted `ActivityEvent` log (`module/activity`):
   `GET /api/activity?limit=`; events recorded on discovery scans + application
   create/status-change (rule 56/72). Surfaced as a **Recent activity timeline** on Today.
   ✅ **Global search** — `GET /api/search?q=` (`module/search`): deterministic
   case-insensitive match across postings, applications, résumés, target roles and
   companies, each hit carrying a route. Wired into the **⌘K command palette** as a live
   "Results" group (250 ms debounce). No fabricated suggestions.
   ✅ **Company view** — `GET /api/companies/overview?name=` (`CompanyInsightService`):
   aggregates open roles / applications / interviews / saved for a company from real
   persisted rows. Page at `/companies/:name`, reachable from a global-search Company hit.
9. ✅ File-based H2 for persistent local dev — `local` profile now uses
   `jdbc:h2:file:./data/jobpilot` (survives restart); throwaway `local-mem` profile
   added for a clean DB. Prod (Postgres) unchanged; `DataSeeder` still local-only and
   seeds an empty DB only once.

**P2 — visual maturity:** ✅ **substantially done.** Bespoke "Desk" compositions:
Today (agenda + momentum + activity), Discover (opportunity feed), Pipeline (progression
map), Settings (config workspace), **Insights (decision intelligence — story headline,
"what's working" findings, bottleneck callout, evidence tables + funnel)**, **Résumés
(studio list)**, **Criteria (strategy list with boolean-rule badge)**. Review/Queue uses a
functional 2-tab workbench. Remaining polish is incremental, not structural.

---

## 13. Definition of done (rule 88) — current status

| Gate | Status |
|---|---|
| Every frontend API maps to a real backend endpoint | ✅ verified |
| No fake/random business data at runtime | ✅ (analytics from persisted rows; no `Math.random` in business values) |
| Deterministic scoring authoritative; AI optional | ✅ |
| Learning gated at ≥20 | ✅ |
| Demo data only in local profile | ✅ |
| Docs describe the actual system | ✅ after this pass (audit header + this spec) |
| Candidate/target-roles reachable in UI | ❌ **open (P0-2)** |
| Zero dead buttons | ⚠️ mostly; verify after profile/target-role screens land |
| Application state machine enforced server-side | ❌ **open (P0-5)** |

---

*This spec is authoritative. Update it in the same commit as any behaviour change so it
never drifts from the code again.*

