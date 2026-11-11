package cl.luxury.catalogo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.catalogo.exception.*;
import cl.luxury.catalogo.model.Resena;
import cl.luxury.catalogo.repository.ResenaRepository;
import cl.luxury.catalogo.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.*;

@Tag(name = "Resena", description = "API de reseñas: reseñas de clientes por producto. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController @RequestMapping("/api/resenas")
public class ResenaController {
  private final ResenaRepository resenas;
  private final ProductoRepository productos;
  private final RestClient ordenes;
  public ResenaController(ResenaRepository resenas, ProductoRepository productos,
                          @Value("${luxury.ordenes.url:http://localhost:8083}") String ordenesUrl) {
    this.resenas = resenas;
    this.productos = productos;
    this.ordenes = RestClient.builder().baseUrl(ordenesUrl).build();
  }
  public record NuevaResena(Long productoId, String usuarioId, String nombre, Integer estrellas, String texto) {}

  @Operation(summary = "Listar reseñas", description = "Devuelve la lista de reseñas de clientes por producto. Para filtros disponibles: consultar parámetros del recurso. Si no hay coincidencias devuelve una lista vacía.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista obtenida (puede estar vacía)")})
    @GetMapping
  public List<Resena> listar(@RequestParam Long productoId) {
    return resenas.findByProductoIdOrderByFechaDesc(productoId);
  }

  @Operation(summary = "Crear reseña", description = "Recibe un JSON con los campos del recurso, valida sus datos y devuelve el registro creado junto a su ubicación.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Recurso creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PostMapping
  public ResponseEntity<Resena> crear(@RequestBody NuevaResena dato) {
    if (dato == null || dato.productoId() == null || dato.usuarioId() == null || dato.usuarioId().isBlank()
        || dato.nombre() == null || dato.nombre().isBlank() || dato.estrellas() == null
        || dato.estrellas() < 1 || dato.estrellas() > 5 || dato.texto() == null
        || dato.texto().isBlank() || dato.texto().trim().length() > 1000) {
      throw new SolicitudInvalidaException("Completa nombre, comentario (máximo 1000 caracteres) y calificación de 1 a 5.");
    }
    if (!productos.existsById(dato.productoId())) throw new NoEncontradoException("Producto no encontrado.");
    if (resenas.existsByProductoIdAndUsuarioId(dato.productoId(), dato.usuarioId()))
      throw new ConflictoException("Ya has publicado una reseña para este perfume.");
    if (!haComprado(dato.usuarioId(), dato.productoId()))
      throw new ConflictoException("Debes haber comprado este perfume para dejar una reseña.");
    Resena nueva = new Resena();
    nueva.setProductoId(dato.productoId()); nueva.setUsuarioId(dato.usuarioId());
    nueva.setNombre(dato.nombre().trim()); nueva.setEstrellas(dato.estrellas());
    nueva.setTexto(dato.texto().trim());
    try {
      Resena guardada = resenas.save(nueva);
      return ResponseEntity.created(URI.create("/api/resenas/" + guardada.getId())).body(guardada);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new ConflictoException("Ya has publicado una reseña para este perfume.");
    }
  }

  private boolean haComprado(String usuarioId, Long productoId) {
    try {
      JsonNode respuesta = ordenes.get().uri(uri -> uri.path("/api/ordenes")
          .queryParam("usuarioId", usuarioId).build()).retrieve().body(JsonNode.class);
      if (respuesta == null || !respuesta.isArray()) return false;
      for (JsonNode orden : respuesta) {
        if (!Set.of("Pagado", "En preparación", "Enviado", "Entregado")
            .contains(orden.path("estado").asText())) continue;
        for (JsonNode item : orden.path("items")) {
          if (String.valueOf(productoId).equals(item.path("productoId").asText())) return true;
        }
      }
      return false;
    } catch (RestClientException e) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "No se pudieron verificar las compras.", e);
    }
  }

  // Demo académica: igual que el CRUD de productos, sin sesión JWT en la API.
  @Operation(summary = "Eliminar reseña", description = "Elimina el registro si existe y si las reglas de negocio lo permiten. Devuelve 204 sin contenido al eliminar.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Recurso eliminado"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    if (!resenas.existsById(id)) throw new NoEncontradoException("Reseña no encontrada.");
    resenas.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
