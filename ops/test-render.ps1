param([string]$Image = 'rodatours:render-check')
$ErrorActionPreference = 'Stop'
$suffix = [Guid]::NewGuid().ToString('N').Substring(0, 12)
$network = "rodatours-check-$suffix"
$database = "rodatours-db-$suffix"
$web = "rodatours-web-$suffix"
$password = [Guid]::NewGuid().ToString('N')

function Invoke-Docker {
    # Función simple: no interpretar -e como abreviatura de -ErrorAction.
    $result = & docker @args
    if ($LASTEXITCODE -ne 0) { throw 'Falló Docker durante la prueba aislada.' }
    return $result
}

function Start-Web([string]$SchemaMode) {
    Invoke-Docker run -d --name $web --network $network --memory 512m -p '127.0.0.1::10000' `
        -e "DB_URL=jdbc:postgresql://${database}:5432/rodatours" `
        -e 'DB_USERNAME=rodatours' -e "DB_PASSWORD=$password" `
        -e "SPRING_JPA_HIBERNATE_DDL_AUTO=$SchemaMode" `
        -e 'SESSION_COOKIE_SECURE=true' -e 'SERVER_FORWARD_HEADERS_STRATEGY=framework' `
        -e 'JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65.0' `
        $Image | Out-Null
    $binding = Invoke-Docker port $web 10000
    $script:baseUrl = 'http://' + ($binding | Select-Object -First 1)
    for ($i = 0; $i -lt 90; $i++) {
        try {
            $health = Invoke-WebRequest "$script:baseUrl/api/experience-types" -UseBasicParsing -TimeoutSec 3
            if ($health.StatusCode -eq 200) { return }
        } catch { }
        $running = Invoke-Docker inspect --format '{{.State.Running}}' $web
        if ($running -ne 'true') { throw 'El contenedor web terminó antes de estar listo.' }
        Start-Sleep -Seconds 2
    }
    throw 'El health check no respondió correctamente.'
}

try {
    Invoke-Docker network create $network | Out-Null
    Invoke-Docker run -d --name $database --network $network `
        -e 'POSTGRES_DB=rodatours' -e 'POSTGRES_USER=rodatours' `
        -e "POSTGRES_PASSWORD=$password" postgres:16-alpine | Out-Null
    $ready = $false
    for ($i = 0; $i -lt 45; $i++) {
        & docker exec $database pg_isready -U rodatours -d rodatours *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'PostgreSQL aislado no está listo.' }

    Start-Web 'update'
    $homePage = Invoke-WebRequest "$baseUrl/" -UseBasicParsing
    $route = Invoke-WebRequest "$baseUrl/panel/operaciones" -UseBasicParsing
    if ($homePage.Content -notmatch '<div id="root"' -or $homePage.Content -ne $route.Content) {
        throw 'El frontend o el fallback de rutas React no funciona.'
    }
    $csrf = Invoke-WebRequest "$baseUrl/api/auth/csrf" -UseBasicParsing
    $cookies = $csrf.Headers['Set-Cookie'] -join ';'
    if ($cookies -notmatch 'Secure' -or $cookies -notmatch 'HttpOnly' -or $cookies -notmatch 'SameSite=Lax') {
        throw 'Faltan atributos de seguridad en la cookie de sesión.'
    }
    if (($csrf.Content | ConvertFrom-Json).token.Length -eq 0) { throw 'Falta el token CSRF.' }
    try {
        Invoke-WebRequest "$baseUrl/assets/missing.js" -UseBasicParsing | Out-Null
        throw 'Un asset inexistente no debe devolver index.html.'
    } catch {
        if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 404) { throw }
    }
    Write-Output 'OK: esquema nuevo, API, frontend, rutas React, assets y cookie segura/CSRF.'

    Invoke-Docker rm -f $web | Out-Null
    Start-Web 'validate'
    Write-Output 'OK: segundo arranque con validate contra el esquema generado (contenedor limitado a 512 MiB).'
} finally {
    # Sólo recursos efímeros creados por esta ejecución. Nunca la base local/Render.
    & docker rm -f -v $web $database 2>$null | Out-Null
    & docker network rm $network 2>$null | Out-Null
}
