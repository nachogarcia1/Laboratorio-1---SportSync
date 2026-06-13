package com.sportsync.backend.model.sede;

import com.sportsync.backend.model.cancha.Cancha;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;

@Entity
@Table(name = "sede")
public class Sede {

    /** Fábrica JTS con SRID 4326 (WGS-84). Coordinate(x, y) → x = longitud, y = latitud. */
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

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

    /** Buffer de deserialización: lat/lng llegan por JSON como campos separados del Point. */
    @Transient
    @JsonIgnore
    private Double latitudEntrante;

    @Transient
    @JsonIgnore
    private Double longitudEntrante;

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

    /** Permite recibir latitud por JSON (POST /sedes desde el mapa del admin). */
    @JsonProperty("latitud")
    public void setLatitud(Double latitud) {
        this.latitudEntrante = latitud;
        actualizarUbicacionDesdeEntrantes();
    }

    /** Permite recibir longitud por JSON (POST /sedes desde el mapa del admin). */
    @JsonProperty("longitud")
    public void setLongitud(Double longitud) {
        this.longitudEntrante = longitud;
        actualizarUbicacionDesdeEntrantes();
    }

    private void actualizarUbicacionDesdeEntrantes() {
        if (latitudEntrante != null && longitudEntrante != null) {
            this.ubicacion = GEOMETRY_FACTORY.createPoint(
                    new Coordinate(longitudEntrante, latitudEntrante));
        }
    }
}
