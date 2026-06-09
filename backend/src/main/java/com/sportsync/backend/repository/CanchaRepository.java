package com.sportsync.backend.repository;

import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.admin.EstadoCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {
    List<Cancha> findBySedeId(Long sedeId);
    List<Cancha> findBySedeIdAndEstado(Long sedeId, EstadoCancha estado);
    List<Cancha> findByTipo(int tipo);
    List<Cancha> findBySedeIdAndTipo(Long sedeId, int tipo);
    List<Cancha> findBySedeIdAndTipoAndEstado(Long sedeId, int tipo, EstadoCancha estado);

    // Búsqueda combinada: nombre de cancha (contiene) + tipo opcional, solo HABILITADAS
    // :nombre debe venir pre-calculado como "%" (todos) o "%texto%" (filtrado) — nunca null
    @Query("""
        SELECT c FROM Cancha c
        WHERE c.estado = :estado
        AND LOWER(c.nombre) LIKE :nombre
        AND (:tipo IS NULL OR c.tipo = :tipo)
    """)
    List<Cancha> buscar(
            @Param("estado") EstadoCancha estado,
            @Param("nombre") String nombre,
            @Param("tipo")   Integer tipo
    );
}