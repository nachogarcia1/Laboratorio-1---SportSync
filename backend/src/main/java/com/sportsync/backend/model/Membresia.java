package com.sportsync.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "membresia")
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMembresia estado = EstadoMembresia.ACTIVA;

    public Membresia() {}

    public Long getId()                    { return id; }
    public Usuario getUsuario()            { return usuario; }
    public LocalDate getFechaInicio()      { return fechaInicio; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public EstadoMembresia getEstado()     { return estado; }

    public void setUsuario(Usuario usuario)            { this.usuario = usuario; }
    public void setFechaInicio(LocalDate f)            { this.fechaInicio = f; }
    public void setFechaVencimiento(LocalDate f)       { this.fechaVencimiento = f; }
    public void setEstado(EstadoMembresia estado)      { this.estado = estado; }
}
