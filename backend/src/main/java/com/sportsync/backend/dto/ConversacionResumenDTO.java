package com.sportsync.backend.dto;

import java.time.LocalDateTime;

public class ConversacionResumenDTO {
    private Long usuarioId;
    private String usuarioNombre;
    private String ultimoMensaje;
    private LocalDateTime ultimoTimestamp;
    private long noLeidos;

    public ConversacionResumenDTO(Long usuarioId, String usuarioNombre, String ultimoMensaje, LocalDateTime ultimoTimestamp, long noLeidos) {
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.ultimoMensaje = ultimoMensaje;
        this.ultimoTimestamp = ultimoTimestamp;
        this.noLeidos = noLeidos;
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public String getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }
    public LocalDateTime getUltimoTimestamp() { return ultimoTimestamp; }
    public void setUltimoTimestamp(LocalDateTime ultimoTimestamp) { this.ultimoTimestamp = ultimoTimestamp; }
    public long getNoLeidos() { return noLeidos; }
    public void setNoLeidos(long noLeidos) { this.noLeidos = noLeidos; }
}