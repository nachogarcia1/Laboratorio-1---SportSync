package com.sportsync.backend.model;

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
    private int nota; // 1 a 5

    @Column
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public CriticaCancha() {}

    public Long getId()              { return id; }
    public Usuario getUsuario()      { return usuario; }
    public Cancha getCancha()        { return cancha; }
    public Reserva getReserva()      { return reserva; }
    public int getNota()             { return nota; }
    public String getComentario()    { return comentario; }
    public LocalDateTime getFecha()  { return fecha; }

    public void setUsuario(Usuario usuario)      { this.usuario = usuario; }
    public void setCancha(Cancha cancha)         { this.cancha = cancha; }
    public void setReserva(Reserva reserva)      { this.reserva = reserva; }
    public void setNota(int nota)                { this.nota = nota; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}