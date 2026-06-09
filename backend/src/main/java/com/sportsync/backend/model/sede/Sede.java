package com.sportsync.backend.model.sede;

import com.sportsync.backend.model.cancha.Cancha;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.util.List;

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

    /**
     * Ubicación geográfica almacenada como punto PostGIS (SRID 4326 = WGS-84).
     * Se serializa como latitud/longitud separados para compatibilidad con el frontend.
     */
    @Column(columnDefinition = "geometry(Point,4326)")
    @JsonIgnore
    private Point ubicacion;

    @OneToMany(mappedBy = "sede", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cancha> canchas;

    // Constructor vacío (obligatorio para JPA)
    public Sede() {}

    // Getters
    public Long getId()             { return id; }
    public String getNombre()       { return nombre; }
    public String getDireccion()    { return direccion; }
    public String getHoraApertura() { return horaApertura; }
    public String getHoraCierre()   { return horaCierre; }
    public boolean isActiva()       { return activa; }

    /** Latitud extraída del Point (Y en coordenadas JTS). */
    @JsonProperty("latitud")
    public Double getLatitud() {
        return ubicacion != null ? ubicacion.getY() : null;
    }

    /** Longitud extraída del Point (X en coordenadas JTS). */
    @JsonProperty("longitud")
    public Double getLongitud() {
        return ubicacion != null ? ubicacion.getX() : null;
    }

    @JsonIgnore
    public Point getUbicacion() { return ubicacion; }

    @JsonIgnore
    public List<Cancha> getCanchas() { return canchas; }

    // Setters
    public void setNombre(String nombre)             { this.nombre = nombre; }
    public void setDireccion(String direccion)       { this.direccion = direccion; }
    public void setHoraApertura(String horaApertura) { this.horaApertura = horaApertura; }
    public void setHoraCierre(String horaCierre)     { this.horaCierre = horaCierre; }
    public void setActiva(boolean activa)            { this.activa = activa; }
    public void setUbicacion(Point ubicacion)        { this.ubicacion = ubicacion; }
    public void setCanchas(List<Cancha> canchas)     { this.canchas = canchas; }
}