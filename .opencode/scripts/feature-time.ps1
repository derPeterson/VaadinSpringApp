param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('Start', 'Stop')]
    [string]$Action,
    [string]$Id
)

$ErrorActionPreference = 'Stop'
$now = [DateTimeOffset]::Now
$culture = [Globalization.CultureInfo]::InvariantCulture
$owned = $false
$path = $null

try {
    $directory = Join-Path $env:LOCALAPPDATA 'Temp\opencode'
    $directory = [IO.Path]::GetFullPath($directory)
    $repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
    if ($directory.StartsWith($repository + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase) -or $directory -eq $repository) {
        throw 'Time directory must be outside the repository.'
    }

    if ($Action -eq 'Start') {
        if ($Id) { throw 'Start creates its own ID; do not supply -Id.' }
        $Id = [guid]::NewGuid().ToString('N')
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
        $path = Join-Path $directory "feature-time-$Id.json"
        $record = @{ owner = 'opencode-feature-time-v1'; id = $Id; start = $now.ToString('o') }
        # CreateNew prevents overwriting any existing file, even on an ID collision.
        $stream = [IO.File]::Open($path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
        $writer = New-Object IO.StreamWriter($stream)
        try { $writer.Write(($record | ConvertTo-Json -Compress)) }
        finally { $writer.Dispose() }
        Write-Output "Lauf-ID: $Id"
        Write-Output "Start: $($now.ToString('o'))"
    } else {
        if ($Id -cnotmatch '^[a-f0-9]{32}$') { throw 'Missing or invalid run ID.' }
        $path = Join-Path $directory "feature-time-$Id.json"
        $record = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
        if ($record.owner -cne 'opencode-feature-time-v1' -or $record.id -cne $Id) {
            throw 'Time file does not belong to this run.'
        }
        $owned = $true
        $start = [DateTimeOffset]::ParseExact($record.start, 'o', $culture)
        if ($now -lt $start) { throw 'System clock moved backwards; duration is invalid.' }
        Write-Output "Start: $($start.ToString('o'))"
        Write-Output "Ende: $($now.ToString('o'))"
        Write-Output "Dauer: $(($now - $start).ToString('c', $culture))"
    }
} catch {
    Write-Output "Ende: $($now.ToString('o'))"
    Write-Output 'Dauer nicht erfasst'
    Write-Error $_ -ErrorAction Continue
    exit 1
} finally {
    if ($owned) {
        try { Remove-Item -LiteralPath $path -ErrorAction Stop }
        catch { Write-Error "Own time file could not be removed: $path" -ErrorAction Continue; exit 1 }
    }
}
