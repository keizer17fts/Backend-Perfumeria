package cl.luxury.usuarios.exception;

/** Login fallido. "campo" indica dónde mostrar el error en el front: "correo" o "password". */
public class CredencialesInvalidasException extends RuntimeException {

    private final String campo;

    public CredencialesInvalidasException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
