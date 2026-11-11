package cl.luxury.catalogo.repository;
import cl.luxury.catalogo.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Long> {
  List<Resena> findByProductoIdOrderByFechaDesc(Long productoId);
  boolean existsByProductoIdAndUsuarioId(Long productoId, String usuarioId);
}
