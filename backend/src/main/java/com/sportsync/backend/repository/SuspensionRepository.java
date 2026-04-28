package com.sportsync.backend.repository;

import com.sportsync.backend.model.admin.Suspension;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SuspensionRepository extends JpaRepository<Suspension, Long> {
    Optional<Suspension> findByUsuarioIdAndActivaTrue(Long usuarioId);
}