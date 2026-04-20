package com.sportsync.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "suspension")
public class Suspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSuspension tipo;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activa = true;

    public Suspension() {}

    public Long getId()              { return id; }
    public Usuario getUsuario()      { return usuario; }
    public TipoSuspension getTipo()  { return tipo; }
    public LocalDate getFechaInicio(){ return fechaInicio; }
    public LocalDate getFechaFin()   { return fechaFin; }
    public boolean isActiva()        { return activa; }

    public void setUsuario(Usuario usuario)      { this.usuario = usuario; }
    public void setTipo(TipoSuspension tipo)     { this.tipo = tipo; }
    public void setFechaInicio(LocalDate f)      { this.fechaInicio = f; }
    public void setFechaFin(LocalDate f)         { this.fechaFin = f; }
    public void setActiva(boolean activa)        { this.activa = activa; }
}
