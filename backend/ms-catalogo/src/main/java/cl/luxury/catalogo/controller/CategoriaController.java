package cl.luxury.catalogo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.catalogo.model.Categoria;
import cl.luxury.catalogo.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Categoria", description = "API de categorías: categorías utilizadas para clasificar perfumes. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Listar categorías", description = "Devuelve la lista de categorías utilizadas para clasificar perfumes. Para filtros disponibles: sin filtros específicos. Si no hay coincidencias devuelve una lista vacía.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista obtenida (puede estar vacía)")})
    @GetMapping
    public List<Categoria> listar() {
        return servicio.listar();
    }

    @Operation(summary = "Consultar categoría por identificador", description = "Obtiene un recurso existente; devuelve 404 cuando no se encuentra.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @GetMapping("/{id}")
    public Categoria obtener(@PathVariable String id) {
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear categoría", description = "Recibe un JSON con los campos del recurso, valida sus datos y devuelve el registro creado junto a su ubicación.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Recurso creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PostMapping
    public ResponseEntity<Categoria> crear(@Valid @RequestBody Categoria categoria) {
        Categoria creada = servicio.crear(categoria);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + creada.getId())).body(creada);
    }

    @Operation(summary = "Reemplazar categoría mediante PUT", description = "Reemplaza los datos editables con el JSON completo. No sirve para editar solo un campo; se aplican las validaciones y reglas de negocio.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PutMapping("/{id}")
    public Categoria actualizar(@PathVariable String id, @Valid @RequestBody Categoria categoria) {
        return servicio.actualizar(id, categoria);
    }

    @Operation(summary = "Eliminar categoría", description = "Elimina el registro si existe y si las reglas de negocio lo permiten. Devuelve 204 sin contenido al eliminar.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Recurso eliminado"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
