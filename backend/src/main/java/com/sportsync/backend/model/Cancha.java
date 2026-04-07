package com.sportsync.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cancha")
public class Cancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    // 5, 7 u 11
    @Column(nullable = false)
    private int tipo;

    @Column(nullable = false)
    private double precioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCancha estado = EstadoCancha.HABILITADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    // Constructor vacío (obligatorio para JPA)
    public Cancha() {}

    // Getters
    public Long getId()              { return id; }
    public String getNombre()        { return nombre; }
    public int getTipo()             { return tipo; }
    public double getPrecioBase()    { return precioBase; }
    public EstadoCancha getEstado()  { return estado; }
    public Sede getSede()            { return sede; }

    // Setters
    public void setNombre(String nombre)          { this.nombre = nombre; }
    public void setTipo(int tipo)                 { this.tipo = tipo; }
    public void setPrecioBase(double precioBase)  { this.precioBase = precioBase; }
    public void setEstado(EstadoCancha estado)     { this.estado = estado; }
    public void setSede(Sede sede)                { this.sede = sede; }
}