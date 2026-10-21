@echo off
setlocal
title Lokaler HTTPS-Server
pushd "%~dp0" || exit /b 1

if not exist "data\https\tools\caddy.exe" (
    echo Caddy fehlt: data\https\tools\caddy.exe
    echo Bitte Caddy v2.10.2 dort ablegen. Siehe docs\entwicklung.md.
    set "result=1"
    goto :finish
)

echo HTTPS-Zugang: https://localhost:8443
echo Die Anwendung in IntelliJ mit dem Profil https starten.
echo Dieses Fenster offen lassen. Strg+C beendet Caddy.
echo Beim ersten Start danach https-zertifikat-einrichten.bat ausfuehren.
echo.
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0scripts\https\ide.ps1" -CaddyPath "%~dp0data\https\tools\caddy.exe"
set "result=%errorlevel%"

:finish
if not "%result%"=="0" echo HTTPS-Server konnte nicht gestartet werden oder wurde beendet.
popd
pause
exit /b %result%
