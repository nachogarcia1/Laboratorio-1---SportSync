package com.sportsync.backend.model;

import jakarta.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "sede")
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    // Ej: "Lunes a Viernes 08:00-22:00, Sábados 09:00-18:00"
    @Column(nullable = false)
    private String horarios;

    @Column(nullable = false)
    private boolean activa = true;

    @OneToMany(mappedBy = "sede", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cancha> canchas;

    // Constructor vacío (obligatorio para JPA)
    public Sede() {}

    // Getters
    public Long getId()           { return id; }
    public String getNombre()     { return nombre; }
    public String getDireccion()  { return direccion; }
    public String getHorarios()   { return horarios; }
    public boolean isActiva()     { return activa; }

    @JsonIgnore
    public List<Cancha> getCanchas() { return canchas; }

    // Setters
    public void setNombre(String nombre)       { this.nombre = nombre; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public void setHorarios(String horarios)   { this.horarios = horarios; }
    public void setActiva(boolean activa)      { this.activa = activa; }
    public void setCanchas(List<Cancha> canchas) { this.canchas = canchas; }
}