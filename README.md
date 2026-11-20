# Maison Luxury — Backend

API de la perfumería Maison Luxury, desarrollada como proyecto de Full Stack. Atiende el catálogo, las cuentas de usuario y las compras del frontend React. El proyecto está separado en dos repositorios: este contiene el backend y el otro contiene la interfaz web.

## Arquitectura

El navegador consulta al gateway en `http://localhost:8080/api/v1`. El gateway envía cada petición al microservicio correspondiente.

| Componente | Puerto | Función | Esquema PostgreSQL |
| --- | --- | --- | --- |
| gateway | 8080 | Rutas de la API y CORS | No utiliza base de datos |
| ms-catalogo | 8081 | Productos, categorías, ofertas, cupones y reseñas | maison_catalogo |
| ms-usuarios | 8082 | Registro, inicio de sesión y usuarios | maison_usuarios |
| ms-ordenes | 8083 | Órdenes, boletas y mensajes | maison_ordenes |

Los servicios usan Spring Boot 3.3.5, Spring Data JPA, Spring Security y Flyway. La configuración para la presentación utiliza PostgreSQL en Google Cloud SQL. El perfil se llama `firebase` por la configuración original del proyecto; la conexión de los servicios es JDBC a PostgreSQL.

## Requisitos

- JDK 21 recomendado y Maven 3.9 o superior disponibles en PATH.
- PowerShell en Windows para ejecutar los comandos de este documento.
- Acceso a Internet para la base de datos y para la primera descarga de dependencias.
- Puertos 8080, 8081, 8082 y 8083 libres.

El lanzador busca primero JDK 21 y, si no lo encuentra, permite JDK 25. Los POM compilan para Java 21; para repetir el entorno comprobado en el laboratorio, usar JDK 21. Permitir JDK 25 en el script no garantiza compatibilidad de todas las dependencias con esa versión.

Comprobar el entorno:

```powershell
java -version
mvn.cmd -version
```

Si Maven está usando otro JDK, seleccionar el instalado en esa terminal. Ajustar la ruta a la carpeta real del equipo:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn.cmd -version
```

La selección dura mientras esté abierta esa terminal. Repetirla en las terminales independientes usadas para el inicio manual.

## Configuración inicial

Abrir PowerShell en la raíz del repositorio, donde están `iniciar-backend.ps1` y `.env.example`.

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Completar estos valores sin comillas envolventes y guardar el archivo:

```dotenv
MAISON_PG_URL=jdbc:postgresql://34.176.108.103:5432/postgres?sslmode=require
MAISON_PG_USER=postgres
MAISON_PG_PASSWORD=REEMPLAZAR_POR_LA_CLAVE_REAL
MAIL_ENABLED=false
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_USER=
SMTP_PASSWORD=
```

La contraseña es la del usuario PostgreSQL. No es la contraseña de Google. No subir `.env` a GitHub ni reemplazar un archivo configurado por la plantilla vacía.

Los tres microservicios leen el `.env` de la raíz cuando se ejecutan desde `backend/ms-catalogo`, `backend/ms-usuarios` y `backend/ms-ordenes`. Un `.env` dentro de un microservicio o variables de entorno anteriores pueden sobrescribir valores: mantener una sola configuración compartida y revisar esas copias si aparece un dato inesperado.

### Clave compartida de autenticación

El lanzador crea `MAISON_JWT_SECRET` en `.env` si no existe. Para un primer arranque completamente manual, generar la clave una sola vez desde la raíz:

```powershell
if (-not (Select-String -Path .env -Pattern '^MAISON_JWT_SECRET=.+$' -Quiet)) {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $secreto = [Convert]::ToBase64String($bytes)
    Add-Content -Path .env -Value "`r`nMAISON_JWT_SECRET=$secreto"
    Remove-Variable secreto
}
```

Los tres servicios deben utilizar la misma clave. Cambiarla invalida las sesiones existentes.

### Acceso a Cloud SQL desde otra red

En el PC donde se ejecutará el backend:

```powershell
curl.exe -4 https://api.ipify.org
Test-NetConnection 34.176.108.103 -Port 5432
```

La IP devuelta por el primer comando es la IP pública que debe estar autorizada en Cloud SQL. La IP puede cambiar aunque se use el mismo computador. La comprobación esperada es `TcpTestSucceeded : True`; un ping correcto no comprueba el puerto PostgreSQL.

Si hace falta autorizar la IP, abrir Cloud Shell desde el navegador con la cuenta del proyecto y consultar las redes existentes:

```bash
gcloud sql instances describe maison-652f9-instance --project=maison-652f9 --format="yaml(state,settings.ipConfiguration.authorizedNetworks)"
```

Actualizar la lista conservando las redes que aún se necesiten. Sustituir los textos de este ejemplo por las IP reales; no ejecutarlo literalmente:

```bash
gcloud sql instances patch maison-652f9-instance --project=maison-652f9 --authorized-networks="IP_EXISTENTE/32,IP_NUEVA/32"
```

Este comando reemplaza la lista completa de redes autorizadas. Autorizar direcciones concretas, no `0.0.0.0/0`. Al terminar la operación, repetir `Test-NetConnection` en el PC. Si continúa fallando, revisar la IP actual y si la red permite salir por el puerto 5432.

Con esta conexión directa no se necesita instalar Google Cloud CLI ni dejar un proxy abierto en el computador de la presentación. Los comandos `gcloud` se ejecutan en Cloud Shell, no en la consola `postgres=>`.

## Opción 1 — Iniciar con el lanzador

Desde la raíz del repositorio:

```powershell
Unblock-File .\iniciar-backend.ps1
.\iniciar-backend.ps1 -Firebase
```

El script selecciona Java, prepara una caché de Maven en `%LOCALAPPDATA%\MaisonLuxury\maven-repository`, revisa los puertos, configura el correo y abre cuatro terminales. Esperar que los servicios muestren `Started ...Application` antes de probar la página. Abrir una ventana no significa que el servicio haya terminado de iniciar.

Usar `-Firebase` para la base en la nube. Sin ese parámetro, el perfil predeterminado de los microservicios es H2 local y no corresponde a la presentación con PostgreSQL.

Si PowerShell sigue bloqueando el script descargado, se puede habilitar su ejecución únicamente para esa sesión, si las políticas del equipo lo permiten:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\iniciar-backend.ps1 -Firebase
```

