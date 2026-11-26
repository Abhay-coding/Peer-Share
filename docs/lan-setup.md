# Pair PeerShare on two Windows laptops

Use the same updated version of PeerShare on both laptops, with a JDK installed. Connect them to a trusted network that lets devices reach each other. Guest or campus Wi-Fi may isolate devices even when they show the same network name.

## 1. Create each laptop’s certificate

On each laptop, open PowerShell in its project folder and enter a local password of at least six characters. The passwords can differ. If PowerShell can’t find `keytool.exe`, set `JAVA_HOME` to your JDK folder first.

```powershell
$env:PEERSHARE_TLS_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Local PeerShare password' -AsSecureString)).Password
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pair-lan.ps1
```

Keep the PowerShell window open. Exchange **only** the public `peer.cer` files. Each laptop keeps its own `identity.p12` and password.

## 2. Trust the other laptop

Save the other laptop’s public certificate as `other-laptop.cer` in your project folder. On each laptop, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\pair-lan.ps1 -Trust .\other-laptop.cer
```

Compare the SHA‑256 fingerprint shown with the one printed on the other laptop. Type `yes` at the certificate prompt if it matches. A paired laptop can browse and download all files in your shared folder.

## 3. Start and connect

On the sender, set `server.enabled=true` and `server.bindAddress=0.0.0.0` in `config.properties`. Check `server.port`. On a laptop that only receives, set `server.enabled=false`.

Start both copies from the same PowerShell windows where you entered the passwords:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

On the sender, copy a sample file into **Shared Files → Open shared folder**. Run `ipconfig` and look up the sender’s current Wi-Fi or Ethernet IPv4 address. On the receiver, enter it in **Peers** with the sender’s port, click **Browse files**, then download the sample.

If the receiver can’t connect, test the port from its PowerShell window. This example uses port `5001`.

```powershell
$senderAddress = Read-Host 'Enter the sender IPv4 address'
Test-NetConnection $senderAddress -Port 5001
```

If TCP fails, check the address, the sender’s port and firewall, and the network. If the Wi-Fi isolates devices, switch both laptops to a trusted network that permits local connections and look up the sender’s new address. Enter the local password again whenever you launch from a new PowerShell window for LAN use.

## macOS or Linux

Use the matching JavaFX libraries described in the [README](../README.md). On the Mac or Linux computer, use Bash and the JDK’s `keytool` in place of the PowerShell pairing script:

```bash
read -rsp 'Local PeerShare password: ' PEERSHARE_TLS_PASSWORD; printf '\n'
export PEERSHARE_TLS_PASSWORD
mkdir -p .peershare-tls
keytool -genkeypair -alias peershare -keyalg EC -groupname secp256r1 -dname CN=PeerShare -validity 730 -keystore .peershare-tls/identity.p12 -storetype PKCS12 -storepass:env PEERSHARE_TLS_PASSWORD
keytool -exportcert -alias peershare -file .peershare-tls/peer.cer -keystore .peershare-tls/identity.p12 -storepass:env PEERSHARE_TLS_PASSWORD
keytool -printcert -file .peershare-tls/peer.cer
```

Exchange the public certificates as above. Save the other laptop’s certificate as `other-laptop.cer`. Compare the SHA‑256 fingerprint before typing `yes` to trust it, then launch:

```bash
keytool -importcert -alias paired-peer -file other-laptop.cer -keystore .peershare-tls/trusted.p12 -storetype PKCS12 -storepass:env PEERSHARE_TLS_PASSWORD
sh scripts/run.sh
```
