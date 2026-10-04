param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('Start', 'Finish')]
    [string]$Action,

    [string]$Id,
    [string]$Model,
    [string]$Provider,
    [string]$Task,
    [string]$TargetClass,

    [int]$HumanInterventions = 0,
    [int]$CorrectionRounds = 0
)

$ErrorActionPreference = 'Stop'
$culture = [Globalization.CultureInfo]::InvariantCulture

# Repository automatisch bestimmen
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))

# Projektname automatisch aus dem Projektordner übernehmen
$projectName = Split-Path $repo -Leaf

# Benchmark-Daten bewusst außerhalb des Git-Repositories speichern
$benchmarkRoot = 'C:\Dev\AI-Benchmarks'
$store = Join-Path $benchmarkRoot $projectName

$stateDir = Join-Path $store 'state'
$runsDir = Join-Path $store 'runs'
$resultsFile = Join-Path $store 'results.csv'

New-Item -ItemType Directory -Force -Path $stateDir, $runsDir | Out-Null
Set-Location $repo

function Invoke-FreshReports {
    & .\mvnw.cmd clean test
    if ($LASTEXITCODE -ne 0) {
        throw "mvn clean test failed with exit code $LASTEXITCODE"
    }

    & .\mvnw.cmd jacoco:report
    if ($LASTEXITCODE -ne 0) {
        throw "jacoco:report failed with exit code $LASTEXITCODE"
    }
}

