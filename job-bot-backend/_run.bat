@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
cd /d "%~dp0"
"%JAVA_HOME%\bin\java.exe" -jar "target\job-bot-backend-0.0.1-SNAPSHOT.jar" > target\run.log 2>&1

