package com.sportsync.backend.dto;

import com.sportsync.backend.model.chat.RemitenteChat;
import java.time.LocalDateTime;

public class ChatMensajeDTO {
    private Long id;
    private Long usuarioId;
    private String contenido;
    private RemitenteChat remitente;
    private LocalDateTime timestamp;

    public ChatMensajeDTO() {}

    public ChatMensajeDTO(Long id, Long usuarioId, String contenido, RemitenteChat remitente, LocalDateTime timestamp) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.contenido = contenido;
        this.remitente = remitente;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public RemitenteChat getRemitente() { return remitente; }
    public void setRemitente(RemitenteChat remitente) { this.remitente = remitente; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}