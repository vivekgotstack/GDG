param([switch]$NoOpen)
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$runtimePath=Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Path $runtimePath -Force | Out-Null
if(-not(Get-Command docker -ErrorAction SilentlyContinue)){throw 'Install Docker Desktop and configure backend/.env first. MeetGrid requires PostgreSQL and Redis; there is no H2 fallback.'}
if(-not(Test-Path (Join-Path $projectRoot 'backend/.env'))){throw 'Copy backend/.env.example to backend/.env and configure it. For local HTTP use SESSION_COOKIE_SECURE=false and APP_URL=http://127.0.0.1:3000.'}
Push-Location $projectRoot
try{
 docker compose --env-file backend/.env up -d --build
 if($LASTEXITCODE -ne 0){throw 'Service startup failed. Read docker compose logs.'}
 $previousApi=$env:API_BASE_URL
 try{
  $env:API_BASE_URL='http://127.0.0.1:8080'
  try{$null=Invoke-WebRequest 'http://127.0.0.1:3000' -TimeoutSec 3;$running=$true}catch{$running=$false}
  if(-not $running){Start-Process -FilePath $env:ComSpec -ArgumentList '/d /c npm.cmd run dev -- --hostname 127.0.0.1 --port 3000' -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimePath 'frontend.log') -RedirectStandardError (Join-Path $runtimePath 'frontend-errors.log') | Out-Null}
 }finally{$env:API_BASE_URL=$previousApi}
 Write-Host 'MeetGrid: http://127.0.0.1:3000. PostgreSQL is the only application database.'
 if(-not $NoOpen){Start-Process 'http://127.0.0.1:3000'}
}finally{Pop-Location}
