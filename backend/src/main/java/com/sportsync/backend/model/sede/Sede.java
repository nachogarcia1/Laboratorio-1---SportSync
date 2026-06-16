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

    /** Zona horaria IANA de la sede (p.ej. "America/Argentina/Buenos_Aires"). */
    @Column(nullable = false)
    private String zonaHoraria = "America/Argentina/Buenos_Aires";

    /**
     * Ubicación geográfica almacenada como punto PostGIS (SRID 4326 = WGS-84).
     * Se serializa como latitud/longitud separados para compatibilidad con el frontend.
     */
    @Column(columnDefinition = "geometry(Point,4326)")
    @JsonIgnore
    private Point ubicacion;

    /** Coordenadas como columnas simples (fuente de verdad para el mapa). */
    @Column
    private Double latitud;

    @Column
    private Double longitud;

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
    public String getZonaHoraria()  { return zonaHoraria; }

    @JsonProperty("latitud")
    public Double getLatitud()  { return latitud; }

    @JsonProperty("longitud")
    public Double getLongitud() { return longitud; }

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
    public void setZonaHoraria(String zonaHoraria)   { this.zonaHoraria = zonaHoraria; }
    public void setUbicacion(Point ubicacion)        { this.ubicacion = ubicacion; }
    public void setCanchas(List<Cancha> canchas)     { this.canchas = canchas; }

    /** Recibe latitud por JSON (POST /sedes desde el mapa del admin) o por geocodificación. */
    @JsonProperty("latitud")
    public void setLatitud(Double latitud) {
        this.latitud = latitud;
        sincronizarUbicacion();
    }

    @JsonProperty("longitud")
    public void setLongitud(Double longitud) {
        this.longitud = longitud;
        sincronizarUbicacion();
    }

    /** Mantiene el Point PostGIS en sync (por compatibilidad); la lectura usa las columnas. */
    private void sincronizarUbicacion() {
        if (latitud != null && longitud != null) {
            this.ubicacion = GEOMETRY_FACTORY.createPoint(new Coordinate(longitud, latitud));
        }
    }
}
