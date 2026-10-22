param(
    [Parameter(Mandatory)][ValidateSet('Add', 'Remove')][string]$Action,
    [Parameter(Mandatory)][string]$Certificate,
    [Parameter(Mandatory, ParameterSetName='Chain')][string]$Intermediate,
    [Parameter(Mandatory, ParameterSetName='RootOnly')][switch]$RootOnly
)
$ErrorActionPreference = 'Stop'
# Nur die expliziten öffentlichen Zertifikate bearbeiten; keine Schlüssel importieren.
$root = [Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.Path]::GetFullPath($Certificate))
$issuer = if ($RootOnly) { $null } else { [Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.Path]::GetFullPath($Intermediate)) }
$issuerThumbprint = if ($null -eq $issuer) { '' } else { $issuer.Thumbprint }
$marker = [IO.Path]::GetFullPath($Certificate) + '.owned'
$rootStore = [Security.Cryptography.X509Certificates.X509Store]::new('Root', 'CurrentUser')
$issuerStore = if ($RootOnly) { $null } else { [Security.Cryptography.X509Certificates.X509Store]::new('CA', 'CurrentUser') }
$rootStore.Open('ReadWrite')
if ($null -ne $issuerStore) { $issuerStore.Open('ReadWrite') }
try {
    if ($Action -eq 'Add') {
        if (Test-Path -LiteralPath $marker) { throw 'Trust ownership marker already exists' }
        if (@($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint).Count -ne 0 -or
            ($null -ne $issuerStore -and @($issuerStore.Certificates | Where-Object Thumbprint -eq $issuerThumbprint).Count -ne 0)) {
            throw 'This CA is already present; refusing to take ownership of existing trust'
        }
        # Vor dem Import schreiben, damit auch ein abgebrochener Import bereinigt werden kann.
        @{ root = $root.Thumbprint; intermediate = $issuerThumbprint } | ConvertTo-Json | Set-Content -LiteralPath $marker
        Write-Output ('Confirm local HTTPS CA in Windows: ' + $root.Thumbprint)
        $rootStore.Add($root)
        if (@($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint).Count -ne 1) {
            throw 'Temporary CA trust was not installed'
        }
    } elseif (Test-Path -LiteralPath $marker) {
        $owned = Get-Content -LiteralPath $marker -Raw | ConvertFrom-Json
        if ($owned.root -ne $root.Thumbprint -or $owned.intermediate -ne $issuerThumbprint) {
            throw 'Trust ownership mismatch'
        }
        foreach ($stored in @($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint)) { $rootStore.Remove($stored) }
        if ($null -ne $issuerStore) {
            foreach ($stored in @($issuerStore.Certificates | Where-Object Thumbprint -eq $issuerThumbprint)) { $issuerStore.Remove($stored) }
        }
        if (@($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint).Count -ne 0 -or
            ($null -ne $issuerStore -and @($issuerStore.Certificates | Where-Object Thumbprint -eq $issuerThumbprint).Count -ne 0)) {
            throw 'Temporary CA trust remains installed'
        }
        Remove-Item -LiteralPath $marker
    }
} finally {
    $rootStore.Close()
    if ($null -ne $issuerStore) { $issuerStore.Close() }
}
