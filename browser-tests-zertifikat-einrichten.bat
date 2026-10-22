@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0browser-tests\run.ps1" -SetupHttps
if errorlevel 1 (
  echo.
  echo Browser-Test-HTTPS konnte nicht eingerichtet werden.
  pause
  exit /b 1
)
echo.
echo Browser-Test-HTTPS ist eingerichtet. Nachtlaeufe benoetigen keine Zertifikatsdialoge.
pause
