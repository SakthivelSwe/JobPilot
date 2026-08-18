# application-engine (JobPilot local Playwright engine)

Polls the JobPilot backend for **APPROVED** Naukri/Indeed jobs and applies to
them inside a **visible** Chromium window running on your own machine. This is
the piece you run manually when you're at your desk — it never runs on Render,
it never uses stealth to impersonate a human, and **it never touches LinkedIn**
(LinkedIn is handled by the Chrome Extension in your real browser session).

## Install

```powershell
cd application-engine
npm install
npx playwright install chromium
copy .env.example .env
# edit .env — set NAUKRI_EMAIL / NAUKRI_PASSWORD if you want the login-wall path
npm run dev
```

## What it does per cycle

1. Poll `GET /api/engine/pending?platform=NAUKRI` (then `INDEED`).
2. If a job comes back, first check `GET /api/platform-config/{platform}` locally.
3. Open the job in Chromium (visible, persistent profile → cookies survive).
4. Run the platform's applicator:
   - Skip if "Already Applied" is on the page.
   - Return `CAPTCHA` if a reCAPTCHA iframe is present → backend routes to Manual queue.
   - Fill the Naukri login wall if credentials are configured.
   - Screenshot the final page under `./logs/screenshots/`.
5. `POST /api/engine/report` with the outcome.
6. Sleep `MIN_INTER_APPLY_MS` – `MAX_INTER_APPLY_MS` (defaults 5–8 min) before the next apply.

## Design notes

- **Never headless.** `headless: false` is the whole safety net.
- **No stealth override of the profile.** We do use `playwright-extra` +
  `puppeteer-extra-plugin-stealth` because bot-flag DOM markers alone will get
  the flow bounced before a human can even review it — but the persistent
  profile carries real cookies/history and no fingerprint spoofing is
  configured.
- **Rate limits enforced twice.** Once client-side (the backend's
  `PlatformConfig`), once server-side inside `pickNextApproved`.
- **Server owns state.** This process holds nothing durable except the Chromium
  profile directory. Kill it any time.

## Environment

See `.env.example`. Key values:

| Var | Purpose |
|---|---|
| `API_BASE_URL` | JobPilot backend URL |
| `API_USERNAME` / `API_PASSWORD` | Used to log in and refresh the JWT on 401 |
| `API_TOKEN` | Optional pre-issued JWT (skips login) |
| `PLATFORMS` | Which platforms to poll; default `NAUKRI,INDEED` |
| `POLL_INTERVAL_MS` | Poll frequency (default 30 s) |
| `MIN_INTER_APPLY_MS` / `MAX_INTER_APPLY_MS` | Sleep between successful applies |
| `CHROME_PROFILE_DIR` | Persistent Chromium profile path |
| `NAUKRI_EMAIL` / `NAUKRI_PASSWORD` | Only used when a Naukri login wall appears |

## Not deployed

This engine is **not part of the Render deployment**. Run it on your ASUS
laptop whenever you're at your desk. Everything works cold too — if you're not
running the engine, approved Naukri/Indeed jobs simply pile up in the queue
until you start it.

