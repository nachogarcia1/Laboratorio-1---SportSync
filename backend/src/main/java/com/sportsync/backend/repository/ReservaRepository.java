package com.sportsync.backend.repository;

import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.reserva.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Historial de un usuario
    List<Reserva> findByUsuarioId(Long usuarioId);

    // Reservas pasadas sin calificación de admin
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.usuario.id = :usuarioId
        AND (r.fecha < :hoy OR (r.fecha = :hoy AND r.horaFin < :ahora))
        AND NOT EXISTS (
            SELECT cu FROM CriticaUsuario cu WHERE cu.reserva.id = r.id
        )
    """)
    List<Reserva> findReservasSinCalificarAdmin(
            @Param("usuarioId") Long usuarioId,
            @Param("hoy") LocalDate hoy,
            @Param("ahora") LocalTime ahora
    );

    // Reservas activas de una cancha en una fecha
    List<Reserva> findByCanchaIdAndFechaAndEstado(Long canchaId, LocalDate fecha, EstadoReserva estado);

    // Validación anti-solapamiento (UC-31)
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.cancha.id = :canchaId
        AND r.fecha = :fecha
        AND r.estado = 'ACTIVA'
        AND r.horaInicio < :horaFin
        AND r.horaFin > :horaInicio
    """)
    boolean existeSolapamiento(
            @Param("canchaId") Long canchaId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );

    @Query("""
    SELECT r.horaInicio, COUNT(r)
    FROM Reserva r
    WHERE r.cancha.id = :canchaId
    AND r.fecha >= :desde
    AND r.estado <> com.sportsync.backend.model.admin.EstadoReserva.CANCELADA
    GROUP BY r.horaInicio
""")
    List<Object[]> contarReservasPorSlot(
            @Param("canchaId") Long canchaId,
            @Param("desde") LocalDate desde
    );

    long countByUsuarioIdAndEstado(Long usuarioId, EstadoReserva estado);
}