Si el equipo tiene una política institucional que lo impide, usar el inicio manual descrito a continuación.

## Opción 2 — Iniciar cada servicio manualmente

La configuración de `.env`, la clave JWT y el acceso a Cloud SQL deben estar preparados antes. Abrir cuatro terminales PowerShell **en la raíz del backend**. Verificar el JDK en cada una.

Los comandos utilizan la misma caché que el lanzador. En Windows, `--%` evita que PowerShell divida las propiedades `-D` de Maven. Copiar cada comando Maven completo en una sola línea.

**Terminal 1 — Catálogo**

```powershell
cd .\backend\ms-catalogo
mvn.cmd --% -Dmaven.repo.local=%LOCALAPPDATA%\MaisonLuxury\maven-repository -Dspring-boot.run.profiles=firebase spring-boot:run
```

**Terminal 2 — Usuarios**

```powershell
cd .\backend\ms-usuarios
mvn.cmd --% -Dmaven.repo.local=%LOCALAPPDATA%\MaisonLuxury\maven-repository -Dspring-boot.run.profiles=firebase spring-boot:run
```

**Terminal 3 — Órdenes**

```powershell
cd .\backend\ms-ordenes
mvn.cmd --% -Dmaven.repo.local=%LOCALAPPDATA%\MaisonLuxury\maven-repository -Dspring-boot.run.profiles=firebase spring-boot:run
```

**Terminal 4 — Gateway**

```powershell
cd .\backend\gateway
mvn.cmd --% -Dmaven.repo.local=%LOCALAPPDATA%\MaisonLuxury\maven-repository spring-boot:run
```

No ejecutar la opción manual mientras las ventanas del lanzador sigan atendiendo los mismos puertos. Para pasar del lanzador al inicio manual, detener primero los cuatro procesos con `Ctrl+C`. La caché descargada permanece en ese PC y se reutiliza; mantener el argumento `-Dmaven.repo.local` en los comandos manuales.

El frontend se inicia por separado desde su repositorio con `npm.cmd run dev` y se abre en `http://localhost:5173`.

## Correo y acceso con Google

Para probar correos, completar en `.env`:

```dotenv
MAIL_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_USER=TU_CORREO
SMTP_PASSWORD=TU_CLAVE_DE_APLICACION
MAIL_FROM=TU_CORREO
```

Usar una contraseña de aplicación de Gmail, sin espacios, y reiniciar los servicios después de guardar. Si el lanzador informa que el correo está desactivado, revisar `.env` y no solo `.env.example`.

Para Google, agregar `GOOGLE_CLIENT_ID` al `.env` del backend y usar el mismo valor en `VITE_GOOGLE_CLIENT_ID` del frontend. El origen usado en el navegador debe estar autorizado en el cliente OAuth. El inicio de sesión con correo y contraseña puede probarse independientemente de Google.

## Comprobar el funcionamiento

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/catalogo/salud
Invoke-RestMethod http://localhost:8080/api/v1/productos
Invoke-RestMethod http://localhost:8080/api/v1/ordenes/salud
```

Después, desde el frontend:

1. Consultar catálogo y detalle de un perfume.
2. Iniciar sesión como cliente, agregar un producto al carrito y registrar una compra de prueba.
3. Revisar la orden y descargar la boleta.
4. Ingresar como administrador y crear, editar y consultar un producto.
5. Comprobar que una cuenta sin permisos no pueda administrar usuarios.

Las compras y los cambios de inventario realizados contra Cloud SQL quedan guardados en la base compartida. La pantalla de pago del proyecto no debe presentarse como evidencia de un cobro bancario real.

En PostgreSQL, para revisar órdenes recientes:

```sql
SELECT id, fecha, estado, total
FROM maison_ordenes.ordenes
ORDER BY id DESC
LIMIT 10;
```

## Documentación y pruebas

Swagger de cada microservicio, una vez iniciado:

- Catálogo: http://localhost:8081/swagger-ui.html
- Usuarios: http://localhost:8082/swagger-ui.html
- Órdenes: http://localhost:8083/swagger-ui.html

Desde la carpeta del microservicio que se desea probar:

```powershell
mvn.cmd --% -Dmaven.repo.local=%LOCALAPPDATA%\MaisonLuxury\maven-repository test
```

Repetir en `ms-catalogo`, `ms-usuarios`, `ms-ordenes` y `gateway`. Los recursos de pruebas tienen su propia configuración. No activar el perfil `firebase` para las pruebas locales; si quedó definido en esa terminal, quitarlo antes:

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
```

