package com.sportsync.backend.repository;

import com.sportsync.backend.model.admin.EstadoMembresia;
import com.sportsync.backend.model.admin.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
    Optional<Membresia> findByUsuarioIdAndEstado(Long usuarioId, EstadoMembresia estado);
}
