param([switch]$Start, [switch]$Build, [switch]$Visitors, [switch]$Administrator, [switch]$Administration, [int]$Port = 60827)
$ErrorActionPreference = 'Stop'
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../../../../..')).Path
Set-Location -LiteralPath $repository
$runtimeName = if ($Administration) { 'target/admin-reference-review' } elseif ($Visitors) { 'target/visitor-reference-review' } else { 'target/account-reference-review' }
$runtime = Join-Path $repository $runtimeName
$bootstrapGroup = if ($Administration) { 'superadmins' } elseif ($Administrator) { 'admins' } else { 'users' }
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
& ./mvnw.cmd --version
if ($LASTEXITCODE -ne 0) { throw 'Java-/Maven-Prüfung fehlgeschlagen.' }
if ((& java -version 2>&1 | Out-String) -notmatch 'version "25[.]') { throw 'JDK 25 erforderlich.' }
if ($Build) {
    & ./mvnw.cmd -pl app -am package -Pproduction '-Dvaadin.productionMode=true' '-DskipTests'
    if ($LASTEXITCODE -ne 0) { throw 'Anwendungsbuild fehlgeschlagen.' }
}
$bundle = Join-Path $repository 'app/target/app-1.0-SNAPSHOT.jar.original'
if (-not (Test-Path -LiteralPath $bundle)) { throw 'Optimierter Build fehlt. Zuerst mit -Build vorbereiten.' }
$classpathFile = Join-Path $runtime 'classpath.txt'
& ./mvnw.cmd -o -pl app -am dependency:build-classpath -DincludeScope=runtime "-Dmdep.outputFile=$classpathFile"
if ($LASTEXITCODE -ne 0) { throw 'Abhängigkeiten konnten nicht aufgelöst werden.' }
$dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
$classes = Join-Path $runtime 'review-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$classpath = (@($classes, (Join-Path $repository 'shared/target/classes'), $bundle, $dependencies) -join ';')
& javac --release 25 -encoding UTF-8 -cp $classpath -d $classes 'app/src/test/java/de/derpeterson/frontendreview/FrontendReviewApplication.java' 'app/src/test/java/de/derpeterson/frontendreview/VisitorReviewExamples.java' 'app/src/test/java/de/derpeterson/frontendreview/VisitorMaintenancePage.java' 'app/src/test/java/de/derpeterson/frontendreview/VisitorRecoveryPage.java'
if ($LASTEXITCODE -ne 0) { throw 'Sichtungsumgebung kompiliert nicht.' }
$web = Join-Path $runtime 'www/vergleich'
New-Item -ItemType Directory -Force -Path $web | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'index.html') -Destination $web -Force
Copy-Item -LiteralPath 'docs/design/frontend-referenzen/portal.html' -Destination (Join-Path $web 'referenz.html') -Force
$referenceRevision = (Get-FileHash -LiteralPath (Join-Path $web 'referenz.html') -Algorithm SHA256).Hash.Substring(0, 12).ToLowerInvariant()
$comparisonPath = Join-Path $web 'index.html'
$comparisonSource = [System.IO.File]::ReadAllText($comparisonPath).Replace('__REFERENCE_REVISION__', $referenceRevision)
[System.IO.File]::WriteAllText($comparisonPath, $comparisonSource, [System.Text.UTF8Encoding]::new($false))
if ($Visitors) {
    $visitorWeb = Join-Path $runtime 'www/images/visitor-review'
    New-Item -ItemType Directory -Force -Path $visitorWeb | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'visitors.html') -Destination (Join-Path $visitorWeb 'index.html') -Force
    Copy-Item -LiteralPath 'docs/design/frontend-referenzen/public.html' -Destination (Join-Path $visitorWeb 'reference.html') -Force
    $visitorRevision = (Get-FileHash -LiteralPath (Join-Path $visitorWeb 'reference.html') -Algorithm SHA256).Hash.Substring(0, 12).ToLowerInvariant()
    $visitorIndex = Join-Path $visitorWeb 'index.html'
    [IO.File]::WriteAllText($visitorIndex, [IO.File]::ReadAllText($visitorIndex).Replace('__PUBLIC_REFERENCE_REVISION__', $visitorRevision), [Text.UTF8Encoding]::new($false))
}
if ($Administration) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'admin.html') -Destination (Join-Path $web 'admin.html') -Force
    Copy-Item -LiteralPath 'docs/design/frontend-referenzen/admin-inhalt.html' -Destination (Join-Path $web 'admin-reference.html') -Force
    $durationDemoSource = Join-Path $PSScriptRoot '../admin-entwuerfe/dauer-beispiel'
    $durationDemoTarget = Join-Path $web 'dauer-feld'
    New-Item -ItemType Directory -Force -Path $durationDemoTarget | Out-Null
    foreach ($durationDemoFile in @('index.html', 'beispiel.css', 'beispiel.js')) {
        Copy-Item -LiteralPath (Join-Path $durationDemoSource $durationDemoFile) -Destination $durationDemoTarget -Force
    }
    $adminRevision = (Get-FileHash -LiteralPath (Join-Path $web 'admin-reference.html') -Algorithm SHA256).Hash.Substring(0,12).ToLowerInvariant()
    $adminIndex = Join-Path $web 'admin.html'
    [IO.File]::WriteAllText($adminIndex, [IO.File]::ReadAllText($adminIndex).Replace('__ADMIN_REFERENCE_REVISION__',$adminRevision),[Text.UTF8Encoding]::new($false))
}
$argumentsFile = Join-Path $runtime 'java.args'
$readyFile = Join-Path $runtime ('ready-' + [Guid]::NewGuid().ToString() + '.txt')
$arguments = @(
    '-Dspring.devtools.restart.enabled=false', '-cp', ('"' + $classpath.Replace('\', '/') + '"'),
    'de.derpeterson.frontendreview.FrontendReviewApplication',
    ('"--frontend.review.ready-file=' + $readyFile.Replace('\','/') + '"'),
    '--spring.config.location=classpath:/application.properties', '--spring.config.import=',
    '--spring.profiles.active=account-reference-review', '--server.address=127.0.0.1', "--server.port=$Port",
    '--spring.datasource.url=jdbc:h2:mem:account-reference-review;MODE=MySQL;DB_CLOSE_DELAY=-1;SCHEMA=PUBLIC',
    "--frontend.review.visitors=$($Visitors.IsPresent.ToString().ToLowerInvariant())",
    '--app.bootstrap.enabled=true', "--app.bootstrap.$($bootstrapGroup)[0].email=reference-user@example.test",
    "--app.bootstrap.$($bootstrapGroup)[0].password=ReferenceUser1!", "--app.bootstrap.$($bootstrapGroup)[0].first-name=Alex",
    "--app.bootstrap.$($bootstrapGroup)[0].last-name=Berger", "--app.bootstrap.$($bootstrapGroup)[0].preferred-locale=de",
    '--app.config.defaults.service.name={ServiceName}', "--app.config.defaults.base.url=http://localhost:$Port/",
    '--app.config.defaults.rememberMe.secret.key=own-isolated-account-review-secret',
    '--app.config.defaults.mail.host=127.0.0.1', '--app.config.defaults.mail.port=1',
    '--app.config.defaults.mail.smtp.auth=false', '--app.config.defaults.mail.username=', '--app.config.defaults.mail.password=',
    '--vaadin.productionMode=true', '--vaadin.frontend.hotdeploy=false', '--vaadin.launch-browser=false',
    '--spring.h2.console.enabled=false', '--app.security.expose-h2-console=false',
    '--logging.level.root=WARN', '--logging.level.de.derpeterson.app=INFO',
    "--logging.file.name=$($runtime.Replace('\','/'))/application.log",
    "--spring.web.resources.static-locations=classpath:/META-INF/resources/,classpath:/resources/,classpath:/static/,classpath:/public/,file:///$($runtime.Replace('\','/'))/www/"
)
if ($Administration) {
    $arguments += '--frontend.review.admin=true'
    for ($index = 1; $index -le 24; $index++) {
        $group = if ($index % 6 -eq 0) { 'admins' } else { 'users' }
        $slot = if ($group -eq 'admins') { [int]($index / 6) - 1 } else { $index - 1 - [int][Math]::Floor($index / 6) }
        $prefix = "--app.bootstrap.$($group)[$slot]"
        $arguments += "$prefix.email=person$index@example.test"
        $arguments += "$prefix.password=ReferenceUser1!"
        $arguments += "$prefix.first-name=Person$index"
        $arguments += "$prefix.last-name=Beispiel"
        $arguments += "$prefix.preferred-locale=de"
    }
}
[IO.File]::WriteAllLines($argumentsFile, $arguments, [Text.UTF8Encoding]::new($false))
Write-Output "Vergleich vorbereitet: $runtime"
if ($Start) {
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
        throw "Port $Port ist belegt. Es wurde kein vorhandener Prozess beendet."
    }
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = (Get-Command java).Source
    $startInfo.WorkingDirectory = $runtime
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.WindowStyle = [Diagnostics.ProcessWindowStyle]::Hidden
    # Die echte App erhält ausschließlich diese isolierten Einstellungen, keine geerbten Secrets.
    $startInfo.Environment.Clear()
    foreach ($name in @('SYSTEMROOT','WINDIR','TEMP','TMP','PATH','USERPROFILE','LOCALAPPDATA','APPDATA')) {
        $value = [Environment]::GetEnvironmentVariable($name)
        if ($value) { $startInfo.Environment[$name] = $value }
    }
    $startInfo.ArgumentList.Add('@' + $argumentsFile)
    $process = [Diagnostics.Process]::Start($startInfo)
    [IO.File]::WriteAllText((Join-Path $runtime 'process-id.txt'), [string]$process.Id)
    if ($process.WaitForExit(3000)) { throw 'Sichtungsumgebung konnte nicht starten.' }
    $deadline = [DateTime]::UtcNow.AddSeconds(50)
    while (-not (Test-Path -LiteralPath $readyFile)) {
        if ($process.HasExited) { throw 'Sichtungsumgebung wurde vor der Bereitschaft beendet.' }
        if ([DateTime]::UtcNow -gt $deadline) { throw 'Sichtungsumgebung ist noch nicht bereit; eigene Startdiagnose prüfen.' }
        Start-Sleep -Milliseconds 500
    }
    Write-Output "Zuerst anmelden: http://localhost:$Port/login"
    Write-Output 'Eigenes Testkonto: reference-user@example.test / ReferenceUser1!'
    if ($Administration) {
        Write-Output "Adminvergleich: http://localhost:$Port/vergleich/admin.html"
    } elseif ($Visitors) {
        Write-Output "Besuchervergleich ohne Anmeldung: http://127.0.0.1:$Port/images/visitor-review/index.html"
    } else {
        Write-Output "Danach Vergleich: http://localhost:$Port/vergleich/index.html"
    }
}
