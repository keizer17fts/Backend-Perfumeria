-- Vinculación de cuenta Google, sin alterar los registros existentes.
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(255) NULL;
CREATE UNIQUE INDEX uk_usuarios_google_sub ON usuarios (google_sub);
