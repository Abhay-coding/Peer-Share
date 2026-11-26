param([string]$Trust)

$ErrorActionPreference = 'Stop'
if (-not $env:PEERSHARE_TLS_PASSWORD) {
    throw 'Set PEERSHARE_TLS_PASSWORD in this PowerShell window before pairing.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$folder = Join-Path $projectRoot '.peershare-tls'
$identity = Join-Path $folder 'identity.p12'
$certificate = Join-Path $folder 'peer.cer'
$trusted = Join-Path $folder 'trusted.p12'
$keytool = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\keytool.exe'
} else {
    (Get-Command keytool.exe -ErrorAction Stop).Source
}

if ($Trust) {
    $peerCertificate = (Resolve-Path -LiteralPath $Trust -ErrorAction Stop).Path
    $fingerprint = (Get-FileHash -LiteralPath $peerCertificate -Algorithm SHA256).Hash
    Write-Host "Compare this SHA-256 with the other laptop before trusting it: $fingerprint"
    & $keytool -importcert -alias $fingerprint -file $peerCertificate -keystore $trusted -storetype PKCS12 -storepass:env PEERSHARE_TLS_PASSWORD
    exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force $folder | Out-Null
if (-not (Test-Path -LiteralPath $identity)) {
    & $keytool -genkeypair -alias peershare -keyalg EC -groupname secp256r1 -dname 'CN=PeerShare' -validity 730 -keystore $identity -storetype PKCS12 -storepass:env PEERSHARE_TLS_PASSWORD
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
& $keytool -exportcert -alias peershare -file $certificate -keystore $identity -storetype PKCS12 -storepass:env PEERSHARE_TLS_PASSWORD
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Share only $certificate with the other laptop."
Get-FileHash -LiteralPath $certificate -Algorithm SHA256
