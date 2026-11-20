package cl.luxury.usuarios.service;

import cl.luxury.usuarios.dto.UsuarioRequest;
import cl.luxury.usuarios.exception.*;
import cl.luxury.usuarios.model.Usuario;
import cl.luxury.usuarios.repository.UsuarioRepository;
import cl.luxury.usuarios.correo.RegistroCreadoEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {
    UsuarioRepository repo; PasswordEncoder encoder; ApplicationEventPublisher eventos; UsuarioService servicio;
    @BeforeEach void preparar() {
        repo=mock(UsuarioRepository.class); encoder=mock(PasswordEncoder.class); eventos=mock(ApplicationEventPublisher.class);
        servicio=new UsuarioService(repo,encoder,new ObjectMapper(),mock(Validator.class),eventos);
    }
    UsuarioRequest solicitud() {
        UsuarioRequest r=new UsuarioRequest(); r.setRun("19.011.022-2"); r.setNombre(" Ana ");
        r.setApellidos(" Perez "); r.setCorreo(" ANA@EXAMPLE.COM "); r.setPassword("clave123"); return r;
    }
    @Test void rechazaRunRepetido() {
        when(repo.existsById("190110222")).thenReturn(true);
        assertThrows(ConflictoException.class,()->servicio.crear(solicitud()));
        verify(repo,never()).save(any()); verifyNoInteractions(eventos);
    }
    @Test void rechazaCorreoRepetido() {
        when(repo.existsByCorreoAndRunNot("ana@example.com","190110222")).thenReturn(true);
        assertThrows(ConflictoException.class,()->servicio.crear(solicitud()));
        verify(repo,never()).save(any()); verifyNoInteractions(eventos);
    }
    @Test void guardaHashNormalizaDatosYPublicaRegistro() {
        when(encoder.encode("clave123")).thenReturn("hash-de-prueba");
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));
        Usuario u=servicio.crear(solicitud());
        assertEquals("hash-de-prueba",u.getPasswordHash()); assertEquals("ana@example.com",u.getCorreo());
        assertEquals("Ana",u.getNombre()); assertEquals("Cliente",u.getTipo());
        verify(eventos).publishEvent(any(RegistroCreadoEvent.class));
    }
    @Test void rechazaContrasenaIncorrecta() {
        Usuario u=new Usuario();u.setPasswordHash("hash");
        when(repo.findByCorreo("ana@example.com")).thenReturn(Optional.of(u));
        when(encoder.matches("incorrecta","hash")).thenReturn(false);
        assertThrows(CredencialesInvalidasException.class,()->servicio.login(" ANA@EXAMPLE.COM ","incorrecta"));
    }
    @Test void noEliminaAlUnicoAdministrador() {
        Usuario u=new Usuario();u.setRun("190110222");u.setTipo("Administrador");
        when(repo.findById("190110222")).thenReturn(Optional.of(u));when(repo.countByTipo("Administrador")).thenReturn(1L);
        assertThrows(ConflictoException.class,()->servicio.eliminar("19.011.022-2"));verify(repo,never()).delete(any(Usuario.class));
    }
}
