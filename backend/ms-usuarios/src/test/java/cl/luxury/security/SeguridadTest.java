package cl.luxury.security;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import static org.junit.jupiter.api.Assertions.*;

class SeguridadTest {
    String clave(String texto) { return Base64.getEncoder().encodeToString(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }

    @Test void tokenConservaSujetoRolEmisorYCaducidad() {
        Seguridad seguridad = new Seguridad(clave("clave-de-pruebas-de-32-bytes-o-mas"));
        var jwt = seguridad.decoder().decode(seguridad.emitir("190110222", "Cliente"));
        assertEquals("190110222", jwt.getSubject());
        assertEquals("Cliente", jwt.getClaimAsString("rol"));
        assertEquals("maison", jwt.getClaimAsString("iss"));
        assertEquals(28800, java.time.Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds());
    }
    @Test void rechazaTokenFirmadoConOtraClave() {
        Seguridad emisor = new Seguridad(clave("primera-clave-de-pruebas-32-bytes"));
        Seguridad receptor = new Seguridad(clave("segunda-clave-de-pruebas-32-bytes"));
        assertThrows(JwtException.class, () -> receptor.decoder().decode(emisor.emitir("190110222", "Administrador")));
    }
    @Test void rechazaClaveDemasiadoCorta() {
        assertThrows(IllegalArgumentException.class, () -> new Seguridad(clave("corta")));
    }
}
