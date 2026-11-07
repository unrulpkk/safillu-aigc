param([string]$EnvFile = '.env.local')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Install Docker Desktop / Docker Engine with Compose v2 first.' }
docker info --format '{{.ServerVersion}}'
if ($LASTEXITCODE -ne 0) { throw 'Docker Engine is not running. Start Docker Desktop first.' }
if (-not (Test-Path -LiteralPath $EnvFile)) {
    $template = if ($EnvFile -match 'prod') { '.env.prod.example' } else { '.env.example' }
    Copy-Item -LiteralPath $template -Destination $EnvFile
}
foreach ($service in @('secrets-init', 'backend', 'web', 'admin')) {
    docker compose --env-file $EnvFile build $service
    if ($LASTEXITCODE -ne 0) { throw "Image build failed: $service" }
}
docker compose --env-file $EnvFile up -d --no-build --wait --wait-timeout 300
if ($LASTEXITCODE -ne 0) { throw 'Startup failed. Inspect docker compose logs; persistent data has been retained.' }
docker compose --env-file $EnvFile ps
Write-Host 'Safillu is ready. Port defaults: creator http://localhost:8088 ; admin http://localhost:8089 .'
Write-Host 'Initial account passwords are in <SAFILLU_RUNTIME_DIR>/secrets/initial-accounts.json (default .runtime).'
