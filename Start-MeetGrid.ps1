param([switch]$NoOpen)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$runtimePath = Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Path $runtimePath -Force | Out-Null

foreach ($command in @('java', 'mvn.cmd', 'node', 'npm.cmd')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "Install $command and add it to PATH first." }
}
if (-not (Test-Path (Join-Path $projectRoot 'frontend/node_modules/next'))) {
    throw 'Frontend dependencies are missing. Run npm ci inside frontend, then run this script again.'
}

function Test-Endpoint([string]$Url) {
    try { $null = Invoke-RestMethod -Uri $Url -TimeoutSec 3; return $true } catch { return $false }
}

if (-not (Test-Endpoint 'http://127.0.0.1:8080/api/health')) {
    # A generated local owner password stays in the ignored runtime directory.
    $previousAdminPassword = $env:ADMIN_BOOTSTRAP_PASSWORD
    if (-not $env:ADMIN_BOOTSTRAP_PASSWORD) {
        $credentialPath = Join-Path $runtimePath 'admin-credentials.json'
        if (-not (Test-Path -LiteralPath $credentialPath)) {
            $randomBytes = New-Object byte[] 32
            $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
            $generator.GetBytes($randomBytes)
            $generator.Dispose()
            $localAdminEmail = if ($env:ADMIN_EMAIL) { $env:ADMIN_EMAIL } else { 'vivekni1224@nigam' }
            @{ email = $localAdminEmail; password = [Convert]::ToBase64String($randomBytes) } | ConvertTo-Json | Set-Content -LiteralPath $credentialPath -Encoding UTF8
        }
        $localAdmin = Get-Content -LiteralPath $credentialPath -Raw | ConvertFrom-Json
        if ($env:ADMIN_EMAIL -and $env:ADMIN_EMAIL -ne $localAdmin.email) { throw 'ADMIN_EMAIL differs from the saved local credentials. Set ADMIN_BOOTSTRAP_PASSWORD explicitly.' }
        $env:ADMIN_BOOTSTRAP_PASSWORD = $localAdmin.password
        Write-Host 'Local administrator credentials: .runtime/admin-credentials.json (never committed).'
    }
    Write-Host 'Starting the local backend. Logs: .runtime/backend.log'
    try {
    Start-Process -FilePath $env:ComSpec -ArgumentList '/d /c mvn.cmd -B -ntp spring-boot:run -Dspring-boot.run.profiles=local' `
        -WorkingDirectory (Join-Path $projectRoot 'backend') -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimePath 'backend.log') `
        -RedirectStandardError (Join-Path $runtimePath 'backend-errors.log') | Out-Null
    } finally { $env:ADMIN_BOOTSTRAP_PASSWORD = $previousAdminPassword }
    $deadline = (Get-Date).AddSeconds(90)
    while (-not (Test-Endpoint 'http://127.0.0.1:8080/api/health')) {
        if ((Get-Date) -gt $deadline) { throw 'Backend did not start. Read .runtime/backend.log and .runtime/backend-errors.log.' }
        Start-Sleep -Seconds 2
    }
}

if (-not (Test-Endpoint 'http://localhost:3000/api/health')) {
    Write-Host 'Starting the frontend. Logs: .runtime/frontend.log'
    $previousApi = $env:API_BASE_URL
    try {
        $env:API_BASE_URL = 'http://127.0.0.1:8080'
        Start-Process -FilePath $env:ComSpec -ArgumentList '/d /c npm.cmd run dev -- --hostname 127.0.0.1 --port 3000' `
            -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $runtimePath 'frontend.log') `
            -RedirectStandardError (Join-Path $runtimePath 'frontend-errors.log') | Out-Null
    } finally { $env:API_BASE_URL = $previousApi }
    $deadline = (Get-Date).AddSeconds(90)
    while (-not (Test-Endpoint 'http://localhost:3000/api/health')) {
        if ((Get-Date) -gt $deadline) { throw 'Frontend did not start. Read .runtime/frontend.log and .runtime/frontend-errors.log.' }
        Start-Sleep -Seconds 2
    }
}
Write-Host 'MeetGrid is running at http://localhost:3000. Both servers stay running after this script exits.'
if (-not $NoOpen) { Start-Process 'http://localhost:3000' }
