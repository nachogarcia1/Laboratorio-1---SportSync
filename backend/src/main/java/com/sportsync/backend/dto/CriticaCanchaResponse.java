package com.sportsync.backend.dto;

import com.sportsync.backend.model.critica.CriticaCancha;
import java.time.LocalDateTime;

public class CriticaCanchaResponse {

    private Long          id;
    private Long          reservaId;
    private String        nombreCancha;
    private String        nombreSede;
    private int           notaCancha;
    private int           notaStaff;
    private int           notaServicios;
    private String        comentario;
    private LocalDateTime fecha;

    public CriticaCanchaResponse(CriticaCancha c) {
        this.id            = c.getId();
        this.reservaId     = c.getReserva() != null ? c.getReserva().getId() : null;
        this.nombreCancha  = c.getCancha().getNombre();
        this.nombreSede    = c.getCancha().getSede().getNombre();
        this.notaCancha    = c.getNotaCancha();
        this.notaStaff     = c.getNotaStaff();
        this.notaServicios = c.getNotaServicios();
        this.comentario    = c.getComentario();
        this.fecha         = c.getFecha();
    }

    public Long getId()              { return id; }
    public Long getReservaId()       { return reservaId; }
    public String getNombreCancha()  { return nombreCancha; }
    public String getNombreSede()    { return nombreSede; }
    public int getNotaCancha()       { return notaCancha; }
    public int getNotaStaff()        { return notaStaff; }
    public int getNotaServicios()    { return notaServicios; }
    public String getComentario()    { return comentario; }
    public LocalDateTime getFecha()  { return fecha; }
}
