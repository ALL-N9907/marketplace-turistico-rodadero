# Solo lectura: no crea preferencias, pagos ni notificaciones. Nunca imprime secretos.
param([switch]$CheckConnectivity)
$ErrorActionPreference = 'Stop'
$secrets = Join-Path $PSScriptRoot 'local-secrets.ps1'
if (Test-Path $secrets) {
    try { . $secrets *> $null } catch { throw 'No se pudo cargar local-secrets.ps1. Revisa su sintaxis sin compartir sus valores.' }
}
function Test-HttpsUrl([string]$Value) {
    $uri = $null
    return ([Uri]::TryCreate($Value, [UriKind]::Absolute, [ref]$uri) -and
        $uri.Scheme -ceq 'https' -and $uri.Host -and -not $uri.UserInfo -and
        -not $uri.Query -and -not $uri.Fragment)
}
$checks = [ordered]@{
    'MP_ENABLED activado' = ($env:MP_ENABLED -ieq 'true')
    'MP_ACCESS_TOKEN formato reconocido (no acredita sandbox)' = ($env:MP_ACCESS_TOKEN -cmatch '^(TEST-|APP_USR-)\S+$')
    'MP_WEBHOOK_SECRET longitud minima 16' = ([string]$env:MP_WEBHOOK_SECRET).Length -ge 16
    'MP_COLLECTOR_ID numerico' = ($env:MP_COLLECTOR_ID -match '^[0-9]+$')
    'APP_PUBLIC_URL HTTPS' = (Test-HttpsUrl $env:APP_PUBLIC_URL)
    'MP_NOTIFICATION_URL HTTPS' = (Test-HttpsUrl $env:MP_NOTIFICATION_URL)
}
foreach ($item in $checks.GetEnumerator()) { Write-Host ('{0}: {1}' -f $item.Key, $item.Value) }
if ($checks.Values -contains $false) {
    throw 'Configuracion incompleta. Corrige los campos False en ops/local-secrets.ps1; no compartas sus valores.'
}
Write-Host 'Configuracion local valida. Esto no confirma las variables del backend en ejecucion ni la firma del webhook.'
$buyerConfigured = -not [string]::IsNullOrWhiteSpace($env:MP_TEST_BUYER_ID)
Write-Host ('Comprador de prueba explicito configurado={0}' -f $buyerConfigured)
if ($buyerConfigured -and ($env:MP_TEST_BUYER_ID -notmatch '^[0-9]+$' -or $env:MP_TEST_BUYER_ID -ceq $env:MP_COLLECTOR_ID)) {
    throw 'MP_TEST_BUYER_ID debe ser numerico, verificado en Cuentas de prueba y distinto del vendedor.'
}
if (-not $CheckConnectivity) { return }
function Test-Endpoint([string]$Label, [string]$Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 20 -MaximumRedirection 0
        Write-Host ('{0}: HTTP={1}' -f $Label, [int]$response.StatusCode)
    } catch {
        $status = 0
        if ($null -ne $_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        Write-Host ('{0}: HTTP={1}; fallo de red/HTTP, sin detalles sensibles.' -f $Label, $status)
    }
}
Test-Endpoint 'Backend local' 'http://127.0.0.1:8080/api/experience-types'
Test-Endpoint 'Frontend HTTPS' $env:APP_PUBLIC_URL
$notification = [Uri]$env:MP_NOTIFICATION_URL
Test-Endpoint 'Backend HTTPS' ($notification.GetLeftPart([UriPartial]::Authority) + '/api/experience-types')
# Solo se envia el token de prueba al host oficial y nunca se muestra la respuesta.
$headers = @{ Authorization = 'Bearer ' + $env:MP_ACCESS_TOKEN }
try {
    $account = Invoke-RestMethod -Uri 'https://api.mercadopago.com/users/me' -Headers $headers -TimeoutSec 20
    $matchesCollector = [string]$account.id -ceq $env:MP_COLLECTOR_ID
    $isTest = @($account.tags) -ccontains 'test_user'
    Write-Host ('Mercado Pago: autenticacion=True; collector coincide={0}; cuenta de prueba={1}' -f $matchesCollector, $isTest)
    if (-not $matchesCollector -or -not $isTest) { throw 'Cuenta no verificada como vendedor de prueba.' }
    if ($buyerConfigured) {
        $buyer = Invoke-RestMethod -Uri ('https://api.mercadopago.com/users/' + $env:MP_TEST_BUYER_ID) -Headers $headers -TimeoutSec 20
        Write-Host ('Comprador permitido: ID coincide={0}; perfil TESTUSER numerico={1}. Confirmar tambien en Cuentas de prueba.' -f
            ([string]$buyer.id -ceq $env:MP_TEST_BUYER_ID), ([string]$buyer.nickname -cmatch '^TESTUSER[0-9]+$'))
    }
} catch {
    $status = 0
    if ($null -ne $_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
    Write-Host ('Mercado Pago: HTTP={0}; no se pudo verificar la cuenta. No se muestran detalles sensibles.' -f $status)
} finally {
    $headers.Clear()
    $account = $null
    $buyer = $null
}
