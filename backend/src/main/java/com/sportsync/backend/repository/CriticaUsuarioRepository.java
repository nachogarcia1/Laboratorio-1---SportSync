package com.sportsync.backend.repository;

import com.sportsync.backend.model.CriticaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriticaUsuarioRepository extends JpaRepository<CriticaUsuario, Long> {

    List<CriticaUsuario> findByUsuarioId(Long usuarioId);

    @Query("SELECT AVG(c.nota) FROM CriticaUsuario c WHERE c.usuario.id = :usuarioId")
    Double calcularRating(@Param("usuarioId") Long usuarioId);
}