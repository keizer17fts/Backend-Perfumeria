package cl.luxury.usuarios.service;

import cl.luxury.usuarios.dto.UsuarioRequest;
import cl.luxury.usuarios.dto.CambioPasswordRequest;
import cl.luxury.usuarios.exception.ConflictoException;
import cl.luxury.usuarios.exception.CredencialesInvalidasException;
import cl.luxury.usuarios.exception.NoEncontradoException;
import cl.luxury.usuarios.exception.SolicitudInvalidaException;
import cl.luxury.usuarios.model.Usuario;
import cl.luxury.usuarios.repository.UsuarioRepository;
import cl.luxury.usuarios.util.RunUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.context.ApplicationEventPublisher;
import cl.luxury.usuarios.correo.RegistroCreadoEvent;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UsuarioService {

    private static final String ADMIN = "Administrador";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final ObjectMapper mapper;
    private final Validator validador;
    private final ApplicationEventPublisher eventos;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder,
                          ObjectMapper mapper, Validator validador, ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.mapper = mapper;
        this.validador = validador;
        this.eventos = eventos;
    }

    /** Filtros opcionales: ?correo=..., ?run=..., ?tipo=... (los que usa el registro y el admin). */
    @Transactional(readOnly = true)
    public List<Usuario> listar(Map<String, String> filtros) {
        Specification<Usuario> spec = (root, query, cb) -> cb.conjunction();

        String correo = filtros.get("correo");
        if (correo != null) {
            final String valor = correo.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("correo"), valor));
        }
        String run = filtros.get("run");
        if (run != null) {
            final String valor = RunUtil.limpiar(run);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("run"), valor));
        }
        String tipo = filtros.get("tipo");
        if (tipo != null) {
            final String valor = tipo.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), valor));
        }
        return usuarios.findAll(spec, Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public Usuario obtener(String id) {
        return usuarios.findById(RunUtil.limpiar(id))
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));
    }

    @Transactional
    public Usuario crear(UsuarioRequest r) {
        String run = RunUtil.limpiar(r.getRun());
        exigirRunValido(run);
        if (r.getPassword() == null) {
            throw new SolicitudInvalidaException("La contraseña es obligatoria (entre 4 y 10 caracteres).");
        }
        if (usuarios.existsById(run)) {
            throw new ConflictoException("Ya existe un usuario con el RUN " + run);
        }
        Usuario nuevo = new Usuario();
        nuevo.setRun(run);
        nuevo.setTipo("Cliente");
        Usuario creado = guardarCambios(nuevo, r);
        eventos.publishEvent(new RegistroCreadoEvent(creado.getCorreo(), creado.getNombre()));
        return creado;
    }

    /** PUT: reemplaza los datos. Si no viene password se conserva la actual. El RUN no se puede cambiar. */
    @Transactional
    public Usuario actualizar(String id, UsuarioRequest r) {
        Usuario actual = obtener(id);
        if (!actual.getRun().equals(RunUtil.limpiar(r.getRun()))) {
            throw new SolicitudInvalidaException("El RUN no se puede cambiar: es el identificador del usuario.");
        }
        return guardarCambios(actual, r);
    }

    /** PATCH: cambia solo los campos enviados (ej. el perfil). */
    @Transactional
    public Usuario modificar(String id, JsonNode cambios) {
        Usuario actual = obtener(id);
        UsuarioRequest r = aSolicitud(actual);
        try {
            mapper.readerForUpdating(r).readValue(cambios);
        } catch (IOException e) {
            throw new SolicitudInvalidaException("Los cambios enviados no son válidos.");
        }
        r.setRun(actual.getRun()); // el RUN nunca cambia
        Set<ConstraintViolation<UsuarioRequest>> errores = validador.validate(r);
        if (!errores.isEmpty()) {
            throw new ConstraintViolationException(errores);
        }
        return guardarCambios(actual, r);
    }

    @Transactional
    public void eliminar(String id) {
        Usuario usuario = obtener(id);
        if (ADMIN.equals(usuario.getTipo()) && usuarios.countByTipo(ADMIN) <= 1) {
            throw new ConflictoException("No se puede eliminar al único administrador.");
        }
        usuarios.delete(usuario);
    }

    /** Login: compara con BCrypt. Devuelve el usuario (el hash nunca se serializa). */
    @Transactional(readOnly = true)
    public Usuario login(String correo, String password) {
        Usuario usuario = usuarios.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new CredencialesInvalidasException("correo", "Usuario no encontrado."));
        if (!encoder.matches(password, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("password", "Contraseña incorrecta.");
        }
        return usuario;
    }

    /** Revalida la contraseña anterior antes de sustituir el hash BCrypt. */
    @Transactional
    public void cambiarPassword(String id, CambioPasswordRequest r) {
        Usuario usuario = obtener(id);
        if (!usuario.getCorreo().equalsIgnoreCase(r.correo().trim())
                || !encoder.matches(r.passwordActual(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("password", "Contraseña actual incorrecta.");
        }
        if (encoder.matches(r.passwordNueva(), usuario.getPasswordHash())) {
            throw new SolicitudInvalidaException("La nueva contraseña debe ser diferente de la actual.");
        }
        usuario.setPasswordHash(encoder.encode(r.passwordNueva()));
        usuarios.save(usuario);
    }

    // ---------------------------------------------------------------- helpers

    private Usuario guardarCambios(Usuario u, UsuarioRequest r) {
        String correo = r.getCorreo().trim().toLowerCase();
        if (usuarios.existsByCorreoAndRunNot(correo, u.getRun())) {
            throw new ConflictoException("Ya existe un usuario con el correo " + correo);
        }
        if (r.getTipo() != null && !r.getTipo().isBlank()) {
            boolean dejaDeSerAdmin = ADMIN.equals(u.getTipo()) && !ADMIN.equals(r.getTipo());
            if (dejaDeSerAdmin && usuarios.countByTipo(ADMIN) <= 1) {
                throw new ConflictoException("No se puede quitar el rol al único administrador.");
            }
            u.setTipo(r.getTipo());
        }
        u.setNombre(r.getNombre().trim());
        u.setApellidos(r.getApellidos().trim());
        u.setCorreo(correo);
        u.setFechaNacimiento(parsearFecha(r.getFechaNacimiento()));
        u.setRegion(vacioANulo(r.getRegion()));
        u.setComuna(vacioANulo(r.getComuna()));
        u.setDireccion(vacioANulo(r.getDireccion()));
        if (r.getPassword() != null) {
            u.setPasswordHash(encoder.encode(r.getPassword()));
        }
        return usuarios.save(u);
    }

    private UsuarioRequest aSolicitud(Usuario u) {
        UsuarioRequest r = new UsuarioRequest();
        r.setRun(u.getRun());
        r.setNombre(u.getNombre());
        r.setApellidos(u.getApellidos());
        r.setCorreo(u.getCorreo());
        r.setFechaNacimiento(u.getFechaNacimiento() == null ? null : u.getFechaNacimiento().toString());
        r.setTipo(u.getTipo());
        r.setRegion(u.getRegion());
        r.setComuna(u.getComuna());
        r.setDireccion(u.getDireccion());
        return r;
    }

    private void exigirRunValido(String run) {
        if (!RunUtil.esValido(run)) {
            throw new SolicitudInvalidaException("RUN inválido. Verifica el dígito verificador.");
        }
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new SolicitudInvalidaException("La fecha de nacimiento debe tener formato yyyy-MM-dd.");
        }
    }

    private String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
