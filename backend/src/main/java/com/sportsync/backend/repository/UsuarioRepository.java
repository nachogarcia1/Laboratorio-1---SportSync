package com.sportsync.backend.repository;

import com.sportsync.backend.model.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.sportsync.backend.model.admin.EstadoUsuario;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    List<Usuario> findByEstado(EstadoUsuario estado);
}