Para descargar dependencias y compilar antes de la presentación, ejecutar en cada servicio:

```powershell
mvn.cmd --% -Dmaven.repo.local=%LOCALAPPDATA%\MaisonLuxury\maven-repository -U clean install
```

La primera ejecución necesita red. Tener dependencias descargadas reduce esa necesidad, pero la aplicación sigue requiriendo conexión a Cloud SQL.

## Errores que pueden aparecer al iniciar

| Mensaje | Qué revisar |
| --- | --- |
| `Connect timed out` | IP pública autorizada, instancia disponible y puerto 5432 accesible. |
| `no password was provided` | `MAISON_PG_PASSWORD` vacío, `.env` fuera de la raíz o variable de entorno que lo sobrescribe. |
| `password authentication failed` | Contraseña del usuario PostgreSQL. |
| `remaining connection slots...` | Servicios duplicados o conexiones ocupadas. Detener instancias anteriores; los pools del perfil nube están limitados a 3 conexiones por servicio. |
| `No plugin found for prefix '.repo.local=C'` | Propiedad Maven dividida por PowerShell. Utilizar los comandos completos con `--%` o el lanzador incluido. |
| `429 Too Many Requests` | Maven Central está limitando las descargas. Esperar y reintentar un servicio a la vez; no borrar toda la caché. |
| Puerto ocupado | Detener el proceso anterior con `Ctrl+C` en su terminal. |
| `403 Forbidden` | Revisar el rol de la cuenta para esa operación. |
| `401 Unauthorized` | Volver a iniciar sesión y revisar que todos los servicios tengan la misma clave JWT. |

Si Maven guardó una descarga fallida de `oss-parent:58`, detener Maven, borrar solo sus marcadores de error en la caché utilizada y reintentar desde un microservicio:

```powershell
$folder = "$env:LOCALAPPDATA\MaisonLuxury\maven-repository\com\fasterxml\oss-parent\58"
Get-ChildItem -LiteralPath $folder -Filter '*.lastUpdated' -ErrorAction SilentlyContinue | Remove-Item
mvn.cmd --% -Dmaven.repo.local=%LOCALAPPDATA%\MaisonLuxury\maven-repository -U clean install
```

Si se ejecutó Maven sin la propiedad de caché, la ruta equivalente es `$env:USERPROFILE\.m2\repository\com\fasterxml\oss-parent\58`. Asignar `$folder` por sí solo no borra ni repara archivos. Borrar marcadores tampoco elimina una limitación 429 que siga activa en el servidor.

## Estructura y cierre

- `backend/ms-catalogo`, `backend/ms-usuarios`, `backend/ms-ordenes`: servicios de negocio.
- `backend/gateway`: entrada HTTP para el frontend.
- `backend/shared`: código de seguridad compartido.
- `backend/*/src/main/resources/db`: migraciones de base de datos.
- `iniciar-backend.ps1`: inicio de los cuatro procesos en Windows.
- `.env.example`: plantilla sin contraseñas.

Flyway aplica las migraciones al iniciar. No ejecutar scripts antiguos de creación o limpieza sobre la base compartida para intentar resolver un error de arranque.

Para cerrar, presionar `Ctrl+C` en las cuatro ventanas. Cerrar solamente la terminal que ejecutó el lanzador no detiene necesariamente sus procesos hijos. No incluir `.env`, `target/`, cachés de Maven ni bases locales al subir cambios al repositorio.

## Verificación de esta entrega

El 18 de noviembre de 2026 se ejecutaron 94 pruebas backend con JDK 21 y Maven 3.9.9: 21 en catálogo, 32 en usuarios, 38 en órdenes y 3 en gateway. Todas terminaron sin fallos ni errores. De ellas, 25 son pruebas unitarias de servicios o seguridad y 69 comprueban integración de API y CORS. Los reportes se adjuntan en el paquete de entrega.

Las pruebas unitarias usan Mockito para sustituir repositorios y servicios externos. La configuración `mock-maker-subclass` evita necesitar la carga dinámica de un agente en el JDK; no se simulan clases finales. Las pruebas de API utilizan H2 aislada, mientras el perfil `firebase` mantiene PostgreSQL para el uso real. Esta ejecución no acredita acceso actual a la nube ni entrega real de SMTP desde otro equipo.