function Get-Metrics([string]$ClassName) {
    $jacoco = Join-Path $repo 'target\site\jacoco\jacoco.csv'

    if (-not (Test-Path $jacoco)) {
        throw "JaCoCo report not found: $jacoco"
    }

    $matchingRows = @(
        Import-Csv $jacoco |
            Where-Object { $_.CLASS -eq $ClassName }
    )

    if ($matchingRows.Count -eq 0) {
        throw "Class '$ClassName' not found in JaCoCo report."
    }

    if ($matchingRows.Count -eq 1) {
        $row = $matchingRows[0]
    } else {
        # Falls derselbe Klassenname in mehreren Packages vorkommt, das Package
        # aus der Java-Quelldatei ableiten und damit den JaCoCo-Eintrag eindeutig machen.
        $sourceFiles = @(
            Get-ChildItem -Path (Join-Path $repo 'src\main\java') -Recurse -Filter "$ClassName.java" -File -ErrorAction SilentlyContinue
        )

        if ($sourceFiles.Count -ne 1) {
            $packages = ($matchingRows | ForEach-Object { $_.PACKAGE }) -join ', '
            throw "Class '$ClassName' is ambiguous in JaCoCo report (packages: $packages) and could not be uniquely resolved from src/main/java."
        }

        $packageMatch = Select-String -LiteralPath $sourceFiles[0].FullName -Pattern '^\s*package\s+([A-Za-z0-9_.]+)\s*;' | Select-Object -First 1

        if (-not $packageMatch) {
            throw "Could not determine package for '$($sourceFiles[0].FullName)'."
        }

        $packageName = $packageMatch.Matches[0].Groups[1].Value
        $row = $matchingRows |
            Where-Object {
                ($_.PACKAGE -replace '/', '.') -eq $packageName
            } |
            Select-Object -First 1

        if (-not $row) {
            throw "Class '$ClassName' was found multiple times, but no JaCoCo entry matched package '$packageName'."
        }
    }

    $lineMissed = [int]$row.LINE_MISSED
    $lineCovered = [int]$row.LINE_COVERED
    $lineTotal = $lineMissed + $lineCovered

    $branchMissed = [int]$row.BRANCH_MISSED
    $branchCovered = [int]$row.BRANCH_COVERED
    $branchTotal = $branchMissed + $branchCovered

    $linePct = if ($lineTotal -gt 0) {
        [math]::Round(($lineCovered / $lineTotal) * 100, 2)
    } else {
        100
    }

    $branchPct = if ($branchTotal -gt 0) {
        [math]::Round(($branchCovered / $branchTotal) * 100, 2)
    } else {
        100
    }

    $tests = 0
    $failures = 0
    $errors = 0
    $skipped = 0
    $testTime = 0.0

    $reports = Get-ChildItem (Join-Path $repo 'target\surefire-reports\TEST-*.xml')

    foreach ($report in $reports) {
        [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        $suite = $xml.testsuite

        $tests += [int]$suite.tests
        $failures += [int]$suite.failures
        $errors += [int]$suite.errors
        $skipped += [int]$suite.skipped
        $testTime += [double]::Parse([string]$suite.time, $culture)
    }

    [PSCustomObject]@{
        tests            = $tests
        failures         = $failures
        errors           = $errors
        skipped          = $skipped
        testTime         = [math]::Round($testTime, 3)
        lineCoverage     = $linePct
        branchCoverage   = $branchPct
        linesCovered     = $lineCovered
        linesTotal       = $lineTotal
        branchesCovered  = $branchCovered
        branchesTotal    = $branchTotal
    }
}

if ($Action -eq 'Start') {

    if (-not $Model -or -not $Provider -or -not $Task -or -not $TargetClass) {
        throw 'Start requires -Model, -Provider, -Task and -TargetClass.'
    }

    $branch = (& git branch --show-current).Trim()

    if ($branch -ne 'main') {
        throw "Benchmark must start on main. Current branch: $branch"
    }

    if (& git status --porcelain) {
        throw 'Benchmark must start with a clean working tree.'
    }

    Write-Host 'Creating fresh baseline...'
    Invoke-FreshReports

    $before = Get-Metrics $TargetClass
    $startCommit = (& git rev-parse HEAD).Trim()
    $runId = [guid]::NewGuid().ToString('N')
    $start = [DateTimeOffset]::Now

    $state = [ordered]@{
        owner       = 'opencode-ai-benchmark-v1'
        id          = $runId
        model       = $Model
        provider    = $Provider
        task        = $Task
        targetClass = $TargetClass
        startCommit = $startCommit
        start       = $start.ToString('o')
        before      = $before
    }

    $statePath = Join-Path $stateDir "$runId.json"

    $state |
        ConvertTo-Json -Depth 6 |
        Set-Content -LiteralPath $statePath -Encoding UTF8

    Write-Host ''
    Write-Host "Benchmark-ID: $runId"
    Write-Host "Projekt:      $projectName"
    Write-Host "Model:        $Model"
    Write-Host "Provider:     $Provider"
    Write-Host "Start-Commit: $startCommit"
    Write-Host "Start:        $($start.ToString('o'))"
    Write-Host "Speicherort:  $store"
    Write-Host ''

    Write-Host "BEFORE $TargetClass"
    Write-Host "Line Coverage:   $($before.lineCoverage) %"
    Write-Host "Branch Coverage: $($before.branchCoverage) %"
    Write-Host "Tests:            $($before.tests)"
    Write-Host "Failures:         $($before.failures)"
    Write-Host "Errors:           $($before.errors)"
    Write-Host "Skipped:          $($before.skipped)"
    Write-Host "Testzeit:         $($before.testTime) s"

    exit 0
}

if ($Id -cnotmatch '^[a-f0-9]{32}$') {
    throw 'Finish requires a valid -Id.'
}

$statePath = Join-Path $stateDir "$Id.json"

if (-not (Test-Path $statePath)) {
    throw "Benchmark state not found: $statePath"
}

$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json

if ($state.owner -ne 'opencode-ai-benchmark-v1') {
    throw 'Invalid benchmark state file.'
}

$branch = (& git branch --show-current).Trim()

if (-not $branch.StartsWith('feature/', [StringComparison]::OrdinalIgnoreCase)) {
    throw "Benchmark Finish must run on a feature/* branch. Current branch: $branch"
}

# findings.md ist ein bewusstes Run-Artefakt und darf nicht committed werden.
# Finish akzeptiert genau diese Datei als untracked; alle anderen Aenderungen bleiben verboten.
$findingsSource = Join-Path $repo 'findings.md'
$statusLines = @(& git status --porcelain) | Where-Object { $_ }
$unexpectedStatus = @(
    $statusLines | Where-Object {
        $_ -ne '?? findings.md'
    }
)

if ($unexpectedStatus.Count -gt 0) {
    throw "Benchmark Finish requires a clean working tree except for an untracked findings.md. Unexpected status: $($unexpectedStatus -join '; ')"
}

$findingsTracked = (& git ls-files --error-unmatch -- 'findings.md' 2>$null)
if ($LASTEXITCODE -eq 0 -and $findingsTracked) {
    throw 'findings.md must not be tracked or committed. Keep it untracked so Finish can archive it with the run.'
}

# Ende der eigentlichen Agentenarbeit erfassen,
# bevor der abschließende Benchmark-Build gestartet wird.
$end = [DateTimeOffset]::Now
$start = [DateTimeOffset]::Parse($state.start)
$duration = $end - $start

Write-Host 'Creating fresh final report...'
Invoke-FreshReports

$after = Get-Metrics $state.targetClass
$endCommit = (& git rev-parse HEAD).Trim()

$changedFiles = @(
    & git diff --name-only "$($state.startCommit)..HEAD"
) | Where-Object { $_ }

$insertions = 0
$deletions = 0

foreach ($line in (& git diff --numstat "$($state.startCommit)..HEAD")) {
    $parts = $line -split "`t"

    if ($parts.Count -ge 2) {
        if ($parts[0] -match '^\d+$') {
            $insertions += [int]$parts[0]
        }

        if ($parts[1] -match '^\d+$') {
            $deletions += [int]$parts[1]
        }
    }
}

$result = [PSCustomObject]@{
    Date                    = $end.ToString('yyyy-MM-dd HH:mm:ss')
    Id                      = $Id
    Task                    = $state.task
    Model                   = $state.model
    Provider                = $state.provider
    TargetClass             = $state.targetClass
    StartCommit             = $state.startCommit
    EndCommit               = $endCommit
    Branch                  = $branch
    DurationSeconds         = [math]::Round($duration.TotalSeconds, 1)

    TestsBefore             = $state.before.tests
    TestsAfter              = $after.tests
    FailuresAfter           = $after.failures
    ErrorsAfter             = $after.errors
    SkippedAfter            = $after.skipped

    TestTimeBeforeSeconds   = $state.before.testTime
    TestTimeAfterSeconds    = $after.testTime

    LineCoverageBefore      = $state.before.lineCoverage
    LineCoverageAfter       = $after.lineCoverage

    BranchCoverageBefore    = $state.before.branchCoverage
    BranchCoverageAfter     = $after.branchCoverage

    HumanInterventions      = $HumanInterventions
    CorrectionRounds        = $CorrectionRounds

    ChangedFiles            = $changedFiles.Count
    Insertions              = $insertions
    Deletions               = $deletions
}

# Dezimalwerte fuer die CSV kulturunabhaengig mit Punkt schreiben.
# Dadurch bleibt die Datei auch ausserhalb einer deutschen Windows-/Excel-Umgebung
# problemlos maschinell auswertbar.
$csvNumberFormats = @{
    DurationSeconds       = '0.0'
    TestTimeBeforeSeconds = '0.000'
    TestTimeAfterSeconds  = '0.000'
    LineCoverageBefore    = '0.00'
    LineCoverageAfter     = '0.00'
    BranchCoverageBefore  = '0.00'
    BranchCoverageAfter   = '0.00'
}

$csvValues = [ordered]@{}

foreach ($property in $result.PSObject.Properties) {
    if ($csvNumberFormats.ContainsKey($property.Name)) {
        $csvValues[$property.Name] = ([double]$property.Value).ToString(
            $csvNumberFormats[$property.Name],
            $culture
        )
    } else {
        $csvValues[$property.Name] = $property.Value
    }
}

$csvResult = [PSCustomObject]$csvValues

$safeModel = $state.model -replace '[^A-Za-z0-9._-]', '_'
$safeClass = $state.targetClass -replace '[^A-Za-z0-9._-]', '_'

$runFolder = Join-Path $runsDir (
    "$($end.ToString('yyyy-MM-dd_HHmmss'))_${safeClass}_${safeModel}"
)
New-Item -ItemType Directory -Force -Path $runFolder | Out-Null

$reportFile = Join-Path $runFolder 'report.md'
$findingsFile = Join-Path $runFolder 'findings.md'
$diffFile = Join-Path $runFolder 'diff.patch'

# Findings als Run-Artefakt archivieren und danach aus dem Repository entfernen.
# Fehlt die Datei, bleibt trotzdem fuer jeden Run ein eindeutiges findings.md erhalten.
if (Test-Path -LiteralPath $findingsSource -PathType Leaf) {
    Move-Item -LiteralPath $findingsSource -Destination $findingsFile -Force
} else {
    @(
        '# Findings'
        ''
        'Fuer diesen Benchmark-Lauf wurde keine `findings.md` erzeugt.'
    ) | Set-Content -LiteralPath $findingsFile -Encoding UTF8
}

if (& git status --porcelain) {
    throw 'Working tree is not clean after archiving findings.md.'
}

# Vollstaendigen Commit-Diff des Runs als reproduzierbares Patch-Artefakt sichern.
$diffLines = @(& git diff --binary --full-index "$($state.startCommit)..$endCommit")
$diffText = if ($diffLines.Count -gt 0) { ($diffLines -join "`n") + "`n" } else { '' }
[IO.File]::WriteAllText($diffFile, $diffText, [Text.UTF8Encoding]::new($false))

$report = @"
# AI Coding Benchmark

## Aufgabe

$($state.task)

## Umgebung

- Projekt: $projectName
- Benchmark-ID: $Id
- Modell: $($state.model)
- Provider: $($state.provider)
- Zielklasse: $($state.targetClass)
- Start-Commit: $($state.startCommit)
- End-Commit: $endCommit
- Branch: $branch

## Laufzeit

- Start: $($start.ToString('o'))
- Ende: $($end.ToString('o'))
- Gesamtdauer: $($duration.ToString('c'))

## Tests

| Messwert | Vorher | Nachher |
|---|---:|---:|
| Tests | $($state.before.tests) | $($after.tests) |
| Failures | $($state.before.failures) | $($after.failures) |
| Errors | $($state.before.errors) | $($after.errors) |
| Skipped | $($state.before.skipped) | $($after.skipped) |
| reine Testzeit | $($state.before.testTime) s | $($after.testTime) s |

## Coverage – $($state.targetClass)

| Messwert | Vorher | Nachher |
|---|---:|---:|
| Line Coverage | $($state.before.lineCoverage) % | $($after.lineCoverage) % |
| Branch Coverage | $($state.before.branchCoverage) % | $($after.branchCoverage) % |

## Änderungen

- Geänderte Dateien: $($changedFiles.Count)
- Neue Zeilen: $insertions
- Entfernte Zeilen: $deletions
- Menschliche Eingriffe: $HumanInterventions
- Korrekturrunden: $CorrectionRounds

### Geänderte Dateien

$($changedFiles -join "`n")

## Run-Artefakte

- Findings: findings.md
- Diff: diff.patch
"@

$report |
    Set-Content -LiteralPath $reportFile -Encoding UTF8

# results.csv erst aktualisieren, wenn alle Run-Artefakte erfolgreich geschrieben wurden.
if (Test-Path $resultsFile) {
    $csvResult |
        Export-Csv -LiteralPath $resultsFile -NoTypeInformation -Append -Encoding UTF8
} else {
    $csvResult |
        Export-Csv -LiteralPath $resultsFile -NoTypeInformation -Encoding UTF8
}

Remove-Item -LiteralPath $statePath

Write-Host ''
Write-Host 'BENCHMARK ABGESCHLOSSEN'
Write-Host "Projekt:          $projectName"
Write-Host "Dauer:            $($duration.ToString('c'))"
Write-Host "Tests:            $($state.before.tests) -> $($after.tests)"
Write-Host "Line Coverage:    $($state.before.lineCoverage) % -> $($after.lineCoverage) %"
Write-Host "Branch Coverage:  $($state.before.branchCoverage) % -> $($after.branchCoverage) %"
Write-Host "Failures:         $($after.failures)"
Write-Host "Errors:           $($after.errors)"
Write-Host ''
Write-Host "CSV:              $resultsFile"
Write-Host "Run-Ordner:       $runFolder"
Write-Host "Report:           $reportFile"
Write-Host "Findings:         $findingsFile"
Write-Host "Diff:             $diffFile"
