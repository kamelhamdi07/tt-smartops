# Lance le backend et le frontend sans Docker, chacun dans sa propre fenetre PowerShell.
$root = $PSScriptRoot
$mvn  = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd"

# Libere les ports si une ancienne instance tourne encore
foreach ($port in 8080, 4200) {
  Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

# Selon le reseau, Neon n'est joignable qu'en IPv4 ou qu'en IPv6 : on teste avant de lancer Java
function Test-Tcp($ip, $port) {
  $client = New-Object System.Net.Sockets.TcpClient($ip.AddressFamily)
  try { return $client.ConnectAsync($ip, $port).Wait(4000) } catch { return $false } finally { $client.Dispose() }
}
$dbUrl  = (Get-Content "$root\.env" | Where-Object { $_ -match '^DB_URL=' }) -replace '^DB_URL=', ''
$dbHost = ([regex]::Match($dbUrl, '//([^/:?]+)')).Groups[1].Value
$addresses = @()
try { $addresses = [System.Net.Dns]::GetHostAddresses($dbHost) } catch { }
$ipv4 = $addresses | Where-Object { $_.AddressFamily -eq 'InterNetwork' } | Select-Object -First 1
$ipv6 = $addresses | Where-Object { $_.AddressFamily -eq 'InterNetworkV6' } | Select-Object -First 1
if ($ipv4 -and (Test-Tcp $ipv4 5432)) {
  $javaNet = '-Djava.net.preferIPv4Stack=true'; Write-Host "Base Neon joignable en IPv4"
} elseif ($ipv6 -and (Test-Tcp $ipv6 5432)) {
  $javaNet = '-Djava.net.preferIPv6Addresses=true'; Write-Host "Base Neon joignable en IPv6"
} else {
  $javaNet = ''; Write-Warning "Base Neon injoignable (${dbHost}:5432) : verifiez la connexion Internet."
}

$backend = @"
Set-Location '$root\backend'
Get-Content '$root\.env' | ? { `$_ -match '^\s*[^#].*=' } | % { `$k,`$v = `$_ -split '=',2; Set-Item "env:`$(`$k.Trim())" `$v.Trim() }
`$env:JAVA_TOOL_OPTIONS = '$javaNet'
# Journal aussi copie dans backend.log pour le diagnostic
& '$mvn' spring-boot:run 2>&1 | Tee-Object -FilePath '$root\backend.log'
"@
Start-Process powershell -ArgumentList '-NoExit', '-Command', $backend

$frontend = @"
Set-Location '$root\frontend'
if (-not (Test-Path node_modules)) { npm install }
npm start
"@
Start-Process powershell -ArgumentList '-NoExit', '-Command', $frontend

Write-Host "Backend : http://localhost:8080  |  Frontend : http://localhost:4200"
