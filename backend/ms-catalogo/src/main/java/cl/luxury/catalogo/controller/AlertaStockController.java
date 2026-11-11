package cl.luxury.catalogo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.catalogo.service.AlertaStockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Tag(name = "AlertaStock", description = "API de alertas de stock: solicitudes de notificación de reposición. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController @RequestMapping("/api/alertas-stock")
public class AlertaStockController {
    private final AlertaStockService alertas;
    public AlertaStockController(AlertaStockService alertas) { this.alertas = alertas; }
    @Operation(summary = "Registrar solicitud de alerta por reposición", description = "Recibe productoId en la URL y la solicitud de aviso para un perfume sin existencias.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @PostMapping("/{productoId}")
    public ResponseEntity<Map<String, String>> solicitar(@PathVariable Long productoId,
            @RequestBody Map<String, String> datos) {
        try { return ResponseEntity.ok(alertas.solicitar(productoId, datos.get("correo"))); }
        catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", ex.getMessage()));
        }
    }
}
