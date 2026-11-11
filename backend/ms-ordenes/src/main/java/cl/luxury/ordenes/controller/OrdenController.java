package cl.luxury.ordenes.controller;

import cl.luxury.security.Seguridad;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.ordenes.dto.OrdenRequest;
import cl.luxury.ordenes.model.Orden;
import cl.luxury.ordenes.service.OrdenService;
import cl.luxury.ordenes.service.BoletaPdfService;
import cl.luxury.ordenes.service.BoletaCorreoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "Orden", description = "API de órdenes: órdenes con cliente, dirección, artículos, importes y estado. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/v1/ordenes")
public class OrdenController {

    private final Seguridad tokens;

    private Orden consultar(Long id) {
        Orden orden = servicio.obtener(id);
        Seguridad.exigir(Seguridad.rol("Administrador") || Seguridad.rol("Vendedor")
            || (Seguridad.rol("Cliente") && Seguridad.sujeto().equals(orden.getUsuarioId()))
            || (Seguridad.rol("Invitado") && Seguridad.sujeto().equals("orden-" + id)));
        return orden;
    }

    private final OrdenService servicio;
    private final BoletaPdfService boletaPdfService;
    private final BoletaCorreoService boletaCorreoService;

    public OrdenController(OrdenService servicio, BoletaPdfService boletaPdfService, BoletaCorreoService boletaCorreoService, Seguridad tokens) {
        this.tokens = tokens;
        this.servicio = servicio;
        this.boletaPdfService = boletaPdfService;
        this.boletaCorreoService = boletaCorreoService;
    }

    /** Descarga del comprobante de una orden pagada. */
    @Operation(summary = "Descargar boleta de una compra confirmada", description = "Devuelve application/pdf para órdenes Pagado, En preparación, Enviado o Entregado. Órdenes no confirmadas producen 409.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "PDF de la boleta"), @ApiResponse(responseCode = "404", description = "Orden inexistente"), @ApiResponse(responseCode = "409", description = "La compra todavía no está confirmada")})
    @GetMapping(value = "/{id}/boleta", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarBoleta(@PathVariable Long id) {
        Orden orden = consultar(id);
        if (!java.util.Set.of("Pagado", "En preparación", "Enviado", "Entregado").contains(orden.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo hay boletas para compras confirmadas.");
        }
        try {
            byte[] pdf = boletaPdfService.generar(orden);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=boleta-luxury-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .body(pdf);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar la boleta.", ex);
        }
    }

    @Operation(summary = "Consultar estado de envío del comprobante por correo")
    @GetMapping("/{id}/boleta/correo")
    public Map<String, Object> estadoCorreo(@PathVariable Long id) { consultar(id); return boletaCorreoService.consultar(id); }

    @Operation(summary = "Solicitar o reintentar el envío del comprobante al correo de la orden")
    @PostMapping("/{id}/boleta/correo")
    public ResponseEntity<Map<String, Object>> enviarCorreo(@PathVariable Long id) {
        consultar(id);
        return ResponseEntity.accepted().body(boletaCorreoService.solicitar(id));
    }

    /** GET /api/v1/ordenes?usuarioId=20987654K&_sort=fecha&_order=desc  (historial de compras / listado admin) */
    @Operation(summary = "Listar órdenes", description = "Devuelve la lista de órdenes con cliente, dirección, artículos, importes y estado. Para filtros disponibles: usuarioId, estado, _sort y _order. Si no hay coincidencias devuelve una lista vacía.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista obtenida (puede estar vacía)")})
    @GetMapping
    public List<Orden> listar(@RequestParam Map<String, String> filtros) {
        if (Seguridad.rol("Cliente")) filtros.put("usuarioId", Seguridad.sujeto());
        return servicio.listar(filtros);
    }

    /** Comprobación rápida: GET http://localhost:8083/api/v1/ordenes/salud (la ruta literal gana a /{id}). */
    @Operation(summary = "Consultar salud del servicio", description = "Devuelve un JSON identificando el microservicio y su estado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente")})
    @GetMapping("/salud")
    public Map<String, String> salud() {
        return Map.of("servicio", "ms-ordenes", "estado", "OK");
    }

    @Operation(summary = "Consultar orden por identificador", description = "Obtiene un recurso existente; devuelve 404 cuando no se encuentra.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @GetMapping("/{id}")
    public Orden obtener(@PathVariable Long id) {
        return consultar(id);
    }

    @Operation(summary = "Crear orden", description = "Recibe un JSON con los campos del recurso, valida sus datos y devuelve el registro creado junto a su ubicación.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Recurso creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PostMapping
    public ResponseEntity<Orden> crear(@Valid @RequestBody OrdenRequest solicitud) {
        Seguridad.exigir(Seguridad.rol("Administrador") || (solicitud.usuarioId() == null && !Seguridad.rol("Vendedor"))
            || (Seguridad.rol("Cliente") && Seguridad.sujeto().equals(solicitud.usuarioId())));
        Orden creada = servicio.crear(solicitud);
        var respuesta = ResponseEntity.created(URI.create("/api/v1/ordenes/" + creada.getId()));
        if (creada.getUsuarioId() == null) respuesta.header("X-Boleta-Token", tokens.emitir("orden-" + creada.getId(), "Invitado"));
        return respuesta.body(creada);
    }

    @Operation(summary = "Reemplazar orden mediante PUT", description = "Reemplaza los datos editables con el JSON completo. No sirve para editar solo un campo; se aplican las validaciones y reglas de negocio. Solo se permite reemplazar una orden editable; no se pueden reemplazar productos de una compra pagada ni saltarse las transiciones de estado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PutMapping("/{id}")
    public Orden actualizar(@PathVariable Long id, @Valid @RequestBody OrdenRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @Operation(summary = "Actualizar parcialmente orden mediante PATCH", description = "Recibe un objeto JSON con los campos a cambiar. No envíes el recurso completo; campos no enviados se conservan y los no permitidos pueden generar 400. Campos permitidos en PATCH: estado (respetando las transiciones Pendiente, Pago fallido, Pagado, En preparación, Enviado, Entregado); PUT no puede reemplazar una compra pagada o despachada.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @PatchMapping("/{id}")
    public Orden modificar(@PathVariable Long id, @RequestBody JsonNode cambios) {
        return servicio.modificar(id, cambios);
    }

    @Operation(summary = "Eliminar orden", description = "Elimina el registro si existe y si las reglas de negocio lo permiten. Devuelve 204 sin contenido al eliminar. No se permite eliminar una compra pagada o despachada: devuelve 400.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Orden eliminada"), @ApiResponse(responseCode = "400", description = "Orden pagada o despachada, no se permite eliminar"), @ApiResponse(responseCode = "404", description = "Orden inexistente")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
