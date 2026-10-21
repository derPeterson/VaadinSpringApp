param(
    [Parameter(Mandatory)][ValidateSet('Add', 'Remove')][string]$Action,
    [Parameter(Mandatory)][string]$Certificate,
    [Parameter(Mandatory)][string]$Intermediate
)
$ErrorActionPreference = 'Stop'
# Nur die expliziten öffentlichen Zertifikate bearbeiten; keine Schlüssel importieren.
$root = [Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.Path]::GetFullPath($Certificate))
$issuer = [Security.Cryptography.X509Certificates.X509Certificate2]::new([IO.Path]::GetFullPath($Intermediate))
$marker = [IO.Path]::GetFullPath($Certificate) + '.owned'
$rootStore = [Security.Cryptography.X509Certificates.X509Store]::new('Root', 'CurrentUser')
$issuerStore = [Security.Cryptography.X509Certificates.X509Store]::new('CA', 'CurrentUser')
$rootStore.Open('ReadWrite')
$issuerStore.Open('ReadWrite')
try {
    if ($Action -eq 'Add') {
        if (Test-Path -LiteralPath $marker) { throw 'Trust ownership marker already exists' }
        if (@($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint).Count -ne 0 -or
            @($issuerStore.Certificates | Where-Object Thumbprint -eq $issuer.Thumbprint).Count -ne 0) {
            throw 'This CA is already present; refusing to take ownership of existing trust'
        }
        # Vor dem Import schreiben, damit auch ein abgebrochener Import bereinigt werden kann.
        @{ root = $root.Thumbprint; intermediate = $issuer.Thumbprint } | ConvertTo-Json | Set-Content -LiteralPath $marker
        Write-Output ('Confirm temporary test CA in Windows: ' + $root.Thumbprint)
        $rootStore.Add($root)
        if (@($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint).Count -ne 1) {
            throw 'Temporary CA trust was not installed'
        }
    } elseif (Test-Path -LiteralPath $marker) {
        $owned = Get-Content -LiteralPath $marker -Raw | ConvertFrom-Json
        if ($owned.root -ne $root.Thumbprint -or $owned.intermediate -ne $issuer.Thumbprint) {
            throw 'Trust ownership mismatch'
        }
        foreach ($stored in @($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint)) { $rootStore.Remove($stored) }
        foreach ($stored in @($issuerStore.Certificates | Where-Object Thumbprint -eq $issuer.Thumbprint)) { $issuerStore.Remove($stored) }
        if (@($rootStore.Certificates | Where-Object Thumbprint -eq $root.Thumbprint).Count -ne 0 -or
            @($issuerStore.Certificates | Where-Object Thumbprint -eq $issuer.Thumbprint).Count -ne 0) {
            throw 'Temporary CA trust remains installed'
        }
        Remove-Item -LiteralPath $marker
    }
} finally {
    $rootStore.Close()
    $issuerStore.Close()
}
