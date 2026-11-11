-- ============================================================
-- Luxury Perfumería · Microservicio 1: CATÁLOGO
-- Base de datos propia del servicio (una BD por microservicio).
-- MySQL 8+ · utf8mb4
-- Ejecutar:  mysql -u root -p < backend/database/01-schema-catalogo.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS luxury_catalogo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE luxury_catalogo;

DROP TABLE IF EXISTS ofertas;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS categorias;

-- Categoría = intensidad del perfume. El id es un slug (ligeros, moderados, intensos).
CREATE TABLE categorias (
  id          VARCHAR(50)  NOT NULL,
  nombre      VARCHAR(100) NOT NULL,
  descripcion VARCHAR(500) NOT NULL DEFAULT '',
  imagen      VARCHAR(500) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE productos (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  codigo          VARCHAR(30)   NOT NULL,
  ml              INT           NOT NULL DEFAULT 0,
  marca           VARCHAR(100)  NOT NULL,
  nombre          VARCHAR(150)  NOT NULL,
  precio          INT           NOT NULL,            -- CLP entero
  stock           INT           NOT NULL DEFAULT 0,
  stock_critico   INT           NOT NULL DEFAULT 2,
  categoria_id    VARCHAR(50)   NULL,
  familia         VARCHAR(50)   NULL,
  estacion        VARCHAR(50)   NULL,
  genero          VARCHAR(50)   NULL,
  tipo            VARCHAR(50)   NULL,
  concentracion   VARCHAR(50)   NULL,
  imagen          VARCHAR(500)  NULL,
  descripcion     TEXT          NULL,
  hero_fondo      VARCHAR(500)  NULL,
  historia_titulo VARCHAR(200)  NULL,
  historia        TEXT          NULL,
  historia_imagen VARCHAR(500)  NULL,
  cta_fondo       VARCHAR(500)  NULL,
  fondo_pagina    VARCHAR(500)  NULL,
  cta_titulo      VARCHAR(200)  NULL,
  cta_texto       VARCHAR(500)  NULL,
  nav_claro       BOOLEAN       NULL,
  hero_ajuste     VARCHAR(100)  NULL,
  -- Datos anidados (arreglos / objetos) guardados como JSON:
  humor           JSON NULL,   -- ["fresco","elegante"]
  maridaje        JSON NULL,   -- ["dia_a_dia","trabajo_reuniones"]
  notas           JSON NULL,   -- {"salida":[...],"corazon":[...],"fondo":[...]}
  notas_imagenes  JSON NULL,   -- {"salida":"/img/..","corazon":"/img/..","fondo":"/img/.."}
  inspirado_en    JSON NULL,   -- {"marca":"","nombre":"","precioOriginal":0}
  colores         JSON NULL,   -- {"tema-bg":"#...", ...}
  PRIMARY KEY (id),
  UNIQUE KEY uk_productos_codigo (codigo),
  KEY idx_productos_categoria (categoria_id),
  CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
  CONSTRAINT ck_productos_precio CHECK (precio >= 0),
  CONSTRAINT ck_productos_stock  CHECK (stock >= 0)
) ENGINE=InnoDB;

-- El precio con oferta NO se guarda: precio * (1 - descuento/100), calculado en el front.
CREATE TABLE ofertas (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  producto_id   BIGINT       NOT NULL,
  descuento     INT          NOT NULL,                -- porcentaje entero (15 = 15 %)
  etiqueta      VARCHAR(100) NULL,
  vigente_hasta DATE         NULL,
  activa        BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  KEY idx_ofertas_producto (producto_id),
  -- Borrar un producto borra sus ofertas (igual que el borrado en cascada del mock).
  CONSTRAINT fk_ofertas_producto FOREIGN KEY (producto_id) REFERENCES productos (id) ON DELETE CASCADE,
  CONSTRAINT ck_ofertas_descuento CHECK (descuento BETWEEN 1 AND 90)
) ENGINE=InnoDB;
