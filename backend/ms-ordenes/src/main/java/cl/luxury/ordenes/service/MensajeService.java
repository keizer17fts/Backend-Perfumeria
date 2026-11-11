package cl.luxury.ordenes.service;

import cl.luxury.ordenes.dto.MensajeRequest;
import cl.luxury.ordenes.exception.NoEncontradoException;
import cl.luxury.ordenes.exception.SolicitudInvalidaException;
import cl.luxury.ordenes.model.Mensaje;
import cl.luxury.ordenes.repository.MensajeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class MensajeService {

    private final MensajeRepository mensajes;

    public MensajeService(MensajeRepository mensajes) {
        this.mensajes = mensajes;
    }

    /** Más recientes primero por defecto; ?_order=asc invierte. */
    @Transactional(readOnly = true)
    public List<Mensaje> listar(Map<String, String> filtros) {
        Sort.Direction direccion = "asc".equalsIgnoreCase(filtros.getOrDefault("_order", "desc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return mensajes.findAll(Sort.by(direccion, "fecha").and(Sort.by(direccion, "id")));
    }

    @Transactional(readOnly = true)
    public Mensaje obtener(Long id) {
        return mensajes.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el mensaje " + id));
    }

    @Transactional
    public Mensaje crear(MensajeRequest r) {
        Mensaje m = new Mensaje();
        m.setNombre(r.nombre().trim());
        m.setCorreo(r.correo().trim().toLowerCase());
        m.setAsunto(r.asunto().trim());
        m.setMensaje(r.mensaje().trim());
        m.setFecha(r.fecha() != null ? r.fecha() : Instant.now());
        m.setEstado(r.estado() == null || r.estado().isBlank() ? "Nuevo" : r.estado().trim());
        return mensajes.save(m);
    }

    /** PATCH: solo el estado (Nuevo, Leído, Respondido...). */
    @Transactional
    public Mensaje modificar(Long id, JsonNode cambios) {
        Mensaje m = obtener(id);
        if (cambios == null || !cambios.hasNonNull("estado") || cambios.get("estado").asText().isBlank()
                || cambios.get("estado").asText().length() > 20) {
            throw new SolicitudInvalidaException("El cuerpo debe incluir 'estado' (máximo 20 caracteres).");
        }
        m.setEstado(cambios.get("estado").asText().trim());
        return mensajes.save(m);
    }

    @Transactional
    public void eliminar(Long id) {
        mensajes.delete(obtener(id));
    }
}
