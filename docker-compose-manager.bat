@echo off
title Docker Compose Manager

echo ==========================
echo 1. Start (no build)
echo 2. Start (with build)
echo 3. Stop and remove containers
echo ==========================

choice /c 123 /n /m "Choose (1-3): "

if errorlevel 3 goto stop
if errorlevel 2 goto build
if errorlevel 1 goto start

:start
echo Starting containers...
docker compose --env-file .env.local -f docker-compose.local.yml up -d
goto end

:build
echo Building and starting containers...
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
goto end

:down
echo Stopping and removing containers...
docker compose --env-file .env.local -f docker-compose.local.yml down
goto end

:end
echo Done!
pause