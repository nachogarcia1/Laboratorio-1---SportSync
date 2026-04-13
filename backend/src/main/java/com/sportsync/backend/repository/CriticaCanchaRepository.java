package com.sportsync.backend.repository;

import com.sportsync.backend.model.CriticaCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriticaCanchaRepository extends JpaRepository<CriticaCancha, Long> {

    List<CriticaCancha> findByCanchaId(Long canchaId);

    boolean existsByReservaId(Long reservaId);

    @Query("SELECT AVG(c.nota) FROM CriticaCancha c WHERE c.cancha.id = :canchaId")
    Double calcularRatingCancha(@Param("canchaId") Long canchaId);

    @Query("SELECT AVG(c.nota) FROM CriticaCancha c WHERE c.cancha.sede.id = :sedeId")
    Double calcularRatingSede(@Param("sedeId") Long sedeId);
}