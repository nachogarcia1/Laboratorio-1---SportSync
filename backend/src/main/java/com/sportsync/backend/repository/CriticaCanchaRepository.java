package com.sportsync.backend.repository;

import com.sportsync.backend.model.critica.CriticaCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriticaCanchaRepository extends JpaRepository<CriticaCancha, Long> {

    List<CriticaCancha> findByCanchaId(Long canchaId);

    @Query("SELECT c FROM CriticaCancha c JOIN FETCH c.cancha ca JOIN FETCH ca.sede WHERE c.usuario.id = :usuarioId")
    List<CriticaCancha> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) > 0 FROM CriticaCancha c WHERE c.reserva.id = :reservaId")
    boolean existsByReservaId(@Param("reservaId") Long reservaId);

    @Query("SELECT AVG((c.notaCancha + c.notaStaff + c.notaServicios) / 3.0) FROM CriticaCancha c WHERE c.cancha.id = :canchaId")
    Double calcularRatingCancha(@Param("canchaId") Long canchaId);

    @Query("SELECT AVG((c.notaCancha + c.notaStaff + c.notaServicios) / 3.0) FROM CriticaCancha c WHERE c.cancha.sede.id = :sedeId")
    Double calcularRatingSede(@Param("sedeId") Long sedeId);
}