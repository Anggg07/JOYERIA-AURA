# ============================================================================
# run-dev.ps1 — Levanta AURA (backend + frontend) en desarrollo.
# Carga las variables de .env (incluida AI_API_KEY) sin exponerlas en el código.
# Uso:  powershell -ExecutionPolicy Bypass -File .\run-dev.ps1
# ============================================================================
$ErrorActionPreference = "Continue"

$repo     = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend  = Join-Path $repo "joyeria-backend"
$frontend = Join-Path $repo "joyeria-frontend"
$tmp      = Join-Path $env:TEMP "opencode"
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

# 1) Cargar .env en variables de entorno del proceso -------------------------
$envFile = Join-Path $repo ".env"
if (Test-Path -LiteralPath $envFile) {
  Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
      $eq = $line.IndexOf("=")
      $k = $line.Substring(0, $eq).Trim()
      $v = $line.Substring($eq + 1).Trim().Trim('"')
      if ($k) { Set-Item -Path ("Env:" + $k) -Value $v }
    }
  }
  if ($env:AI_API_KEY) {
    "AURA: IA ACTIVA (proveedor=$($env:AI_PROVIDER), modelo=$($env:AI_MODEL))"
  } else {
    "AURA: sin AI_API_KEY -> modo determinista (pega tu key en .env)"
  }
} else {
  "AURA: no hay .env -> modo determinista. Copia .env.example a .env."
}

# 2) JAVA_HOME (si no está definido) ----------------------------------------
if (-not $env:JAVA_HOME) {
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 3) Backend (8080) -----------------------------------------------------------
if (-not (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
  Start-Process -FilePath "mvn.cmd" -ArgumentList @("-o", "spring-boot:run") -WorkingDirectory $backend `
    -WindowStyle Hidden -RedirectStandardOutput (Join-Path $tmp "api-boot.out") `
    -RedirectStandardError (Join-Path $tmp "api-boot.err") | Out-Null
  $ok = $false
  foreach ($i in 1..60) {
    Start-Sleep 2
    if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { "backend 8080 LISTO"; $ok = $true; break }
  }
  if (-not $ok) { "backend NO arrancó. Revisa $tmp\api-boot.out" }
} else {
  "backend 8080 ya estaba arriba"
}

# 4) Frontend (5173) ----------------------------------------------------------
if (-not (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue)) {
  Start-Process -FilePath "npm.cmd" -ArgumentList @("run", "dev") -WorkingDirectory $frontend `
    -WindowStyle Hidden -RedirectStandardOutput (Join-Path $tmp "front.out") `
    -RedirectStandardError (Join-Path $tmp "front.err") | Out-Null
  foreach ($i in 1..20) {
    Start-Sleep 2
    if (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue) { "frontend 5173 LISTO"; break }
  }
} else {
  "frontend 5173 ya estaba arriba"
}

"Tienda: http://localhost:5173      API: http://localhost:8080"
