@echo off
setlocal
pushd "%~dp0" || exit /b 1

if not exist ".venv\Scripts\python.exe" (
    echo Python-Projektumgebung fehlt. Siehe docs\entwicklung.md.
    popd
    exit /b 1
)

set "PYTHONIOENCODING=utf-8"
".venv\Scripts\python.exe" "scripts\sonar\sonar.py" scan %*
set "sonarResult=%errorlevel%"
popd
exit /b %sonarResult%
