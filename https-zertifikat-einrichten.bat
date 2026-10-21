@echo off
setlocal
title Lokales HTTPS-Zertifikat einrichten
pushd "%~dp0" || exit /b 1

if not exist "data\https\ide\pki\authorities\local\root.crt" goto :missingCertificate
if not exist "data\https\ide\pki\authorities\local\intermediate.crt" goto :missingCertificate

echo Das lokale Caddy-Zertifikat wird fuer deinen Windows-Benutzer als vertrauenswuerdig eingerichtet.
echo Den Windows-Zertifikatsdialog bitte bestaetigen.
echo Das Vertrauen bleibt bis zum ausdruecklichen Entfernen bestehen.
echo.
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0scripts\https\trust.ps1" -Action Add -Certificate "%~dp0data\https\ide\pki\authorities\local\root.crt" -Intermediate "%~dp0data\https\ide\pki\authorities\local\intermediate.crt"
set "result=%errorlevel%"
if "%result%"=="0" echo Zertifikatsvertrauen eingerichtet. Zugang: https://localhost:8443
if not "%result%"=="0" echo Zertifikatsvertrauen wurde nicht erfolgreich eingerichtet. Bitte die Meldung oben beachten.
goto :finish

:missingCertificate
echo Zertifikat fehlt. Zuerst https-server-starten.bat starten und laufen lassen.
set "result=1"

:finish
popd
pause
exit /b %result%
