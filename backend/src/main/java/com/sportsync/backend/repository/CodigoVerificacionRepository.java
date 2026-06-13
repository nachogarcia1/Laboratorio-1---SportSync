package com.sportsync.backend.repository;

import com.sportsync.backend.model.entidades.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    /** Último código emitido para un usuario (el más reciente). */
    Optional<CodigoVerificacion> findTopByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    /** Limpieza: borra todos los códigos previos de un usuario antes de emitir uno nuevo. */
    void deleteByUsuarioId(Long usuarioId);
}
