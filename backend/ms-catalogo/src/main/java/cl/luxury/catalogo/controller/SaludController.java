package cl.luxury.catalogo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Para comprobar rápido que el servicio está arriba: GET http://localhost:8081/api/catalogo/salud */
@Tag(name = "Salud", description = "API de salud: estado del microservicio. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/catalogo")
public class SaludController {

    @Operation(summary = "Consultar salud del servicio", description = "Devuelve un JSON identificando el microservicio y su estado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente")})
    @GetMapping("/salud")
    public Map<String, String> salud() {
        return Map.of("servicio", "ms-catalogo", "estado", "OK");
    }
}
