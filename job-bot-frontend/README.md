# JobPilot — Frontend (Angular 17)

Standalone components, lazy-loaded routes, native drag-and-drop Kanban, zero heavy
UI dependencies. Talks to the Spring Boot backend at `environment.apiUrl`.

## Run locally

```powershell
# 1. Start the backend first (separate terminal)
cd ..\job-bot-backend
mvn spring-boot:run

# 2. Start the frontend
cd ..\job-bot-frontend
npm start
```

- App: http://localhost:4200
- Backend API: http://localhost:8080 (see `src/environments/environment.ts`)

CORS is already enabled on the backend for `/api/**`, so no proxy is needed.

## Pages

| Route | Purpose |
|-------|---------|
| `/dashboard` | Stat cards + platform/status breakdowns |
| `/resumes` | Resume profiles (list + form) |
| `/criteria` | Search criteria (list + form) |
| `/jobs` | Imported jobs with ATS scores; score against a criteria; mark applied |
| `/jobs/import` | Paste a job URL + JD -> import & score |
| `/applications` | Kanban CRM (drag to change status) |

## Build for production (Cloudflare Pages)

```powershell
npm run build
```

- Build command: `ng build --configuration production`
- Output directory: `dist/job-bot-frontend/browser`
- `src/_redirects` (`/* /index.html 200`) is bundled for SPA routing.
- `environment.prod.ts` points `apiUrl` at the deployed backend.
