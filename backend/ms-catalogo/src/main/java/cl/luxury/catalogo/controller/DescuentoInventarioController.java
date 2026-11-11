package cl.luxury.catalogo.controller;

import cl.luxury.catalogo.service.DescuentoInventarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Tag(name = "Inventario", description = "Descuento por orden y compensación de inventario entre microservicios")
@RestController
@RequestMapping("/api/inventario")
public class DescuentoInventarioController {
    private final DescuentoInventarioService servicio;
    public DescuentoInventarioController(DescuentoInventarioService servicio) { this.servicio = servicio; }

    @Operation(summary = "Descontar productos por orden", description = "Descuenta varias cantidades de forma atómica. Usa ordenId para impedir descontar dos veces la misma orden. Si falta stock responde 409.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Inventario descontado o descuento ya registrado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Stock insuficiente o conflicto de orden")})
    @PostMapping("/descontar")
    public ResponseEntity<Void> descontar(@Valid @RequestBody DescuentoInventarioService.Solicitud solicitud) {
        servicio.descontar(solicitud);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Compensar inventario de una orden no confirmada", description = "Devuelve al inventario las unidades descontadas por una orden cuya transacción local falló. Es idempotente: si no hay descuento registrado, responde 204.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Inventario compensado o sin descuento registrado"), @ApiResponse(responseCode = "500", description = "No se pudo completar la compensación")})
    @PostMapping("/revertir/{ordenId}")
    public ResponseEntity<Void> revertir(@PathVariable String ordenId) {
        servicio.revertir(ordenId);
        return ResponseEntity.noContent().build();
    }
}
