-- ============================================================
-- Luxury Perfumería · Microservicio 3: ÓRDENES y MENSAJES
-- Base de datos propia del servicio. MySQL 8+ · utf8mb4
-- Ejecutar:  mysql -u root -p < backend/database/04-schema-ordenes.sql
-- (opcional) datos demo:  mysql -u root -p < backend/database/05-seed-ordenes.sql
-- Las fechas se guardan en UTC (el servicio fija hibernate.jdbc.time_zone=UTC).
-- usuario_id y orden_items.producto_id son referencias LÓGICAS a otras bases (no hay FK entre BD).
-- ============================================================
CREATE DATABASE IF NOT EXISTS luxury_ordenes
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE luxury_ordenes;

DROP TABLE IF EXISTS orden_items;
DROP TABLE IF EXISTS ordenes;
DROP TABLE IF EXISTS mensajes;

CREATE TABLE ordenes (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,      -- nº de boleta = id con ceros a la izquierda (lo hace el front)
  fecha               DATETIME(3)  NOT NULL,
  estado              VARCHAR(20)  NOT NULL,
  usuario_id          VARCHAR(9)   NULL,                         -- RUN del cliente; NULL = compra como invitado
  cliente_nombre      VARCHAR(50)  NOT NULL,
  cliente_apellidos   VARCHAR(100) NOT NULL,
  cliente_correo      VARCHAR(100) NOT NULL,
  envio_calle         VARCHAR(200) NOT NULL,
  envio_departamento  VARCHAR(50)  NULL,
  envio_region        VARCHAR(100) NOT NULL,
  envio_comuna        VARCHAR(100) NOT NULL,
  envio_indicaciones  VARCHAR(500) NULL,
  subtotal            INT          NOT NULL,
  cupon               VARCHAR(30)  NULL,
  descuento           INT          NOT NULL DEFAULT 0,
  total               INT          NOT NULL,
  pago_metodo         VARCHAR(30)  NOT NULL,
  pago_tarjeta_final  CHAR(4)      NOT NULL,                     -- nunca el número completo
  PRIMARY KEY (id),
  KEY ix_ordenes_usuario (usuario_id),
  KEY ix_ordenes_fecha (fecha),
  CONSTRAINT ck_ordenes_estado CHECK (estado IN ('Pendiente', 'Pagado', 'Pago fallido', 'En preparación', 'Enviado', 'Entregado'))
) ENGINE=InnoDB;

CREATE TABLE orden_items (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  orden_id     BIGINT       NOT NULL,
  producto_id  VARCHAR(20)  NOT NULL,                            -- referencia lógica a luxury_catalogo.productos
  codigo       VARCHAR(50)  NULL,
  nombre       VARCHAR(150) NOT NULL,                            -- copia del producto al momento de comprar
  marca        VARCHAR(100) NULL,
  imagen       VARCHAR(300) NULL,
  precio       INT          NOT NULL,
  cantidad     INT          NOT NULL,
  subtotal     INT          NOT NULL,
  PRIMARY KEY (id),
  KEY ix_items_orden (orden_id),
  CONSTRAINT fk_items_orden FOREIGN KEY (orden_id) REFERENCES ordenes (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE mensajes (
  id       BIGINT        NOT NULL AUTO_INCREMENT,
  nombre   VARCHAR(100)  NOT NULL,
  correo   VARCHAR(100)  NOT NULL,
  asunto   VARCHAR(150)  NOT NULL,
  mensaje  VARCHAR(2000) NOT NULL,
  fecha    DATETIME(3)   NOT NULL,
  estado   VARCHAR(20)   NOT NULL DEFAULT 'Nuevo',
  PRIMARY KEY (id)
) ENGINE=InnoDB;
