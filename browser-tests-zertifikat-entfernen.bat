@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0browser-tests\run.ps1" -RemoveHttps
if errorlevel 1 (
  echo.
  echo Browser-Test-Zertifikatsvertrauen konnte nicht entfernt werden.
  pause
  exit /b 1
)
echo.
echo Eigenes Browser-Test-Zertifikatsvertrauen wurde entfernt.
pause
