package cl.luxury.ordenes.repository;

import cl.luxury.ordenes.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrdenRepository extends JpaRepository<Orden, Long>, JpaSpecificationExecutor<Orden> {
}
