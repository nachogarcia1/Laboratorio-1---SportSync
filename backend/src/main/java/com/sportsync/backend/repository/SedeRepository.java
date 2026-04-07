package com.sportsync.backend.repository;

import com.sportsync.backend.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SedeRepository extends JpaRepository<Sede, Long> {
    List<Sede> findByActivaTrue();
}