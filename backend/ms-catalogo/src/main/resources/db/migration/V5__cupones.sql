CREATE TABLE cupones (
    codigo VARCHAR(40) NOT NULL PRIMARY KEY,
    descuento INT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    vigente_desde DATE NULL,
    vigente_hasta DATE NULL,
    CONSTRAINT ck_cupon_descuento CHECK (descuento BETWEEN 1 AND 90)
);
INSERT INTO cupones (codigo, descuento, activo) VALUES ('LUXURY10',10,TRUE),('BIENVENIDO15',15,TRUE);
