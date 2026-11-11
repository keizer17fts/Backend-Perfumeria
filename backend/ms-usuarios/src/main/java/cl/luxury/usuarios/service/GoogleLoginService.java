package cl.luxury.usuarios.service;

import cl.luxury.usuarios.exception.SolicitudInvalidaException;
import cl.luxury.usuarios.correo.RegistroCreadoEvent;
import cl.luxury.usuarios.model.Usuario;
import cl.luxury.usuarios.repository.UsuarioRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

@Service
public class GoogleLoginService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventos;
    private final GoogleIdTokenVerifier verifier;

    public GoogleLoginService(UsuarioRepository usuarios, PasswordEncoder encoder,
                              ApplicationEventPublisher eventos,
                              @Value("${luxury.google.client-id}") String clientId) throws GeneralSecurityException, IOException {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.eventos = eventos;
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Falta GOOGLE_CLIENT_ID");
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Transactional
    public Usuario autenticar(String credential) {
        final GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("No se pudo verificar la identidad de Google.");
        }
        if (token == null || token.getPayload() == null) {
            throw new SolicitudInvalidaException("Credencial de Google inválida o vencida.");
        }
        GoogleIdToken.Payload payload = token.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new SolicitudInvalidaException("Google no ha verificado este correo electrónico.");
        }
        String sub = payload.getSubject();
        String correo = payload.getEmail();
        if (sub == null || sub.isBlank() || correo == null || correo.isBlank()) {
            throw new SolicitudInvalidaException("Google no entregó los datos necesarios.");
        }
        correo = correo.trim().toLowerCase(Locale.ROOT);
        var existente = usuarios.findByGoogleSub(sub);
        if (existente.isPresent()) return existente.get();

        // No vincular automáticamente una cuenta existente por coincidencia de correo:
        // podría contener un rol administrativo o pertenecer a otro método de acceso.
        if (usuarios.existsByCorreo(correo)) {
            throw new SolicitudInvalidaException(
                    "Ya existe una cuenta con ese correo. Ingresa con tu contraseña habitual.");
        }

        Usuario usuario = new Usuario();
        // El esquema antiguo obliga a un RUN de 9 caracteres. Este ID interno
        // no representa un RUN chileno: no debe usarse para boletas ni despacho.
        String id;
        do {
            id = "G" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        } while (usuarios.existsById(id));
        usuario.setRun(id);
        usuario.setGoogleSub(sub);
        usuario.setCorreo(correo);
        usuario.setNombre(recortar((String) payload.get("given_name"), 50, "Cliente"));
        usuario.setApellidos(recortar((String) payload.get("family_name"), 100, "Google"));
        usuario.setTipo("Cliente");
        usuario.setPasswordHash(encoder.encode(UUID.randomUUID().toString() + UUID.randomUUID()));
        Usuario creado = usuarios.save(usuario);
        // Enviar bienvenida solo al crear la cuenta, no en cada inicio de sesión con Google.
        eventos.publishEvent(new RegistroCreadoEvent(creado.getCorreo(), creado.getNombre()));
        return creado;
    }

    private String recortar(String texto, int max, String defecto) {
        String limpio = texto == null || texto.isBlank() ? defecto : texto.trim();
        return limpio.substring(0, Math.min(limpio.length(), max));
    }
}
