# Pulido del panel administrativo — 02/11/2026

## Alcance
Se realizaron mejoras en el manejo de estados del frontend, sin cambiar el backend, contratos REST, migraciones, tests, datos ni los estilos globales. Navidad queda fuera del alcance.

## Cambios
- **CuponesAdmin.jsx**: muestra carga inicial, diferencia errores de conexión de una lista realmente vacía, permite reintentar, evita envíos simultáneos y cambios de estado duplicados, conserva los mensajes de operaciones, normaliza código y fechas al enviar, y limita visualmente la fecha de término cuando hay inicio.
- **ProductosAdmin.jsx**: evita eliminación duplicada mientras la API responde y muestra indicador de progreso en la fila correspondiente.
- **CategoriasAdmin.jsx**: evita eliminación duplicada y ofrece un botón de reintento de carga.
- **UsuariosAdmin.jsx**: permite reintentar la consulta sin recargar el sitio y no presenta errores de red como resultados vacíos.

## Verificación manual sugerida
1. Con los servicios ejecutándose, abrir /admin/cupones, crear un cupón, editarlo y alternar su estado.
2. Desconectar temporalmente Gateway y recargar /admin/usuarios y /admin/cupones; comprobar que los errores se muestran y el botón Reintentar funciona al volver a conectar.
3. Eliminar un producto o categoría que no esté en uso; verificar que la operación no se dispara dos veces aunque se haga doble clic.
4. Revisar las tablas en escritorio y móvil.

## Estado de compilación
No se pudo completar `npm ci --offline` en este entorno porque faltaba `yargs-parser` en la caché npm. No se ejecutó build ni prueba en navegador. Se requiere `npm install` y `npm run build` en un entorno con acceso a dependencias.
