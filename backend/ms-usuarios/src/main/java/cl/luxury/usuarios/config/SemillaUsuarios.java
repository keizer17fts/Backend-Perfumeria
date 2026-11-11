package cl.luxury.usuarios.config;

import cl.luxury.usuarios.model.Usuario;
import cl.luxury.usuarios.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Crea los usuarios demo (los mismos de seed/usuarios.js) la primera vez que arranca el servicio,
 * guardando la contraseña con BCrypt. Solo actúa si la tabla está vacía.
 */
@Component
@ConditionalOnProperty(name = "luxury.seed.usuarios", havingValue = "true", matchIfMissing = false)
public class SemillaUsuarios implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public SemillaUsuarios(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) {
            return;
        }
        usuarios.save(demo("190110222", "Administrador", "Luxury", "admin@duoc.cl", "admin123", null,
                "Administrador", "Región Metropolitana", "Santiago", "Casa matriz Luxury"));
        usuarios.save(demo("152345674", "Valentina", "Rojas", "vendedor@duoc.cl", "vende123", LocalDate.of(1995, 4, 12),
                "Vendedor", "Región Metropolitana", "Providencia", "Sucursal Providencia 123"));
        usuarios.save(demo("20987654K", "Camila", "Soto", "cliente@gmail.com", "cliente123", LocalDate.of(2000, 8, 30),
                "Cliente", "Valparaíso", "Viña del Mar", "Av. Perú 456"));
    }

    private Usuario demo(String run, String nombre, String apellidos, String correo, String clave,
                         LocalDate nacimiento, String tipo, String region, String comuna, String direccion) {
        Usuario u = new Usuario();
        u.setRun(run);
        u.setNombre(nombre);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setPasswordHash(encoder.encode(clave));
        u.setFechaNacimiento(nacimiento);
        u.setTipo(tipo);
        u.setRegion(region);
        u.setComuna(comuna);
        u.setDireccion(direccion);
        return u;
    }
}
