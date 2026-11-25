# Prueba activa del receptor: crea un evento local para un pago de prueba EXISTENTE.
# No crea preferencias ni pagos. No demuestra una entrega originada por Mercado Pago.
param([string]$PaymentId = '')
$ErrorActionPreference = 'Stop'
$secrets = Join-Path $PSScriptRoot 'local-secrets.ps1'
try { if (Test-Path $secrets) { . $secrets *> $null } }
catch { throw 'No se pudo cargar la configuracion privada. No comparta sus valores.' }
$endpoint = $null
if ($env:MP_ENABLED -ine 'true' -or $env:MP_ACCESS_TOKEN -cnotmatch '^(TEST-|APP_USR-)\S+$' -or
    ([string]$env:MP_WEBHOOK_SECRET).Length -lt 16 -or $env:MP_COLLECTOR_ID -notmatch '^[0-9]+$' -or
    -not [Uri]::TryCreate($env:MP_NOTIFICATION_URL, [UriKind]::Absolute, [ref]$endpoint) -or
    $endpoint.Scheme -cne 'https' -or $endpoint.UserInfo -or $endpoint.Query -or $endpoint.Fragment -or
    $endpoint.AbsolutePath -cne '/api/payments/webhook') {
    throw 'Configuracion invalida: se requiere sandbox y la URL HTTPS exacta del webhook, sin query ni credenciales.'
}
function Get-Provider([string]$Path) {
    try { return Invoke-RestMethod -Uri ('https://api.mercadopago.com' + $Path) -Headers $script:providerHeaders -TimeoutSec 20 -MaximumRedirection 0 }
    catch { throw 'No se pudo consultar Mercado Pago. No se muestran cuerpos, cabeceras ni excepciones remotas.' }
}
function Send-Notification([string]$RequestId, [string]$Signature) {
    $headers = @{ 'x-request-id' = $RequestId; 'x-signature' = $Signature }
    try {
        $response = Invoke-WebRequest -Uri ($endpoint.AbsoluteUri + '?data.id=' + $PaymentId + '&type=payment') -Method Post -Headers $headers -ContentType 'application/json' -Body '{}' -UseBasicParsing -TimeoutSec 20 -MaximumRedirection 0
        return [int]$response.StatusCode
    } catch {
        if ($null -ne $_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    } finally { $headers.Clear() }
}
$providerHeaders = @{ Authorization = 'Bearer ' + $env:MP_ACCESS_TOKEN }
$hmac = $null
try {
    $seller = Get-Provider '/users/me'
    if ([string]$seller.id -cne $env:MP_COLLECTOR_ID -or @($seller.tags) -cnotcontains 'test_user') {
        throw 'Operacion bloqueada: no se verifico el vendedor de prueba.'
    }
    if (-not $PaymentId) {
        $search = Get-Provider '/v1/payments/search?sort=date_created&criteria=desc&limit=20'
        $approved = @($search.results | Where-Object { $_.status -ceq 'approved' -and [string]$_.collector_id -ceq $env:MP_COLLECTOR_ID })
        if ($approved.Count -ne 1) { throw 'Indique -PaymentId: no hay exactamente un pago aprobado entre los ultimos 20.' }
        $PaymentId = [string]$approved[0].id
    }
    if ($PaymentId -notmatch '^[0-9]{1,30}$') { throw 'PaymentId debe ser numerico.' }
    $payment = Get-Provider ('/v1/payments/' + $PaymentId)
    if ([string]$payment.id -cne $PaymentId -or $payment.status -cne 'approved' -or
        [string]$payment.collector_id -cne $env:MP_COLLECTOR_ID -or $payment.currency_id -cne 'COP' -or
        [string]::IsNullOrWhiteSpace([string]$payment.external_reference) -or $payment.live_mode -isnot [bool]) {
        throw 'El pago no es un aprobado COP verificable del vendedor configurado.'
    }
    if ($payment.live_mode) {
        if ($env:MP_TEST_BUYER_ID -notmatch '^[0-9]+$' -or $env:MP_TEST_BUYER_ID -ceq $env:MP_COLLECTOR_ID -or
            [string]$payment.payer.id -cne $env:MP_TEST_BUYER_ID) { throw 'Comprador de prueba no permitido.' }
        $buyer = Get-Provider ('/users/' + $env:MP_TEST_BUYER_ID)
        if ([string]$buyer.id -cne $env:MP_TEST_BUYER_ID -or [string]$buyer.nickname -cnotmatch '^TESTUSER[0-9]+$') {
            throw 'Perfil del comprador de prueba no verificable.'
        }
    }
    $requestId = 'rodatours-diagnostic-' + [Guid]::NewGuid().ToString('N')
    $ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString()
    $manifest = 'id:' + $PaymentId + ';request-id:' + $requestId + ';ts:' + $ts + ';'
    $hmac = New-Object Security.Cryptography.HMACSHA256
    $hmac.Key = [Text.Encoding]::UTF8.GetBytes($env:MP_WEBHOOK_SECRET)
    $signature = -join ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($manifest)) | ForEach-Object { $_.ToString('x2') })
    $header = 'ts=' + $ts + ',v1=' + $signature
    Write-Host ('Pago existente de prueba verificado. ID={0}. No se realiza ningun cobro.' -f $PaymentId)
    $first = Send-Notification $requestId $header
    Write-Host ('Notificacion nueva: HTTP={0}' -f $first)
    if ($first -ne 200) { throw 'La notificacion valida no fue aceptada. No se enviaran mas casos.' }
    $duplicate = Send-Notification $requestId $header
    Write-Host ('Duplicado exacto: HTTP={0}' -f $duplicate)
    $invalid = Send-Notification ('rodatours-invalid-' + [Guid]::NewGuid().ToString('N')) ('ts=' + $ts + ',v1=' + ('0' * 64))
    Write-Host ('Firma invalida: HTTP={0}; rechazada={1}' -f $invalid, ($invalid -in @(401,403)))
    if ($duplicate -ne 200 -or $invalid -notin @(401,403)) { throw 'Prueba HTTP fallida. Revisar el receptor sin desactivar la firma.' }
    Write-Host 'HTTP correcto. Falta verificar persistencia/procesamiento en ADMIN > /panel/operaciones > Eventos de pago:'
    Write-Host 'Debe existir UN solo evento nuevo para este pago, DONE con 1 intento, sin reintento manual.'
    Write-Host 'La firma invalida no debe crear eventos. El pago debe seguir APPROVED y la reserva CONFIRMED.'
    Write-Host 'Esta prueba simula el emisor; no acredita una nueva entrega originada en Mercado Pago.'
} finally {
    $providerHeaders.Clear()
    if ($null -ne $hmac) { $hmac.Dispose() }
    $signature = $null; $header = $null; $payment = $null; $search = $null; $approved = $null; $buyer = $null; $seller = $null
}
