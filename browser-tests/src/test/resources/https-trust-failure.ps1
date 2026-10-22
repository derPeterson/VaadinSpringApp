param([string]$Repository, [string]$Directory, [string]$Scenario, [string]$RootOnlyMode = 'false')
$ErrorActionPreference = 'Stop'
# Nur die vier .NET-Konstruktoren ersetzen. Der eigentliche Add-/Remove-Ablauf
# wird unverändert ausgeführt, ohne den Windows-Zertifikatsspeicher zu öffnen.
$script:stores = @{}
$script:failure = $Scenario
function New-TestStore([string]$name) {
    if ($script:stores.ContainsKey($name)) { return $script:stores[$name] }
    $store = [pscustomobject]@{ Name = $name; Certificates = [Collections.Generic.List[object]]::new() }
    $store.Certificates.Add([pscustomobject]@{ Thumbprint = 'foreign' })
    $store | Add-Member ScriptMethod Open {
        param($mode)
        if ($script:failure -eq 'rights') { throw [UnauthorizedAccessException]::new('fixture') }
    }
    $store | Add-Member ScriptMethod Close {}
    $store | Add-Member ScriptMethod Add {
        param($certificate)
        if ($script:failure -eq 'partial') { $this.Certificates.Add($certificate) }
        if ($script:failure -eq 'denied') { throw [System.ComponentModel.Win32Exception]::new(5, 'fixture denied') }
        throw [System.ComponentModel.Win32Exception]::new(1223, 'fixture cancelled or denied')
    }
    $store | Add-Member ScriptMethod Remove {
        param($certificate)
        $this.Certificates.Remove($certificate) | Out-Null
    }
    $script:stores[$name] = $store
    return $store
}
$source = Get-Content -LiteralPath (Join-Path $Repository 'scripts/https/trust.ps1') -Raw
$source = $source.Replace('[Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.Path]::GetFullPath($Certificate))', "([pscustomobject]@{ Thumbprint = 'owned-root' })")
$source = $source.Replace('[Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.Path]::GetFullPath($Intermediate))', "([pscustomobject]@{ Thumbprint = 'owned-issuer' })")
$source = $source.Replace("[Security.Cryptography.X509Certificates.X509Store]::new('Root', 'CurrentUser')", "(New-TestStore 'Root')")
$source = $source.Replace("[Security.Cryptography.X509Certificates.X509Store]::new('CA', 'CurrentUser')", "(New-TestStore 'CA')")
if ($source.Contains('::new(')) { throw 'Unexpected unmocked constructor in trust helper' }
$helper = [scriptblock]::Create($source)
$certificate = Join-Path $Directory 'root.crt'
$intermediate = Join-Path $Directory 'intermediate.crt'
$parameters = @{ Action = 'Add'; Certificate = $certificate }
if ($RootOnlyMode -eq 'true') { $parameters.RootOnly = $true }
else { $parameters.Intermediate = $intermediate }
$failed = $false
try { & $helper @parameters } catch { $failed = $true }
if (-not $failed) { throw 'Failed import was accepted' }
if ($Scenario -ne 'rights' -and -not (Test-Path -LiteralPath ($certificate + '.owned'))) {
    throw 'Failed import lost ownership evidence'
}
$script:failure = ''
$parameters.Action = 'Remove'
& $helper @parameters
if (Test-Path -LiteralPath ($certificate + '.owned')) { throw 'Ownership marker remains' }
foreach ($store in $script:stores.Values) {
    if ($store.Certificates.Count -ne 1 -or $store.Certificates[0].Thumbprint -ne 'foreign') {
        throw 'Owned entry remains or foreign trust changed'
    }
}
Write-Output 'cleanup verified'
