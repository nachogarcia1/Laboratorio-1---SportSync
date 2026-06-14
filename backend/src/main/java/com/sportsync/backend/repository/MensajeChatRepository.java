package com.sportsync.backend.repository;

import com.sportsync.backend.model.chat.MensajeChat;
import com.sportsync.backend.model.chat.RemitenteChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {

    List<MensajeChat> findByUsuarioIdOrderByTimestampAsc(Long usuarioId);

    Optional<MensajeChat> findTopByUsuarioIdOrderByTimestampDesc(Long usuarioId);

    @Query("SELECT DISTINCT m.usuario.id FROM MensajeChat m")
    List<Long> findUsuarioIdsConChat();

    long countByUsuarioIdAndRemitenteAndLeidoFalse(Long usuarioId, RemitenteChat remitente);

    @Modifying
    @Query("UPDATE MensajeChat m SET m.leido = true WHERE m.usuario.id = :usuarioId AND m.leido = false")
    void marcarTodosLeidos(@Param("usuarioId") Long usuarioId);
}