# Backend · Luxury Perfumería

Arquitectura: **React → gateway :8080 → 3 microservicios Spring Boot → MySQL** (una base de datos por servicio).

| Servicio | Puerto | Base de datos | Responsabilidad | Etapa |
|---|---|---|---|---|
| `ms-catalogo` | 8081 | `luxury_catalogo` | productos, categorías, ofertas | **15 (listo)** |
| `ms-usuarios` | 8082 | `luxury_usuarios` | usuarios, login (BCrypt) | **16 (listo)** |
| `ms-ordenes` | 8083 | `luxury_ordenes` | órdenes/boletas, mensajes de contacto | **17 (listo)** |
| `gateway` | 8080 | — | punto único para el front + CORS | **17 (listo)** |

## Requisitos
- JDK 21, Maven 3.9+ (o IntelliJ / VS Code con Extension Pack for Java)
- MySQL 8 (XAMPP, MySQL Workbench o Docker)

## Ejecución recomendada

Usa el lanzador `iniciar-perfumeria.ps1` y las instrucciones del [README principal](../README.md).
Las migraciones Flyway se ejecutan automáticamente con bases MySQL previamente creadas; no ejecutes los scripts antiguos con DROP/DELETE sobre datos reales.

## Pruebas
```bash
cd backend/ms-catalogo && mvn test      # usa H2 en memoria, no necesita MySQL
cd backend/ms-usuarios && mvn test
cd backend/ms-ordenes && mvn test      # 24 pruebas (MockMvc + H2)
cd backend/gateway && mvn test         # pruebas de CORS (no necesita los microservicios)
```

## Regenerar el SQL de datos
Los datos salen de los mismos `seed/*.js` que usa json-server:
```bash
node backend/database/generar-seed-catalogo.mjs
```

## Capas (patrón de los 3 servicios)
`controller` (HTTP) → `service` (reglas de negocio) → `repository` (Spring Data JPA) → `model` (entidades).
Errores unificados en `exception/ManejadorErrores` con cuerpo `{status, error, mensaje}`.
