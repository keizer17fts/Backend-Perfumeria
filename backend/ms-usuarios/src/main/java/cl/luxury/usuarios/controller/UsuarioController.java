package cl.luxury.usuarios.controller;

import cl.luxury.security.Seguridad;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import cl.luxury.usuarios.dto.UsuarioRequest;
import cl.luxury.usuarios.dto.CambioPasswordRequest;
import cl.luxury.usuarios.model.Usuario;
import cl.luxury.usuarios.service.UsuarioService;
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

@Tag(name = "Usuario", description = "API de usuarios: cuentas y datos de perfil. Consultar cada operación para conocer sus validaciones y respuestas.")
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/v1/usuarios?correo=x@gmail.com  |  ?run=190110222  |  ?tipo=Cliente */
    @Operation(summary = "Listar usuarios", description = "Devuelve la lista de cuentas y datos de perfil. Para filtros disponibles: según filtros admitidos por el servicio. Si no hay coincidencias devuelve una lista vacía.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista obtenida (puede estar vacía)")})
    @GetMapping
    public List<Usuario> listar(@RequestParam Map<String, String> filtros) {
        return servicio.listar(filtros);
    }

    @Operation(summary = "Consultar usuario por identificador", description = "Obtiene un recurso existente; devuelve 404 cuando no se encuentra.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @GetMapping("/{id}")
    public Usuario obtener(@PathVariable String id) {
        Seguridad.exigir(Seguridad.rol("Administrador") || id.equals(Seguridad.sujeto()));
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear usuario", description = "Recibe un JSON con los campos del recurso, valida sus datos y devuelve el registro creado junto a su ubicación.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Recurso creado"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody UsuarioRequest solicitud) {
        if (!Seguridad.rol("Administrador")) solicitud.setTipo("Cliente");
        Usuario creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.getRun())).body(creado);
    }

    @Operation(summary = "Reemplazar usuario mediante PUT", description = "Reemplaza los datos editables con el JSON completo. No sirve para editar solo un campo; se aplican las validaciones y reglas de negocio.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado"), @ApiResponse(responseCode = "409", description = "Conflicto con el estado del recurso")})
    @PutMapping("/{id}")
    public Usuario actualizar(@PathVariable String id, @Valid @RequestBody UsuarioRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @Operation(summary = "Actualizar parcialmente usuario mediante PATCH", description = "Recibe un objeto JSON con los campos a cambiar. No envíes el recurso completo; campos no enviados se conservan y los no permitidos pueden generar 400.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Operación realizada correctamente"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @PatchMapping("/{id}")
    public Usuario modificar(@PathVariable String id, @RequestBody JsonNode cambios) {
        if (!Seguridad.rol("Administrador")) {
            Seguridad.exigir(id.equals(Seguridad.sujeto()));
            var campos = cambios.fieldNames();
            var permitidos = java.util.Set.of("nombre","apellidos","correo","direccion","region","comuna","fechaNacimiento");
            while (campos.hasNext()) Seguridad.exigir(permitidos.contains(campos.next()));
        }
        return servicio.modificar(id, cambios);
    }

    @Operation(summary = "Cambiar contraseña del usuario", description = "Recibe los datos requeridos para cambiar la contraseña. Devuelve 204 al completar.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Contraseña actualizada"), @ApiResponse(responseCode = "400", description = "Solicitud inválida"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @PostMapping("/{id}/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable String id,
            @Valid @RequestBody CambioPasswordRequest solicitud) {
        Seguridad.exigir(Seguridad.rol("Administrador") || id.equals(Seguridad.sujeto()));
        servicio.cambiarPassword(id, solicitud);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar usuario", description = "Elimina el registro si existe y si las reglas de negocio lo permiten. Devuelve 204 sin contenido al eliminar.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Recurso eliminado"), @ApiResponse(responseCode = "404", description = "Recurso no encontrado")})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
