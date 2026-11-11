package cl.luxury.catalogo.repository;

import cl.luxury.catalogo.model.AlertaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {
    boolean existsByProductoIdAndCorreoIgnoreCaseAndEnviadaFalse(Long productoId, String correo);
    List<AlertaStock> findByProductoIdAndEnviadaFalse(Long productoId);
}
