$ErrorActionPreference = 'Continue'
Write-Host 'Herramientas necesarias (no se inspeccionan contraseñas):'
foreach ($command in @('java','node','npm','docker')) {
    $found = Get-Command $command -ErrorAction SilentlyContinue
    if ($found) { Write-Host "$command : disponible" } else { Write-Host "$command : NO encontrado" }
}
foreach ($port in @(5432,8080,5173)) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $task = $client.ConnectAsync('127.0.0.1',$port)
        if ($task.Wait(2000) -and $client.Connected) { Write-Host "Puerto $port : abierto" }
        else { Write-Host "Puerto $port : sin servicio accesible" }
    } catch { Write-Host "Puerto $port : sin servicio accesible" }
    finally { $client.Dispose() }
}
Write-Host 'El puerto abierto no garantiza usuario, contraseña o esquema correctos.'
try {
    $response = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/experience-types' -UseBasicParsing -TimeoutSec 5
    Write-Host "Catálogo backend: HTTP $($response.StatusCode)"
} catch { Write-Host 'El catálogo backend todavía no está disponible. Revisa el registro de Spring Boot.' }
