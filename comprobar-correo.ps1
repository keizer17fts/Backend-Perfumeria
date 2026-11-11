# Comprueba la configuración local sin enviar mensajes ni mostrar contraseñas.
$ErrorActionPreference = 'Stop'
$config = @{}
$ruta = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path $ruta)) { throw 'Falta .env. Copia .env.example y completa los datos SMTP.' }
foreach ($linea in (Get-Content -LiteralPath $ruta -Encoding UTF8)) {
  if ($linea.Trim() -match '^([A-Za-z_][A-Za-z_0-9]*)\s*=\s*(.*)$') {
    $config[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'")
  }
}
$servidor = if ($config['SMTP_HOST']) { $config['SMTP_HOST'] } else { 'smtp.gmail.com' }
$puertoCorreo = if ($config['SMTP_PORT']) { [int]$config['SMTP_PORT'] } else { 587 }
$cuentaLista = $config['SMTP_USER'] -match '^[^@\s]+@[^@\s]+\.[^@\s]+$'
$claveLista = -not [string]::IsNullOrWhiteSpace($config['SMTP_PASSWORD'])
Write-Host "MAIL_ENABLED activo: $($config['MAIL_ENABLED'] -eq 'true')"
Write-Host "SMTP_USER configurado: $cuentaLista"
Write-Host "SMTP_PASSWORD configurado: $claveLista"
Write-Host "Comprobando conexion TCP a $servidor en puerto $puertoCorreo..."
$conexion = Test-NetConnection -ComputerName $servidor -Port $puertoCorreo -WarningAction SilentlyContinue
Write-Host "Conexion TCP disponible: $($conexion.TcpTestSucceeded)"
Write-Host 'Esta comprobacion no valida la contrasena ni entrega un correo.'
Write-Host 'Para Gmail, usa una contrasena de aplicacion y reinicia todos los microservicios despues de cambiar .env.'
Write-Host 'Despues de una compra confirmada puedes consultar el estado en la pantalla de pago o en la boleta de administracion.'
