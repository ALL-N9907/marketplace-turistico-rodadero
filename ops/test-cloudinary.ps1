# Diagnóstico de sólo lectura: no carga ni elimina archivos y no imprime secretos.
$ErrorActionPreference = 'Stop'
$secrets = Join-Path $PSScriptRoot 'local-secrets.ps1'
if (Test-Path $secrets) { . $secrets }
$cloud = $env:CLOUDINARY_CLOUD_NAME
$key = $env:CLOUDINARY_API_KEY
$secret = $env:CLOUDINARY_API_SECRET
if ($cloud -notmatch '^[A-Za-z0-9_-]+$' -or $key -notmatch '^\d+$' -or [string]::IsNullOrWhiteSpace($secret) -or $secret -match '[<>\s]') {
    throw 'CONFIGURACION: cloud, API key o API secret faltantes o con marcadores/espacios. No comparta sus valores.'
}
Write-Host "Cloud=$cloud; API key numérica=True; secret presente=True"
foreach ($name in @('CLOUDINARY_VIDEO_PRESET','CLOUDINARY_IMAGE_PRESET')) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    Write-Host ("{0}: definido={1}" -f $name,(-not [string]::IsNullOrWhiteSpace($value)))
}
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${key}:${secret}")) }
try {
    # Admin API: verifica pertenencia y permisos de la pareja key/secret, sin mutaciones.
    $null = Invoke-RestMethod -Uri "https://api.cloudinary.com/v1_1/$cloud/resources/image/upload?max_results=1" -Headers $headers -TimeoutSec 20
    Write-Host 'Admin API HTTP=200; autenticación correcta. Esto NO valida la firma de upload ni los presets.'
} catch {
    $status = 0
    if ($null -ne $_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
    $category = switch ($status) { 401 {'AUTENTICACION'} 403 {'PERMISOS'} 404 {'NUBE_O_RUTA'} 429 {'LIMITE'} 0 {'RED'} default {'PROVEEDOR'} }
    # No emitir la excepción original: podría contener cabeceras/cuerpos del proveedor.
    throw "Cloudinary categoria=$category HTTP=$status. No se muestran detalles sensibles."
} finally {
    $headers.Clear()
    $key = $null
    $secret = $null
}
