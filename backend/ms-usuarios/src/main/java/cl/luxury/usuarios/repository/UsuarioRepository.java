package cl.luxury.usuarios.repository;

import cl.luxury.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByGoogleSub(String googleSub);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndRunNot(String correo, String run);

    long countByTipo(String tipo);
}
