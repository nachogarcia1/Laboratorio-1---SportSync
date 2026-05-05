package com.sportsync.backend.model.critica;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.entidades.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "critica_cancha")
public class CriticaCancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Column(nullable = false)
    private int notaCancha;

    @Column(nullable = false)
    private int notaStaff;

    @Column(nullable = false)
    private int notaServicios;

    @Column
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public CriticaCancha() {}

    public Long getId()             { return id; }
    @JsonIgnore
    public Usuario getUsuario()     { return usuario; }
    @JsonIgnore
    public Cancha getCancha()       { return cancha; }
    @JsonIgnore
    public Reserva getReserva()     { return reserva; }
    public Long getUsuarioId()      { return usuario != null ? usuario.getId() : null; }
    public Long getCanchaId()       { return cancha != null ? cancha.getId() : null; }
    public Long getReservaId()      { return reserva != null ? reserva.getId() : null; }
    public int getNotaCancha()      { return notaCancha; }
    public int getNotaStaff()       { return notaStaff; }
    public int getNotaServicios()   { return notaServicios; }
    public String getComentario()   { return comentario; }
    public LocalDateTime getFecha() { return fecha; }

    public void setUsuario(Usuario usuario)      { this.usuario = usuario; }
    public void setCancha(Cancha cancha)         { this.cancha = cancha; }
    public void setReserva(Reserva reserva)      { this.reserva = reserva; }
    public void setNotaCancha(int notaCancha)         { this.notaCancha = notaCancha; }
    public void setNotaStaff(int notaStaff)           { this.notaStaff = notaStaff; }
    public void setNotaServicios(int notaServicios)   { this.notaServicios = notaServicios; }
    public void setComentario(String comentario)      { this.comentario = comentario; }
}