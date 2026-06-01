package com.sportsync.backend.repository;

import com.sportsync.backend.model.reserva.DescuentoHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DescuentoHorarioRepository extends JpaRepository<DescuentoHorario, Long> {

    Optional<DescuentoHorario> findByCanchaIdAndHora(Long canchaId, LocalTime hora);

    List<DescuentoHorario> findByCanchaId(Long canchaId);
}