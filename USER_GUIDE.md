# 📖 JobPilot — Complete User Guide

> **What is JobPilot?**
> Your personal job-search command center. It discovers jobs from Naukri, LinkedIn
> and Indeed, scores each one against your résumé, picks the best résumé for each
> job, prepares your application pack, and — for jobs it can — **applies for you
> automatically** while you review everything in a clean dashboard.
>
> You stay in control: nothing is submitted without a job first sitting in your
> **Queue**, and you decide what gets approved.

---

## Table of contents
1. [The big picture — how the 4 parts fit together](#1-the-big-picture)
2. [First-time setup (run everything locally)](#2-first-time-setup)
3. [Logging in](#3-logging-in)
4. [The daily workflow (start to finish)](#4-the-daily-workflow)
5. [Every page explained](#5-every-page-explained)
6. [How auto-apply actually works](#6-how-auto-apply-actually-works)
7. [Setting up the auto-apply engine (Naukri/Indeed)](#7-setting-up-the-auto-apply-engine)
8. [Setting up the Chrome extension (LinkedIn)](#8-setting-up-the-chrome-extension)
9. [Understanding the job statuses](#9-understanding-the-job-statuses)
10. [Safety, rate limits & good practice](#10-safety-rate-limits--good-practice)
11. [Troubleshooting](#11-troubleshooting)
12. [Quick reference card](#12-quick-reference-card)

---

## 1. The big picture

JobPilot is made of **four parts**. You don't need all four to start — the first
two are enough to browse, score and manage jobs.

```
┌──────────────────────────────────────────────────────────────────────┐
│  1. FRONTEND  (the website you look at)                               │
│     http://localhost:4200                                             │
│     Dashboard · Discovery · Queue · Manual · Pipeline · Analytics…    │
└───────────────────────────────┬──────────────────────────────────────┘
                                 │  talks to
┌───────────────────────────────▼──────────────────────────────────────┐
│  2. BACKEND  (the brain — scoring, matching, the database)            │
│     http://localhost:8080                                             │
│     • Finds jobs on Naukri / LinkedIn / Indeed                        │
│     • Scores each job vs your résumé (ATS + 8-factor match)           │
│     • Keeps the queue of jobs waiting for your approval               │
└──────────────┬──────────────────────────────────┬────────────────────┘
               │ approved Naukri/Indeed jobs       │ approved LinkedIn jobs
┌──────────────▼────────────────┐   ┌──────────────▼────────────────────┐
│  3. APPLICATION-ENGINE         │   │  4. CHROME EXTENSION               │
│     (runs on YOUR laptop)      │   │     (runs in YOUR Chrome)          │
│     Opens a real browser and   │   │     Clicks "Easy Apply" on         │
│     applies to Naukri/Indeed   │   │     LinkedIn for you               │
└────────────────────────────────┘   └────────────────────────────────────┘
```

**In plain words:**
- The **backend** is the smart part. It finds jobs and figures out which are worth
  applying to.
- The **frontend** is what you look at and click.
- The **application-engine** is a helper program on your own computer that does the
  actual "clicking apply" on **Naukri and Indeed**.
- The **Chrome extension** does the "clicking apply" for **LinkedIn**, right inside
  your own Chrome browser.

> **Why two separate "apply" helpers?** LinkedIn's rules don't allow outside
> programs to log into your account. So LinkedIn applies happen *inside your own
> browser* (the extension), while Naukri/Indeed applies happen in a separate
> browser window the engine controls. This keeps everything within the rules.

---

## 2. First-time setup

You already have everything installed. Here's how to start each part.

### Part A — Backend (required)

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "C:\AI projects\dummby-project\job-bot-backend"
mvn spring-boot:run
```
Wait until you see `Started JobBotApplication`. The backend is now at
**http://localhost:8080**.

> ✅ Check it's working: open http://localhost:8080/actuator/health — you should
> see `{"status":"UP"}`.

### Part B — Frontend (required)

Open a **new** terminal:

```powershell
cd "C:\AI projects\dummby-project\job-bot-frontend"
npm start
```
Wait until you see `Local: http://localhost:4200`. Open that address in your
browser.

### Part C — Application-engine (optional — only for auto-applying to Naukri/Indeed)

See [Section 7](#7-setting-up-the-auto-apply-engine).

### Part D — Chrome extension (optional — only for auto-applying to LinkedIn)

See [Section 8](#8-setting-up-the-chrome-extension).

---

## 3. Logging in

1. Go to **http://localhost:4200**
2. Username: **`admin`**
3. Password: **`changeme`**

> 🔒 **Change the password before you ever put this on the internet.** For local
> use on your own laptop, the default is fine.

---

## 4. The daily workflow

Here's the whole journey, start to finish. Do steps 1–4 **once**; repeat 5–8
**daily**.

```
  (one-time setup)
  1. Add your résumé            →  Resumes page
  2. Build your profile         →  (résumé upload auto-fills skills & experience)
  3. Set your criteria          →  Criteria page (Java, Chennai, 2-4 yrs, ₹8-12L)
  4. Add target roles           →  what job titles you want

  (every day)
  5. Run discovery              →  Discovery page → "Scan now"
  6. Review found jobs          →  Queue page → Approve / Skip / Manual
  7. Let the engine apply       →  application-engine + Chrome extension do the work
  8. Track your applications    →  Pipeline page (Kanban board)
```

### Step-by-step the first time

**① Add a résumé** — Go to **Resumes → New**. Either upload a PDF/DOCX or paste
your résumé text. JobPilot reads it and pulls out your skills, experience and
projects automatically. Review what it detected and save.

**② Set your criteria** — Go to **Criteria → New**. Fill in:
- Keywords: `Java, Spring Boot, Kafka, AWS`
- Locations: `Chennai, Bangalore, Remote`
- Experience: `2` to `4` years
- Minimum match score: `65` (jobs below this are filtered out)

**③ Add target roles** — Tell JobPilot the exact job titles you're hunting, e.g.
"Java Backend Developer", "Java Full Stack Developer". Discovery searches Naukri /
LinkedIn / Indeed using these titles.

**④ Run a scan** — Go to **Discovery** and click **"Scan now"**. JobPilot searches
all three platforms, scores each job against your profile, and drops the good ones
into your **Queue**.

**⑤ Review the queue** — Go to **Queue**. Every job shows a **match score ring**
(0–100), the platform, and green/red skill chips (what matches, what's missing).
For each job you choose:
- **✓ Approve** → the engine will auto-apply
- **Manual** → you'll apply yourself (goes to the Manual page)
- **Skip** → not interested

Or click **"Approve all 80+"** to bulk-approve every strong match.

**⑥ Applies happen automatically** — If the application-engine and/or Chrome
extension are running, approved jobs get applied to in the background, one every
5–8 minutes.

**⑦ Track progress** — Go to **Pipeline** (the Kanban board) to see every
application move through: Applied → Screening → Interview → Offer.

---

## 5. Every page explained

| Page | What it's for |
|------|---------------|
| **Dashboard** | Your home screen. Shows today's numbers: new matches, strong matches, jobs in queue, applications, interview rate. Start here each day. |
| **Discovery** | Where you trigger a job scan. Shows which sources are active (Naukri/LinkedIn/Indeed), coverage stats, and the top opportunities found. Click **"Scan now"** to search. |
| **Queue** ⭐ | The heart of the app. Two tabs: **Pending Review** (new jobs waiting for your Approve/Skip/Manual decision) and **Auto-Applying** (jobs currently being applied to). Jobs scoring 85+ get a glowing "STRONG MATCH" badge. |
| **Manual Apply** | Jobs the bot couldn't apply to automatically (LinkedIn, or ones that hit a CAPTCHA). Each has an "Open Job" button — you apply yourself, then click "Applied ✓". |
| **Pipeline** | Your application tracker (Kanban board). Drag cards between columns: Applied → Screening → Interview → Offer → Rejected. |
| **Resumes** | Manage your résumé(s). JobPilot keeps 4 "angles" of your résumé (Backend / Full Stack / Microservices / Cloud) and auto-picks the best one for each job. |
| **Criteria** | Your job filters: keywords, locations, experience, salary, minimum score. Supports advanced boolean logic like `Java AND (Kafka OR Microservices) AND NOT Intern`. |
| **Analytics** | Which résumé/role/location is getting you the most interviews. After 20+ applications, it starts giving you recommendations. |
| **Settings** | Turn platforms on/off, set daily apply limits and delays, manage AI usage, and export/delete your data. |

---

## 6. How auto-apply actually works

Here's what happens behind the scenes when you approve a job:

```
You click "Approve" on a job
          │
          ▼
Job status changes: PENDING_REVIEW → APPROVED
          │
          ├─── If it's a NAUKRI or INDEED job ───────────────┐
          │                                                    ▼
          │                          The application-engine (on your laptop)
          │                          polls the backend every 30 seconds,
          │                          sees the approved job, opens a browser,
          │                          and applies. Status → AUTO_APPLYING → APPLIED
          │
          └─── If it's a LINKEDIN job ───────────────────────┐
                                                               ▼
                                     The Chrome extension polls the backend,
                                     opens the job in your LinkedIn tab,
                                     clicks "Easy Apply". Status → APPLIED
```

**Important safety behaviours:**
- The engine **checks your daily limit first** (e.g. max 15 LinkedIn/day). If you've
  hit it, it stops for the day.
- It waits **5–8 minutes between each apply** so it never looks like spam.
- If a job hits a **CAPTCHA** or needs info the bot can't fill, it's moved to your
  **Manual Apply** page for you to finish by hand.
- Every successful apply automatically creates a card on your **Pipeline** board.

---

## 7. Setting up the auto-apply engine

This is the helper that applies to **Naukri and Indeed**. It runs on your own
laptop in a visible browser window.

### One-time install

```powershell
cd "C:\AI projects\dummby-project\application-engine"
npm install
npx playwright install chromium
copy .env.example .env
```

### Configure it

Open the `.env` file and fill in:

```
API_BASE_URL=http://localhost:8080
API_USERNAME=admin
API_PASSWORD=changeme

# Only needed if Naukri shows a login wall during apply:
NAUKRI_EMAIL=your-naukri-email@example.com
NAUKRI_PASSWORD=your-naukri-password

# Which platforms this engine applies to:
PLATFORMS=NAUKRI,INDEED
```

### Run it

```powershell
npm run dev
```

A Chromium window opens. Leave it running while you work. It will:
1. Poll the backend every 30 seconds for approved Naukri/Indeed jobs.
2. Open each job and apply.
3. Wait 5–8 minutes, then do the next one.

> ✅ Check it's alive: open http://localhost:3001/health — you'll see how many jobs
> it has applied to today.

> ⚠️ **Never close the browser window it opens** while it's working, and **don't run
> it headless** — running visibly is intentional and keeps things above-board.

---

## 8. Setting up the Chrome extension

This is the helper that applies to **LinkedIn**, inside your own Chrome.

### Install

1. Open Chrome and go to `chrome://extensions`
2. Turn on **Developer mode** (top-right toggle)
3. Click **Load unpacked**
4. Select the folder: `C:\AI projects\dummby-project\chrome-extension`

### Configure

1. Click the JobPilot icon in your Chrome toolbar (puzzle-piece menu → pin it).
2. In the popup, enter:
   - **Backend URL:** `http://localhost:8080`
   - **JWT token:** (get this by logging into the app — see below)
3. Click **Save**. The dot turns green when connected.

**How to get your JWT token:**
- Easiest: open the browser DevTools (F12) on the JobPilot site → Application →
  Local Storage → copy the `access` token value.
- Or use the login API directly and copy the `token` from the response.

### Use it

1. Keep a **LinkedIn Jobs tab open** in the same Chrome (e.g. linkedin.com/jobs).
2. Make sure you're **logged into LinkedIn** normally.
3. The extension polls every 30 seconds. When you approve a LinkedIn job in the
   Queue, the extension opens it and clicks Easy Apply for you.
4. The popup shows today's LinkedIn apply count. Use **Pause** to stop anytime.

> If a LinkedIn application needs extra info the extension can't fill, it stops and
> sends that job to your **Manual Apply** page.

---

## 9. Understanding the job statuses

Every job in the queue has a status. Here's what each means:

| Status | Meaning | What you do |
|--------|---------|-------------|
| **PENDING_REVIEW** | New job found, waiting for your decision | Approve / Skip / Manual |
| **APPROVED** | You approved it; waiting for the engine to apply | Nothing — it's automatic |
| **AUTO_APPLYING** | The engine is applying right now | Nothing — wait |
| **APPLIED** | Successfully applied! | Track it on Pipeline |
| **FAILED_APPLY** | The apply failed (some technical issue) | Retry or send to Manual |
| **MANUAL_APPLY** | Needs you to apply by hand (LinkedIn, or hit a CAPTCHA) | Go to Manual page, apply, mark done |
| **SKIPPED** | You skipped it | Nothing |
| **FILTERED_OUT** | Score too low, or marked "Skip" by the recommender | Nothing (kept for the record) |

---

## 10. Safety, rate limits & good practice

JobPilot is deliberately careful. A few things worth knowing:

**Rate limits (set in Settings):**
- Naukri: 30 applies/day (default)
- LinkedIn: 15 applies/day (default)
- Indeed: 20 applies/day (default)
- Minimum 5 minutes between each apply

These defaults are conservative on purpose. **Keep LinkedIn to ~15/day** — it's the
strictest platform.

**What JobPilot never does:**
- It never stores your LinkedIn password or session cookies.
- It never applies without a job first being **approved by you** in the Queue.
- It never hides what it's doing — the Naukri/Indeed browser is always visible.

**Good practice:**
- Review the Queue yourself instead of always "Approve all". Quality > quantity.
- Run the engine only when you're at your desk, so you can spot anything odd.
- Check the **Manual Apply** page daily — the best jobs (LinkedIn) often live there.

---

## 11. Troubleshooting

**The website won't load (localhost:4200)**
- Make sure `npm start` is still running in the frontend terminal.
- Wait a full minute after starting — Angular takes time to compile the first time.

**"Waking JobPilot…" or API errors**
- The backend isn't running or is still starting. Check
  http://localhost:8080/actuator/health shows `{"status":"UP"}`.

**Login fails**
- Username `admin`, password `changeme` (all lowercase). If you changed the
  password, use your new one.

**Discovery scan finds 0 jobs**
- You need at least one **Target Role** set (that's what it searches for).
- Live scraping needs a real internet connection. Job sites also change their page
  layout occasionally, which can temporarily break the parser.

**The engine isn't applying**
- Is `npm run dev` running in the application-engine folder?
- Check http://localhost:3001/health.
- Did you actually **approve** jobs in the Queue? It only applies to APPROVED jobs.
- Have you hit the daily rate limit? Check Settings.

**Chrome extension isn't doing anything**
- Is a LinkedIn Jobs tab open and are you logged in?
- Is the token in the popup still valid? (Tokens expire — paste a fresh one.)
- Is it paused? Check the popup.

**Data disappeared after restarting the backend**
- Locally, JobPilot uses an **in-memory database (H2)** that resets on restart.
  This is normal for local use. For permanent storage, deploy with the `prod`
  profile + a real PostgreSQL database (see `DEPLOY.md`).

---

## 12. Quick reference card

**Start everything:**
```powershell
# Terminal 1 — backend
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
cd "C:\AI projects\dummby-project\job-bot-backend"; mvn spring-boot:run

# Terminal 2 — frontend
cd "C:\AI projects\dummby-project\job-bot-frontend"; npm start

# Terminal 3 — Naukri/Indeed engine (optional)
cd "C:\AI projects\dummby-project\application-engine"; npm run dev
```

**Addresses:**
| What | URL |
|------|-----|
| The app | http://localhost:4200 |
| Backend health | http://localhost:8080/actuator/health |
| Engine health | http://localhost:3001/health |
| Database console | http://localhost:8080/h2-console |

**Login:** `admin` / `changeme`

**Stop everything:**
```powershell
Get-Process java, node | Stop-Process -Force
```

**The 8-step routine:**
1. Add résumé → 2. Set criteria → 3. Add target roles → 4. Scan (Discovery) →
5. Review (Queue) → 6. Approve → 7. Engine applies → 8. Track (Pipeline)

---

*JobPilot finds the right jobs, understands why they fit, picks your best résumé,
and applies for you — while keeping you in the driver's seat. Happy hunting! 🚀*

