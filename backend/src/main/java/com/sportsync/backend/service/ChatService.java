package com.sportsync.backend.service;

import com.sportsync.backend.dto.ChatMensajeDTO;
import com.sportsync.backend.dto.ConversacionResumenDTO;
import com.sportsync.backend.model.chat.MensajeChat;
import com.sportsync.backend.model.chat.RemitenteChat;
import com.sportsync.backend.model.entidades.Usuario;
import com.sportsync.backend.repository.MensajeChatRepository;
import com.sportsync.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final MensajeChatRepository chatRepo;
    private final UsuarioRepository usuarioRepo;

    public ChatService(MensajeChatRepository chatRepo, UsuarioRepository usuarioRepo) {
        this.chatRepo = chatRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Transactional
    public ChatMensajeDTO guardarMensaje(Long usuarioId, String contenido, RemitenteChat remitente) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        MensajeChat msg = new MensajeChat();
        msg.setUsuario(usuario);
        msg.setContenido(contenido);
        msg.setRemitente(remitente);
        msg.setTimestamp(LocalDateTime.now());
        msg.setLeido(false);

        return toDTO(chatRepo.save(msg));
    }

    public List<ChatMensajeDTO> getHistorial(Long usuarioId) {
        return chatRepo.findByUsuarioIdOrderByTimestampAsc(usuarioId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ConversacionResumenDTO> getConversaciones() {
        return chatRepo.findUsuarioIdsConChat().stream()
                .map(uid -> {
                    Optional<Usuario> uOpt = usuarioRepo.findById(uid);
                    if (uOpt.isEmpty()) return null;
                    Usuario u = uOpt.get();
                    MensajeChat ultimo = chatRepo.findTopByUsuarioIdOrderByTimestampDesc(uid).orElse(null);
                    long noLeidos = chatRepo.countByUsuarioIdAndRemitenteAndLeidoFalse(uid, RemitenteChat.USUARIO);
                    return new ConversacionResumenDTO(
                            uid, u.getNombre(),
                            ultimo != null ? ultimo.getContenido() : "",
                            ultimo != null ? ultimo.getTimestamp() : null,
                            noLeidos
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void marcarLeidos(Long usuarioId) {
        chatRepo.marcarTodosLeidos(usuarioId);
    }

    private ChatMensajeDTO toDTO(MensajeChat m) {
        return new ChatMensajeDTO(m.getId(), m.getUsuario().getId(), m.getContenido(), m.getRemitente(), m.getTimestamp());
    }
}