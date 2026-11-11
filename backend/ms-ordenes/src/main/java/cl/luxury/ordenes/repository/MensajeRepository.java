package cl.luxury.ordenes.repository;

import cl.luxury.ordenes.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MensajeRepository extends JpaRepository<Mensaje, Long>, JpaSpecificationExecutor<Mensaje> {
}
