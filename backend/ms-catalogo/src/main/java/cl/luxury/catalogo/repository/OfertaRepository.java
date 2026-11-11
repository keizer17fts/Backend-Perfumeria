package cl.luxury.catalogo.repository;

import cl.luxury.catalogo.model.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByActivaOrderByIdAsc(Boolean activa);

    void deleteByProductoId(Long productoId);
}
