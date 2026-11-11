package cl.luxury.catalogo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.catalogo.model.Oferta;
import cl.luxury.catalogo.service.OfertaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Oferta", description = "API de ofertas: ofertas y descuentos asociados a productos. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/v1/ofertas")
public class OfertaController {

    private final OfertaService servicio;

    public OfertaController(OfertaService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/v1/ofertas?activa=true&_expand=producto  (la forma que ya usa ofertasService.listarActivas) */
    @Operation(summary = "Listar ofertas", description = "Devuelve la lista de ofertas y descuentos asociados a productos. Para filtros disponibles: consultar las condiciones definidas en el servicio. Si no hay coincidencias devuelve una lista vacía.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista obtenida (puede estar vacía)")})
    @GetMapping
    public Object listar(@RequestParam(required = false) Boolean activa,
                         @RequestParam(name = "_expand", required = false) String expand) {
        return "producto".equals(expand) ? servicio.listarConProducto(activa) : servicio.listar(activa);
    }

    @Operation(summary = "Consultar oferta por identificador", description = "Obtiene un recurso existente; devuelve 404 cuando no se encuentra.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @GetMapping("/{id}")
    public Oferta obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear oferta", description = "Recibe un JSON con los campos del recurso, valida sus datos y devuelve el registro creado junto a su ubicación.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Recurso creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PostMapping
    public ResponseEntity<Oferta> crear(@Valid @RequestBody Oferta oferta) {
        Oferta creada = servicio.crear(oferta);
        return ResponseEntity.created(URI.create("/api/v1/ofertas/" + creada.getId())).body(creada);
    }

    @Operation(summary = "Reemplazar oferta mediante PUT", description = "Reemplaza los datos editables con el JSON completo. No sirve para editar solo un campo; se aplican las validaciones y reglas de negocio.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PutMapping("/{id}")
    public Oferta actualizar(@PathVariable Long id, @Valid @RequestBody Oferta oferta) {
        return servicio.actualizar(id, oferta);
    }

    @Operation(summary = "Eliminar oferta", description = "Elimina el registro si existe y si las reglas de negocio lo permiten. Devuelve 204 sin contenido al eliminar.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Recurso eliminado"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
