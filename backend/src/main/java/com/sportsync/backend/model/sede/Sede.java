package com.sportsync.backend.model.sede;

import com.sportsync.backend.model.cancha.Cancha;
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

    @Column(nullable = false)
    private String horaApertura;

    @Column(nullable = false)
    private String horaCierre;

    @Column(nullable = false)
    private boolean activa = true;

    @OneToMany(mappedBy = "sede", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cancha> canchas;

    // Constructor vacío (obligatorio para JPA)
    public Sede() {}

    // Getters
    public Long getId()              { return id; }
    public String getNombre()        { return nombre; }
    public String getDireccion()     { return direccion; }
    public String getHoraApertura()  { return horaApertura; }
    public String getHoraCierre()    { return horaCierre; }
    public boolean isActiva()        { return activa; }

    @JsonIgnore
    public List<Cancha> getCanchas() { return canchas; }

    // Setters
    public void setNombre(String nombre)              { this.nombre = nombre; }
    public void setDireccion(String direccion)        { this.direccion = direccion; }
    public void setHoraApertura(String horaApertura)  { this.horaApertura = horaApertura; }
    public void setHoraCierre(String horaCierre)      { this.horaCierre = horaCierre; }
    public void setActiva(boolean activa)             { this.activa = activa; }
    public void setCanchas(List<Cancha> canchas) { this.canchas = canchas; }
}