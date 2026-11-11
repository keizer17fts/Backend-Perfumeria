CREATE TABLE boleta_envios (
    orden_id BIGINT PRIMARY KEY,
    estado VARCHAR(20) NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    actualizado TIMESTAMP WITH TIME ZONE,
    proximo_intento TIMESTAMP WITH TIME ZONE,
    mensaje VARCHAR(300)
);
CREATE INDEX idx_boleta_envios_pendientes ON boleta_envios(estado, proximo_intento);
