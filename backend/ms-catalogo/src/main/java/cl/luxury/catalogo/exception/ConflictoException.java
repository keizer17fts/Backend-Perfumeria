package cl.luxury.catalogo.exception;

/** Regla de negocio violada que el cliente puede corregir (código duplicado, categoría en uso...). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
