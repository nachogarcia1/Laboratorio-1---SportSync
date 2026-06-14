package com.sportsync.backend.repository;

import com.sportsync.backend.model.cancha.ItemEquipamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemEquipamientoRepository extends JpaRepository<ItemEquipamiento, Long> {
    List<ItemEquipamiento> findByDisponibleTrue();

    /** Ítems activos ofrecidos en una sede: los propios de la sede + los globales (sede null). */
    @Query("""
        SELECT i FROM ItemEquipamiento i
        WHERE i.disponible = true
          AND (i.sede IS NULL OR i.sede.id = :sedeId)
    """)
    List<ItemEquipamiento> findDisponiblesParaSede(@Param("sedeId") Long sedeId);
}
