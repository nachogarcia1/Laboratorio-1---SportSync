package com.sportsync.backend.repository;

import com.sportsync.backend.model.reserva.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // @Query explícito: el getter computado getReservaId() confunde la derivación
    // automática (intenta resolver 'reservaId' como atributo en vez de reserva.id).
    @Query("SELECT p FROM Pago p WHERE p.reserva.id = :reservaId")
    Optional<Pago> findByReservaId(@Param("reservaId") Long reservaId);

    /** Busca el pago por el id de preferencia/transacción del proveedor (para el webhook). */
    Optional<Pago> findByPreferenciaId(String preferenciaId);

    Optional<Pago> findByProveedorId(String proveedorId);
}
