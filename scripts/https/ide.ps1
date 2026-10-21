param([Parameter(Mandatory)][string]$CaddyPath)
$ErrorActionPreference = 'Stop'
$executable = (Resolve-Path -LiteralPath $CaddyPath).Path
$version = & $executable version
if ($LASTEXITCODE -ne 0 -or $version -notlike 'v2.10.2 *') { throw 'Caddy v2.10.2 is required' }
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$previousDirectory = $env:APP_HTTPS_CA_DIR
try {
    $env:APP_HTTPS_CA_DIR = (Join-Path $repoRoot 'data/https/ide').Replace('\', '/')
    # Im Vordergrund laufen lassen; Strg+C beendet nur diesen eigenen Proxy.
    & $executable run --config (Join-Path $PSScriptRoot 'Caddyfile.ide') --adapter caddyfile
    if ($LASTEXITCODE -ne 0) { throw 'IDE HTTPS proxy failed' }
} finally { $env:APP_HTTPS_CA_DIR = $previousDirectory }
