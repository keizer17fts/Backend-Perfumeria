package cl.luxury.catalogo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.catalogo.model.Producto;
import cl.luxury.catalogo.service.ProductoService;
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

@Tag(name = "Producto", description = "API de productos: productos del catálogo, con precio, stock, categoría y datos de presentación. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/v1/productos?categoria=intensos&genero=hombre&q=sauvage */
    @Operation(summary = "Listar productos", description = "Devuelve la lista de productos del catálogo, con precio, stock, categoría y datos de presentación. Para filtros disponibles: categoria, genero, q y otros filtros admitidos por el servicio. Si no hay coincidencias devuelve una lista vacía.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista obtenida (puede estar vacía)")})
    @GetMapping
    public List<Producto> listar(@RequestParam Map<String, String> filtros) {
        return servicio.listar(filtros);
    }

    @Operation(summary = "Consultar producto por identificador", description = "Obtiene un recurso existente; devuelve 404 cuando no se encuentra.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear producto", description = "Recibe un JSON con los campos del recurso, valida sus datos y devuelve el registro creado junto a su ubicación.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Recurso creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody Producto producto) {
        Producto creado = servicio.crear(producto);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.getId())).body(creado);
    }

    @Operation(summary = "Reemplazar producto mediante PUT", description = "Reemplaza los datos editables con el JSON completo. No sirve para editar solo un campo; se aplican las validaciones y reglas de negocio.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        return servicio.actualizar(id, producto);
    }

    @Operation(summary = "Actualizar parcialmente producto mediante PATCH", description = "Recibe un objeto JSON con los campos a cambiar. No envíes el recurso completo; campos no enviados se conservan y los no permitidos pueden generar 400. Ejemplos de campos editables: precio, stock, nombre y descripción (sujetos a las reglas del servicio).")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @PatchMapping("/{id}")
    public Producto modificar(@PathVariable Long id, @RequestBody JsonNode cambios) {
        return servicio.modificar(id, cambios);
    }

    @Operation(summary = "Eliminar producto", description = "Elimina el registro si existe y si las reglas de negocio lo permiten. Devuelve 204 sin contenido al eliminar.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Recurso eliminado"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
