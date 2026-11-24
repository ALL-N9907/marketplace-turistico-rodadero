param(
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbName = 'rodatours',
    [string]$DbUser = 'rodatours',
    [switch]$BootstrapAdmin
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (!(Test-Path (Join-Path $root 'pom.xml'))) { throw 'No se encontró la raíz de Rodatours.' }
if (!(Get-Command java -ErrorAction SilentlyContinue)) { throw 'Instala Java 21 y configura JAVA_HOME/PATH.' }
$names = @('DB_URL','DB_USERNAME','DB_PASSWORD','ADMIN_EMAIL','ADMIN_PASSWORD')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
Push-Location $root
try {
    $password = Read-Host "Contraseña PostgreSQL del usuario $DbUser (no se guarda en archivos)" -AsSecureString
    $env:DB_URL = "jdbc:postgresql://${DbHost}:${DbPort}/${DbName}"
    $env:DB_USERNAME = $DbUser
    $env:DB_PASSWORD = (New-Object System.Management.Automation.PSCredential($DbUser,$password)).GetNetworkCredential().Password
    if ($BootstrapAdmin) {
        $env:ADMIN_EMAIL = Read-Host 'Correo para el primer administrador (debe ser nuevo)'
        $admin = Read-Host 'Contraseña inicial de administrador (mínimo 12 caracteres)' -AsSecureString
        $env:ADMIN_PASSWORD = (New-Object System.Management.Automation.PSCredential('admin',$admin)).GetNetworkCredential().Password
        if ($env:ADMIN_PASSWORD.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($env:ADMIN_PASSWORD) -gt 72) { throw 'La contraseña del administrador no cumple la política.' }
    } else {
        Remove-Item Env:ADMIN_EMAIL -ErrorAction SilentlyContinue
        Remove-Item Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
    }
    Write-Host 'Iniciando Spring Boot con perfil local. No se imprime ninguna contraseña.'
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
    if ($LASTEXITCODE -ne 0) { throw 'El backend no arrancó. Revisa el primer Caused by anterior al resumen de Maven.' }
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') }
    Pop-Location
}
