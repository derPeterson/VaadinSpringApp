param([switch]$Start, [int]$Port = 60826)
$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../../../../..')).Path
Set-Location -LiteralPath $repository
$runtime = Join-Path $repository 'target/personal-data-pilot'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
& ./mvnw.cmd --version
if ($LASTEXITCODE -ne 0) { throw 'Java-/Maven-Prüfung fehlgeschlagen.' }
if ((& java -version 2>&1 | Out-String) -notmatch 'version "25[.]') { throw 'JDK 25 erforderlich.' }
$classpathFile = Join-Path $runtime 'classpath.txt'
& ./mvnw.cmd -o -pl app -am dependency:build-classpath -DincludeScope=runtime "-Dmdep.outputFile=$classpathFile"
if ($LASTEXITCODE -ne 0) { throw 'Abhängigkeiten konnten nicht aufgelöst werden.' }
# Keine Anwendungs-/Shared-Klassen oder Infrastrukturmodule in diesem eigenen Prozess.
$dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim().Split(';') | Where-Object {
    $_ -notmatch '\\(spring-security[^\\]*|spring-session[^\\]*|spring-data[^\\]*|spring-jdbc|spring-orm|hibernate-core|h2|shared|spring-boot-devtools|spring-boot-(jdbc|data-jpa|hibernate|security[^\\]*|session|mail))\\'
}
$classes = Join-Path $runtime 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = @(Get-ChildItem -LiteralPath 'app/src/test/java/de/derpeterson/designpilot' -Filter '*.java' | ForEach-Object FullName)
$sources += @(
    'app/src/main/java/de/derpeterson/app/ui/components/CosmosCardComponent.java',
    'app/src/main/java/de/derpeterson/app/ui/components/CosmosSectionHeaderComponent.java'
)
& javac --release 25 -encoding UTF-8 -cp ($dependencies -join ';') -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Pilot kompiliert nicht.' }
# Vorhandenes Produktions-Frontend unverändert übernehmen, ausschließlich VAADIN-Ressourcen.
# Keine Anwendungskonfiguration, Kontodaten oder Serverklassen aus dem JAR laden.
$bundle = Join-Path $repository 'app/target/app-1.0-SNAPSHOT.jar.original'
if (-not (Test-Path -LiteralPath $bundle)) { throw 'Produktionsbundle fehlt; zunächst den dokumentierten Reactor-Build ausführen.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($bundle)
try {
    foreach ($entry in $archive.Entries) {
        if (-not $entry.FullName.StartsWith('META-INF/VAADIN/') -or $entry.FullName.EndsWith('/')) { continue }
        $destination = [IO.Path]::GetFullPath((Join-Path $classes $entry.FullName))
        if (-not $destination.StartsWith($classes + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Ungültiger Ressourcenpfad.' }
        New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($destination)) | Out-Null
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination, $true)
    }
} finally { $archive.Dispose() }
$assets = Join-Path $classes 'META-INF/resources/pilot'
New-Item -ItemType Directory -Force -Path $assets | Out-Null
Copy-Item -LiteralPath 'app/src/test/resources/personal-data-pilot/personal-data.css' -Destination $assets
Copy-Item -LiteralPath 'app/src/test/resources/personal-data-pilot/icons.svg' -Destination $assets
Copy-Item -LiteralPath 'app/src/main/resources/META-INF/resources/custom-theme/typography.css' -Destination $assets
Copy-Item -LiteralPath 'app/src/main/resources/META-INF/resources/custom-theme/fonts' -Destination $assets -Recurse -Force
Copy-Item -LiteralPath 'docs/design/service-logo/logo-k.svg' -Destination $assets
$runtimeClasspath = (@($classes) + $dependencies) -join ';'
$argumentsFile = Join-Path $runtime 'java.args'
$javaArguments = @(
    '-Dspring.devtools.restart.enabled=false',
    '-cp', ('"' + $runtimeClasspath.Replace('\', '/') + '"'),
    'de.derpeterson.designpilot.PersonalDataPilotApplication',
    '--spring.config.location=optional:classpath:/pilot-only.properties',
    '--spring.config.import=', '--spring.profiles.active=personal-data-pilot',
    '--spring.main.banner-mode=off', '--spring.jmx.enabled=false',
    '--server.address=127.0.0.1', "--server.port=$Port",
    '--vaadin.productionMode=true', '--vaadin.launch-browser=false',
    '--vaadin.allowed-packages=de.derpeterson.designpilot',
    '--logging.level.root=WARN', '--logging.level.de.derpeterson.designpilot=INFO'
)
[IO.File]::WriteAllLines($argumentsFile, $javaArguments, [Text.UTF8Encoding]::new($false))
Write-Output "Pilot vorbereitet: $runtime"
if ($Start) {
    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($listener) { throw "Port $Port ist belegt. Es wurde kein vorhandener Prozess beendet." }
    $process = Start-Process -FilePath (Get-Command java).Source -ArgumentList ('@"' + $argumentsFile + '"') -WorkingDirectory $runtime -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime 'stdout.txt') -RedirectStandardError (Join-Path $runtime 'stderr.txt')
    [IO.File]::WriteAllText((Join-Path $runtime 'process-id.txt'), [string]$process.Id)
    if ($process.WaitForExit(3000)) { throw "Pilotstart fehlgeschlagen. Eigene Startdiagnose: $runtime/stdout.txt und stderr.txt" }
    Write-Output "Vaadin-Pilot: http://127.0.0.1:$Port/ (eigener Prozess $($process.Id))"
}
