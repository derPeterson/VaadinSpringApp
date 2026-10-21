@echo off
setlocal DisableDelayedExpansion

rem ============================================================
rem SonarQube scan for VaadinSpringApp
rem Place this file next to pom.xml.
rem Usage:
rem   sonar-scan.bat         Full Maven build + tests + SonarQube
rem   sonar-scan.bat quick   SonarQube only (requires fresh build)
rem ============================================================

pushd "%~dp0" || (
    echo [ERROR] Could not open the project directory.
    exit /b 1
)

if not exist "pom.xml" (
    echo [ERROR] No pom.xml found next to this BAT file.
    popd
    exit /b 1
)

set "SONAR_HOST_URL=http://localhost:9000"
set "SONAR_PROJECT_KEY=vaadin-spring-app"

rem Prefer the project's Maven wrapper if present.
if exist "mvnw.cmd" (
    set "MVN_CMD=%CD%\mvnw.cmd"
) else (
    where mvn.cmd >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Maven was not found. Install Maven or add mvnw.cmd.
        popd
        exit /b 1
    )
    set "MVN_CMD=mvn.cmd"
)

rem Maven scanner reads SONAR_TOKEN from the environment.
rem Ask only if the variable is missing. Hide token input with PowerShell.
if not defined SONAR_TOKEN (
    echo [INFO] No SONAR_TOKEN set. Please enter your SonarQube User Token.
    for /f "usebackq delims=" %%T in (`powershell.exe -NoProfile -Command "$s=Read-Host 'SonarQube User Token' -AsSecureString; $p=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($s); try {[Runtime.InteropServices.Marshal]::PtrToStringBSTR($p)} finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p)}"`) do set "SONAR_TOKEN=%%T"
)

if not defined SONAR_TOKEN (
    echo [ERROR] No token entered. Scan cancelled.
    popd
    exit /b 1
)

echo.
echo [INFO] Project: %SONAR_PROJECT_KEY%
echo [INFO] Server:  %SONAR_HOST_URL%
echo [INFO] Source:  %CD%
echo [INFO] Token:   available (value not displayed)
echo.

if /i "%~1"=="quick" goto QUICK_SCAN
if not "%~1"=="" (
    echo [ERROR] Unknown argument "%~1".
    echo Usage: sonar-scan.bat [quick]
    popd
    exit /b 2
)

echo [INFO] Running full build, tests, coverage and SonarQube upload...
call "%MVN_CMD%" -B clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar "-Dsonar.projectKey=%SONAR_PROJECT_KEY%" "-Dsonar.host.url=%SONAR_HOST_URL%"
goto SCAN_RESULT

:QUICK_SCAN
if not exist "target\classes" (
    echo [ERROR] target\classes is missing. Run sonar-scan.bat without 'quick' first.
    popd
    exit /b 1
)
echo [WARN] Quick mode does not build or run tests.
echo [WARN] Existing classes and coverage may be outdated.
echo [INFO] Running SonarQube scan and upload...
call "%MVN_CMD%" -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar "-Dsonar.projectKey=%SONAR_PROJECT_KEY%" "-Dsonar.host.url=%SONAR_HOST_URL%"

:SCAN_RESULT
set "SCAN_EXIT=%ERRORLEVEL%"
echo.
if not "%SCAN_EXIT%"=="0" (
    echo [ERROR] Maven/SonarQube failed with exit code %SCAN_EXIT%.
    echo [HINT] HTTP 401: check SONAR_TOKEN. Connection errors: check Docker and port 9000.
    popd
    exit /b %SCAN_EXIT%
)

echo [OK] SonarQube scan uploaded successfully.
echo [INFO] Dashboard: %SONAR_HOST_URL%/dashboard?id=%SONAR_PROJECT_KEY%
echo [INFO] Wait for the server to finish processing before checking new issues.
popd
exit /b 0
