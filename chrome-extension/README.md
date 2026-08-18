# JobPilot LinkedIn Apply (Chrome Extension)

Handles LinkedIn Easy Apply for jobs the JobPilot backend has marked APPROVED,
inside **your own** Chrome session (no headless, no session-cookie storage,
no server-side automation). LinkedIn is intentionally not touched by the
Node.js `application-engine`; this extension is the only surface that applies
on LinkedIn.

## Install

1. In `chrome://extensions`, enable **Developer mode**.
2. **Load unpacked** → point to this `chrome-extension/` folder.
3. Click the toolbar icon → paste:
   - **Backend URL** (e.g. `http://localhost:8080` or your Render URL)
   - **JWT token** from `POST /api/auth/login`
4. Keep at least one **linkedin.com/jobs/…** tab open while the extension is
   running.

## How it works

- The service worker (`background/worker.js`) sets a Chrome alarm every 30 s.
- On each tick it calls `GET /api/engine/pending?platform=LINKEDIN`.
- If a job comes back, it opens (or reuses) a LinkedIn jobs tab at the job URL
  and messages the content script (`content/linkedin.js`).
- The content script clicks **Easy Apply**, walks the modal via Next/Continue
  buttons, and clicks **Submit application**.
- The result is reported back to `POST /api/engine/report`, which either:
  - marks the queue entry **APPLIED** (and creates a `autoApplied=true`
    Application in the Kanban CRM), or
  - marks it **MANUAL_APPLY** if the modal needs a human input.

## Guardrails

- Pause any time from the popup — no polling until resumed.
- Backend enforces `PlatformConfig` daily limit / delay independently.
- The extension never stores your LinkedIn credentials, cookies, or session
  data; it operates only on your already-logged-in Chrome session.

## Icons

Drop 16×16, 48×48, 128×128 PNGs into `chrome-extension/icons/` if you want a
custom toolbar icon (optional — Chrome will use a placeholder otherwise).

