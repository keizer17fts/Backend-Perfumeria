package cl.luxury.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.usuarios.dto.LoginRequest;
import cl.luxury.usuarios.dto.GoogleLoginRequest;
import cl.luxury.usuarios.service.GoogleLoginService;
import cl.luxury.usuarios.model.Usuario;
import cl.luxury.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Auth", description = "API de autenticación: inicio de sesión y datos del usuario autenticado. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final cl.luxury.security.Seguridad tokens;

    private Usuario sesion(Usuario u) {
        u.setAccessToken(tokens.emitir(u.getRun(), u.getTipo()));
        return u;
    }

    private final UsuarioService servicio;
    private final GoogleLoginService google;

    public AuthController(UsuarioService servicio, GoogleLoginService google, cl.luxury.security.Seguridad tokens) {
        this.tokens = tokens;
        this.servicio = servicio;
        this.google = google;
    }

    /** POST /api/v1/auth/login {correo, password} -> usuario SIN contraseña, o 401 con {campo}. */
    @Operation(summary = "Autenticar usuario con credenciales", description = "Recibe las credenciales y devuelve los datos de autenticación si son válidas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @PostMapping("/login")
    public Usuario login(@Valid @RequestBody LoginRequest solicitud) {
        return sesion(servicio.login(solicitud.getCorreo(), solicitud.getPassword()));
    }

    @Operation(summary = "Autenticar mediante Google ID token", description = "Verifica el token de Google; devuelve el usuario o rechaza el token o una cuenta incompatible.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuario autenticado"),
            @ApiResponse(responseCode = "400", description = "Token inválido o cuenta existente")})
    @PostMapping("/google")
    public Usuario loginGoogle(@Valid @RequestBody GoogleLoginRequest solicitud) {
        return sesion(google.autenticar(solicitud.credential()));
    }

    /** Comprobación rápida: GET http://localhost:8082/api/v1/auth/salud */
    @Operation(summary = "Consultar salud del servicio", description = "Devuelve un JSON identificando el microservicio y su estado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente")})
    @GetMapping("/salud")
    public Map<String, String> salud() {
        return Map.of("servicio", "ms-usuarios", "estado", "OK");
    }
}
