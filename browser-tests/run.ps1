param(
    [switch]$Headed,
    [ValidateSet('all', 'infrastructure', 'authentication', 'registration', 'verification', 'password-reset', 'account-lifecycle', 'presentation', 'token-configuration', 'user-management', 'user-filters')]
    [string]$Suite = 'all',
    [ValidateSet('de', 'en')]
    [string]$Language = 'de',
    [switch]$NoBuild,
    [switch]$FailureProbe,
    [switch]$ReportingTests,
    [switch]$Report,
    [string]$ReportPath,
    [switch]$InstallBrowsers,
    [string]$Trace
)
$ErrorActionPreference = 'Stop'
if ($ReportPath) { $Report = $true }
$modulePath = $PSScriptRoot
$repoPath = Split-Path $modulePath -Parent
$pomPath = Join-Path $modulePath 'pom.xml'
$wrapperPath = Join-Path $repoPath 'mvnw.cmd'
$targetPath = Join-Path $modulePath 'target'
# This file only bootstraps Java. Docker, test execution and reports are handled in Java.
& $wrapperPath --version
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
# Rebuild the shared dependency before compiling either helper tests or the runner.
& $wrapperPath -f (Join-Path $repoPath 'shared/pom.xml') clean install
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($ReportingTests) {
    & $wrapperPath -f $pomPath test
    exit $LASTEXITCODE
}
& $wrapperPath -f $pomPath test-compile dependency:build-classpath "-Dmdep.outputFile=$targetPath/classpath.txt"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$javaPath = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$classpath = (Join-Path $targetPath 'test-classes') + ';' + (Get-Content (Join-Path $targetPath 'classpath.txt') -Raw).Trim()
if ($InstallBrowsers) {
    & $javaPath '-cp' $classpath 'com.microsoft.playwright.CLI' 'install' 'chromium'
    exit $LASTEXITCODE
}
if ($Trace) {
    & $javaPath '-cp' $classpath 'com.microsoft.playwright.CLI' 'show-trace' $Trace
    exit $LASTEXITCODE
}
$javaArguments = @('-Dfile.encoding=UTF-8', '-cp', $classpath)
if (-not $Report) { $javaArguments += "-javaagent:$targetPath/tools/aspectjweaver.jar" }
$javaArguments += @('de.derpeterson.browsertests.BrowserTestRunner', $modulePath)
if (-not $Report) { $javaArguments += @('--suite', $Suite, '--language', $Language) }
if ($Headed) { $javaArguments += '--headed' }
if ($NoBuild) { $javaArguments += '--no-build' }
if ($FailureProbe) { $javaArguments += '--failure-probe' }
if ($Report) { $javaArguments += '--report' }
if ($ReportPath) { $javaArguments += @('--report-path', $ReportPath) }
$previousDownloadSetting = $env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD
try {
    $env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD = '1'
    & $javaPath @javaArguments
    $runExit = $LASTEXITCODE
} finally {
    $env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD = $previousDownloadSetting
}
exit $runExit
