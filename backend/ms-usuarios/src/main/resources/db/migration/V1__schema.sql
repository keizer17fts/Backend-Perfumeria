-- Migración automática Flyway. No contiene DROP/DELETE y se ejecuta una sola vez.
-- ============================================================
-- Luxury Perfumería · Microservicio 2: USUARIOS
-- Base de datos propia del servicio. MySQL 8+ · utf8mb4
-- Los 3 usuarios demo los crea el servicio al primer arranque (con la contraseña cifrada con BCrypt).
-- ============================================================



CREATE TABLE usuarios (
  run              VARCHAR(9)   NOT NULL,            -- RUN sin puntos ni guion (ej. 20987654K). Es el id.
  nombre           VARCHAR(50)  NOT NULL,
  apellidos        VARCHAR(100) NOT NULL,
  correo           VARCHAR(100) NOT NULL,            -- siempre en minúsculas
  password_hash    VARCHAR(100) NOT NULL,            -- BCrypt ($2a$...). Nunca texto plano.
  fecha_nacimiento DATE         NULL,
  tipo             VARCHAR(20)  NOT NULL DEFAULT 'Cliente',
  region           VARCHAR(100) NULL,
  comuna           VARCHAR(100) NULL,
  direccion        VARCHAR(300) NULL,
  PRIMARY KEY (run),
  CONSTRAINT uk_usuarios_correo UNIQUE (correo),
  CONSTRAINT ck_usuarios_tipo CHECK (tipo IN ('Administrador', 'Vendedor', 'Cliente'))
);
