@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem ============================================================
rem SonarQube findings export for VaadinSpringApp
rem Place next to sonar-scan.bat / pom.xml.
rem Exports all unresolved regular issues to sonar-findings.json.
rem Run after the SonarQube server has processed the latest scan.
rem Requires Windows PowerShell 5.1+ (no extra modules).
rem ============================================================

set "SONAR_EXTRACT_BAT=%~f0"
where powershell.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Windows PowerShell was not found.
    exit /b 1
)

rem Run the embedded PowerShell code in this BAT file.
rem The token never appears as a command-line argument or in a file.
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$lines = Get-Content -LiteralPath $env:SONAR_EXTRACT_BAT -Encoding UTF8; $script = ($lines | Where-Object { $_.StartsWith('::PS ') } | ForEach-Object { $_.Substring(5) }) -join [Environment]::NewLine; & ([scriptblock]::Create($script))"
exit /b %ERRORLEVEL%

::PS $ErrorActionPreference = 'Stop'
::PS $hostUrl = 'http://localhost:9000'
::PS $projectKey = 'vaadin-spring-app'
::PS $pageSize = 500
::PS $output = Join-Path (Split-Path -Parent $env:SONAR_EXTRACT_BAT) 'sonar-findings.json'
::PS $tempFile = $null
::PS $backupFile = $null
::PS try {
::PS     $token = $env:SONAR_TOKEN
::PS     if ([string]::IsNullOrWhiteSpace($token)) {
::PS         Write-Host '[INFO] SONAR_TOKEN not found. Please enter your SonarQube User Token.'
::PS         $secure = Read-Host 'SonarQube User Token' -AsSecureString
::PS         $token = [System.Net.NetworkCredential]::new('', $secure).Password
::PS         $secure.Dispose()
::PS     }
::PS     if ([string]::IsNullOrWhiteSpace($token)) { throw 'No token entered. Export cancelled.' }
::PS     $headers = @{ Authorization = ('Bearer ' + $token); Accept = 'application/json' }
::PS     Write-Host ('[INFO] Server:  ' + $hostUrl)
::PS     Write-Host ('[INFO] Project: ' + $projectKey)
::PS     Write-Host '[INFO] Exporting all unresolved issues...'
::PS     $issues = [System.Collections.Generic.List[object]]::new()
::PS     $page = 1
::PS     do {
::PS         $uri = '{0}/api/issues/search?componentKeys={1}&resolved=false&ps={2}&p={3}' -f $hostUrl, [uri]::EscapeDataString($projectKey), $pageSize, $page
::PS         $result = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get -TimeoutSec 120
::PS         if ($null -eq $result -or $null -eq $result.issues) { throw 'Unexpected API response: issues list missing.' }
::PS         if ($null -ne $result.paging -and $null -ne $result.paging.total) {
::PS             $total = [int]$result.paging.total
::PS         } elseif ($null -ne $result.total) {
::PS             $total = [int]$result.total
::PS         } else {
::PS             throw 'Unexpected API response: total count missing.'
::PS         }
::PS         if ($total -gt 10000) { throw 'More than 10,000 issues: the SonarQube API requires a partitioned export.' }
::PS         $batch = @($result.issues)
::PS         foreach ($issue in $batch) { if ($null -ne $issue) { $issues.Add($issue) } }
::PS         Write-Host ('[INFO] Retrieved {0}/{1} issues (page {2}).' -f $issues.Count, $total, $page)
::PS         if ($batch.Count -eq 0 -and $issues.Count -lt $total) { throw 'The server returned an empty page before the export was complete.' }
::PS         if ($issues.Count -gt $total) { throw 'Issue count changed during export. Please retry.' }
::PS         $page++
::PS     } while ($issues.Count -lt $total)
::PS     if ($issues.Count -ne $total) { throw 'Incomplete export. Please retry after the server finished processing.' }
::PS     $json = ConvertTo-Json -InputObject $issues.ToArray() -Depth 80
::PS     $tempFile = $output + '.tmp.' + [guid]::NewGuid().ToString('N')
::PS     [System.IO.File]::WriteAllText($tempFile, $json, [System.Text.UTF8Encoding]::new($false))
::PS     # Preserve an existing export until the new JSON has been written completely.
::PS     if ([System.IO.File]::Exists($output)) {
::PS         $backupFile = $output + '.bak.' + [guid]::NewGuid().ToString('N')
::PS         [System.IO.File]::Move($output, $backupFile)
::PS     }
::PS     try {
::PS         [System.IO.File]::Move($tempFile, $output)
::PS     } catch {
::PS         if ($null -ne $backupFile -and [System.IO.File]::Exists($backupFile) -and -not [System.IO.File]::Exists($output)) {
::PS             try { [System.IO.File]::Move($backupFile, $output) }
::PS             catch { Write-Host ('[WARN] Previous export is saved at: ' + $backupFile) -ForegroundColor Yellow }
::PS         }
::PS         throw
::PS     }
::PS     if ($null -ne $backupFile -and [System.IO.File]::Exists($backupFile)) {
::PS         try { [System.IO.File]::Delete($backupFile) }
::PS         catch { Write-Host ('[WARN] Could not delete backup file: ' + $backupFile) -ForegroundColor Yellow }
::PS     }
::PS     Write-Host ('[OK] Exported {0} unresolved issues to: {1}' -f $issues.Count, $output) -ForegroundColor Green
::PS } catch {
::PS     if ($null -ne $tempFile -and [System.IO.File]::Exists($tempFile)) { [System.IO.File]::Delete($tempFile) }
::PS     Write-Host ('[ERROR] Export failed: ' + $_.Exception.Message) -ForegroundColor Red
::PS     Write-Host '[HINT] Check the message above. Existing export is preserved if writing failed.'
::PS     exit 1
::PS }
