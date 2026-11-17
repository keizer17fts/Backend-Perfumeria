# Iniciar Maison Luxury desde PowerShell: .\iniciar-backend.ps1
param([switch]$Firebase)
$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
Set-Location $raiz


# Preferir Java 21 instalado y compartir la seleccion con los procesos hijos.
$candidatosJava = @($env:JAVA_HOME)
foreach ($carpetaJava in @(
  "$env:ProgramFiles\Java",
  "$env:ProgramFiles\Eclipse Adoptium",
  "$env:ProgramFiles\Microsoft"
)) {
  if (Test-Path -LiteralPath $carpetaJava) {
    $candidatosJava += @(Get-ChildItem -LiteralPath $carpetaJava -Directory |
      Select-Object -ExpandProperty FullName)
  }
}
$comandoJava = Get-Command java.exe -ErrorAction SilentlyContinue
if ($comandoJava) {
  $candidatosJava += Split-Path (Split-Path $comandoJava.Source -Parent) -Parent
}
$javaElegido = $null
foreach ($candidato in $candidatosJava) {
  if ([string]::IsNullOrWhiteSpace($candidato)) { continue }
  $releaseJava = Join-Path $candidato 'release'
  if ((Test-Path -LiteralPath $releaseJava) -and
      (Test-Path -LiteralPath (Join-Path $candidato 'bin\javac.exe'))) {
    if (Select-String -LiteralPath $releaseJava -Pattern '^JAVA_VERSION="21(\.|")' -Quiet) {
      $javaElegido = $candidato
      break
    }
  }
}
if (-not $javaElegido) {
  throw 'No se encontro JDK 21. Si esta en otra carpeta, define JAVA_HOME con su ruta y vuelve a ejecutar.'
}
$env:JAVA_HOME = $javaElegido
$env:Path = "$(Join-Path $javaElegido 'bin');$env:Path"
Write-Host "Java seleccionado: $javaElegido"

# Reutilizar la cache propia que funciono en el laboratorio.
$env:MAISON_MAVEN_REPOSITORY = Join-Path $env:LOCALAPPDATA 'MaisonLuxury\maven-repository'
New-Item -ItemType Directory -Force -Path $env:MAISON_MAVEN_REPOSITORY | Out-Null
$pruebaEscritura = Join-Path $env:MAISON_MAVEN_REPOSITORY ([guid]::NewGuid().ToString() + '.tmp')
try {
  [IO.File]::WriteAllText($pruebaEscritura, 'Maison')
} finally {
  if (Test-Path -LiteralPath $pruebaEscritura) { Remove-Item -LiteralPath $pruebaEscritura }
}
Write-Host "Cache de Maven: $env:MAISON_MAVEN_REPOSITORY"

foreach ($cmd in @('java', 'mvn')) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
    throw "Falta '$cmd' en PATH. Instalalo y vuelve a abrir la terminal."
  }
}

if (-not (Test-Path -LiteralPath (Join-Path $raiz '.env'))) {
  Copy-Item -LiteralPath (Join-Path $raiz '.env.example') -Destination (Join-Path $raiz '.env')
}

# Cargar las opciones SMTP privadas del .env compartido en todos los procesos Java.
# Solo se leen nombres concretos; nunca se imprimen contrasenas en la consola.
$clavesCorreo = @('SMTP_HOST', 'SMTP_PORT', 'SMTP_AUTH', 'SMTP_STARTTLS', 'SMTP_USER', 'SMTP_PASSWORD', 'MAIL_FROM', 'MAIL_ENABLED', 'MAISON_JWT_SECRET')
foreach ($linea in (Get-Content -LiteralPath (Join-Path $raiz '.env') -Encoding UTF8)) {
  $texto = $linea.Trim()
  if ($texto -match '^([A-Za-z_][A-Za-z_0-9]*)\s*=\s*(.*)$') {
    $nombre = $Matches[1]
    if ($clavesCorreo -contains $nombre) {
      $valor = $Matches[2].Trim()
      if ($valor.Length -ge 2 -and (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or ($valor.StartsWith("'") -and $valor.EndsWith("'")))) {
        $valor = $valor.Substring(1, $valor.Length - 2)
      }
      [Environment]::SetEnvironmentVariable($nombre, $valor, 'Process')
    }
  }
}

$correoRemitente = [string]$env:SMTP_USER
$claveAplicacion = [string]$env:SMTP_PASSWORD
if ($env:SMTP_HOST -eq 'smtp.gmail.com') {
  $claveAplicacion = $claveAplicacion -replace '\s', ''
  if ($claveAplicacion -and $claveAplicacion.Length -ne 16) {
    Write-Warning 'Gmail requiere una clave de aplicacion de 16 caracteres en SMTP_PASSWORD.'
    $claveAplicacion = ''
  }
}
if ($correoRemitente -match '^[^@\s]+@[^@\s]+\.[^@\s]+$' -and
    $claveAplicacion.Length -gt 0 -and $env:MAIL_ENABLED -ne 'false') {
  $env:SMTP_PASSWORD = $claveAplicacion
  if ([string]::IsNullOrWhiteSpace($env:MAIL_FROM)) { $env:MAIL_FROM = $correoRemitente }
  $env:MAIL_ENABLED = 'true'
  Write-Host "Correo transaccional habilitado para Catalogo, Usuarios y Ordenes ($correoRemitente)."
} else {
  $env:MAIL_ENABLED = 'false'
  Write-Warning 'Correo desactivado: completa SMTP_USER y SMTP_PASSWORD en .env y vuelve a iniciar el proyecto.'
}
$claveAplicacion = $null

# No iniciar servicios duplicados: las credenciales/configuracion solo se leen al arrancar.
foreach ($puerto in 8080,8081,8082,8083) {
  if (Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue) {
    throw "El puerto $puerto ya esta ocupado. Deten los procesos anteriores y reinicia el proyecto."
  }
}

if ($Firebase) {
  $env:SPRING_PROFILES_ACTIVE = 'firebase'
  Write-Host 'Iniciando los servicios con el perfil Firebase PostgreSQL.'
}
# Crear una clave privada compartida en el primer arranque.
if (-not $env:MAISON_JWT_SECRET) {
  $bytes = New-Object byte[] 32
  $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
  try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
  $env:MAISON_JWT_SECRET = [Convert]::ToBase64String($bytes)
  [IO.File]::AppendAllText((Join-Path $raiz '.env'), "`r`nMAISON_JWT_SECRET=$env:MAISON_JWT_SECRET`r`n")
}
foreach ($svc in @('ms-catalogo', 'ms-usuarios', 'ms-ordenes', 'gateway')) {
  Write-Host "Iniciando $svc..."
  Start-Process -FilePath 'powershell.exe' -WorkingDirectory (Join-Path $raiz "backend/$svc") -ArgumentList @('-NoExit', '-Command', '& mvn "-Dmaven.repo.local=$env:MAISON_MAVEN_REPOSITORY" spring-boot:run')
}
Remove-Item Env:SMTP_PASSWORD -ErrorAction SilentlyContinue
