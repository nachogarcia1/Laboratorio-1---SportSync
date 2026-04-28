package com.sportsync.backend.repository;

import com.sportsync.backend.model.cancha.ItemEquipamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemEquipamientoRepository extends JpaRepository<ItemEquipamiento, Long> {
    List<ItemEquipamiento> findByDisponibleTrue();
}