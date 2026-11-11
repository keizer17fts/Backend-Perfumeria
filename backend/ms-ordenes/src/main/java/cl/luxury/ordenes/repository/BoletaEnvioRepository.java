package cl.luxury.ordenes.repository;

import cl.luxury.ordenes.model.BoletaEnvio;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

public interface BoletaEnvioRepository extends JpaRepository<BoletaEnvio, Long> {
    List<BoletaEnvio> findTop10ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(String estado, Instant ahora);
    @Transactional @Modifying
    @Query("update BoletaEnvio b set b.estado='ENVIANDO', b.actualizado=:ahora where b.ordenId=:id and b.estado='PENDIENTE'")
    int reservar(@Param("id") Long id, @Param("ahora") Instant ahora);
    @Transactional @Modifying
    @Query("update BoletaEnvio b set b.estado='PENDIENTE' where b.estado='ENVIANDO' and b.actualizado<:limite")
    int recuperarInterrumpidos(@Param("limite") Instant limite);
}
