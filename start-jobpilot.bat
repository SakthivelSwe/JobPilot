@echo off
echo =========================================
echo       Starting JobPilot Locally...
echo =========================================
echo.

echo [1/3] Starting Backend (Spring Boot)...
start "JobBot Backend" cmd /k "cd job-bot-backend && mvn spring-boot:run"

echo [2/3] Starting Frontend (Angular)...
start "JobBot Frontend" cmd /k "cd job-bot-frontend && npm start"

echo [3/3] Starting Application Engine (Node.js)...
start "JobBot Application Engine" cmd /k "cd application-engine && npm run dev"

echo.
echo All services are booting up in separate windows!
echo Please wait about 30 seconds for the backend to fully start.
echo.
pause
