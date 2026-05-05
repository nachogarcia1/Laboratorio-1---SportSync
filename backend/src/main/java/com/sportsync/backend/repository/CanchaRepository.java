package com.sportsync.backend.repository;

import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.admin.EstadoCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {
    List<Cancha> findBySedeId(Long sedeId);
    List<Cancha> findBySedeIdAndEstado(Long sedeId, EstadoCancha estado);
    List<Cancha> findByTipo(int tipo);
    List<Cancha> findBySedeIdAndTipo(Long sedeId, int tipo);
    List<Cancha> findBySedeIdAndTipoAndEstado(Long sedeId, int tipo, EstadoCancha estado);
}