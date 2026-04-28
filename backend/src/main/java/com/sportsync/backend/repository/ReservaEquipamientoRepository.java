package com.sportsync.backend.repository;

import com.sportsync.backend.model.reserva.ReservaEquipamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservaEquipamientoRepository extends JpaRepository<ReservaEquipamiento, Long> {
    List<ReservaEquipamiento> findByReservaId(Long reservaId);
}