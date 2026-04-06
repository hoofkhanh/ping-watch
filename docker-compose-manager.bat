@echo off
title Docker Compose Manager

echo ==========================
echo 1. Start (no build)
echo 2. Build only
echo 3. Start (with build)
echo 4. Stop and remove containers
echo ==========================

choice /c 1234 /n /m "Choose (1-4): "

if errorlevel 4 goto stop
if errorlevel 3 goto buildrun
if errorlevel 2 goto buildonly
if errorlevel 1 goto start

:start
echo Starting containers...
docker compose --env-file .env.local -f docker-compose.local.yml up -d
goto end

:buildonly
echo Building containers only...
docker compose --env-file .env.local -f docker-compose.local.yml build
goto end

:buildrun
echo Building and starting containers...
docker compose --env-file .env.local -f docker-compose.local.yml up -d --build
goto end

:stop
echo Stopping and removing containers...
docker compose --env-file .env.local -f docker-compose.local.yml down
goto end

:end
echo Done!
pause