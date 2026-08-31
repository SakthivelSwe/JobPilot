# JobPilot

An automated job application and discovery bot.

## 🚀 How to Run Locally (One-Click)

To run the entire application with a single click, simply double-click the **`start-jobpilot.bat`** file in the root directory of this project.

This script will automatically open **3 separate terminal windows** and start the required services in parallel:
1. **Backend** (`job-bot-backend`) — runs on `http://localhost:8080`
2. **Frontend** (`job-bot-frontend`) — runs on `http://localhost:4200`
3. **Application Engine** (`application-engine`) — connects the bot to your browser

*Note: Allow about 30-40 seconds for the backend to fully initialize before the engine starts processing jobs.*

---

### Manual Start (Alternative)
If you prefer to start them manually in your own terminal (like VS Code), open 3 separate terminal tabs and run:

**1. Backend:**
```bash
cd job-bot-backend
mvn spring-boot:run
```

**2. Frontend:**
```bash
cd job-bot-frontend
npm start
```

**3. Application Engine:**
```bash
cd application-engine
npm run dev
```
