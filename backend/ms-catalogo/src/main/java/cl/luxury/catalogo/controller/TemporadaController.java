package cl.luxury.catalogo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Temporada visual de la tienda. El ajuste manual se guarda junto a la BD local
 * del microservicio y sobrevive a reinicios. Sin ajuste, prevalece la fecha.
 */
@Tag(name = "Temporada visual", description = "Activación automática de Navidad y control manual desde administración.")
@RestController
@RequestMapping("/api/temporada")
public class TemporadaController {
    private static final Path ARCHIVO = Path.of("data", "modo-navidad.txt");
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    public record CambioModo(String modo) {}
    public record EstadoTemporada(String modo, boolean activo, boolean activoPorFecha,
                                  String descripcion, String inicio, String termino) {}

    private String leerModo() {
        if (!Files.exists(ARCHIVO)) return "automatico";
        try {
            String valor = Files.readString(ARCHIVO, StandardCharsets.UTF_8).trim();
            return esValido(valor) ? valor : "automatico";
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo leer la configuracion de temporada", ex);
        }
    }

    private boolean esValido(String modo) {
        return "automatico".equals(modo) || "navidad".equals(modo) || "normal".equals(modo);
    }

    private EstadoTemporada estado(String modo) {
        LocalDate fecha = LocalDate.now(ZONA);
        boolean enTemporada = fecha.getMonthValue() == 12 && fecha.getDayOfMonth() <= 26;
        boolean activo = "navidad".equals(modo) || ("automatico".equals(modo) && enTemporada);
        String descripcion = switch (modo) {
            case "navidad" -> "Navidad activada manualmente";
            case "normal" -> "Modo normal activado manualmente";
            default -> activo ? "Navidad activa por calendario" : "Modo normal fuera de temporada";
        };
        return new EstadoTemporada(modo, activo, enTemporada, descripcion, "01-12", "26-12");
    }

    @Operation(summary = "Consultar temporada visual", description = "GET público usado por todas las páginas para mostrar el mismo tema. " +
            "Automático: Navidad del 1 al 26 de diciembre (zona horaria de Chile).")
    @GetMapping
    public synchronized EstadoTemporada obtener() {
        return estado(leerModo());
    }

    @Operation(summary = "Cambiar temporada visual", description = "PUT con modo automatico, navidad o normal. " +
            "Diseñado para el panel de administración de este proyecto universitario.")
    @PutMapping
    public synchronized EstadoTemporada guardar(@RequestBody CambioModo cambio) {
        String modo = cambio == null || cambio.modo() == null ? "" : cambio.modo().trim().toLowerCase(Locale.ROOT);
        if (!esValido(modo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El modo debe ser automatico, navidad o normal");
        }
        try {
            Files.createDirectories(ARCHIVO.getParent());
            Path temporal = Files.createTempFile(ARCHIVO.getParent(), "temporada-", ".tmp");
            try {
                Files.writeString(temporal, modo + System.lineSeparator(), StandardCharsets.UTF_8);
                Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la configuracion de temporada", ex);
        }
        return estado(modo);
    }
}
