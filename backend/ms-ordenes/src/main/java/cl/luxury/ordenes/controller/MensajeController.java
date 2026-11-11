package cl.luxury.ordenes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.ordenes.dto.MensajeRequest;
import cl.luxury.ordenes.model.Mensaje;
import cl.luxury.ordenes.service.MensajeService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "Mensaje", description = "API de mensajes: mensajes enviados desde contacto y su estado de lectura. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/v1/mensajes")
public class MensajeController {

    private final MensajeService servicio;

    public MensajeController(MensajeService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Listar mensajes", description = "Devuelve la lista de mensajes enviados desde contacto y su estado de lectura. Para filtros disponibles: según filtros admitidos por el recurso. Si no hay coincidencias devuelve una lista vacía.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista obtenida (puede estar vacía)")})
    @GetMapping
    public List<Mensaje> listar(@RequestParam Map<String, String> filtros) {
        return servicio.listar(filtros);
    }

    @Operation(summary = "Consultar mensaje por identificador", description = "Obtiene un recurso existente; devuelve 404 cuando no se encuentra.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @GetMapping("/{id}")
    public Mensaje obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear mensaje", description = "Recibe un JSON con los campos del recurso, valida sus datos y devuelve el registro creado junto a su ubicación.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Recurso creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PostMapping
    public ResponseEntity<Mensaje> crear(@Valid @RequestBody MensajeRequest solicitud) {
        Mensaje creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/mensajes/" + creado.getId())).body(creado);
    }

    @Operation(summary = "Actualizar parcialmente mensaje mediante PATCH", description = "Recibe un objeto JSON con los campos a cambiar. No envíes el recurso completo; campos no enviados se conservan y los no permitidos pueden generar 400.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @PatchMapping("/{id}")
    public Mensaje modificar(@PathVariable Long id, @RequestBody JsonNode cambios) {
        return servicio.modificar(id, cambios);
    }

    @Operation(summary = "Eliminar mensaje", description = "Elimina el registro si existe y si las reglas de negocio lo permiten. Devuelve 204 sin contenido al eliminar.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Recurso eliminado"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
