package cl.luxury.usuarios.correo;

/** Evento para enviar correo solo después de confirmar la inserción. */
public record RegistroCreadoEvent(String correo, String nombre) {}
