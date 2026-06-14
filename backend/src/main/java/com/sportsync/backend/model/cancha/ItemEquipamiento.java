package com.sportsync.backend.model.cancha;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sportsync.backend.model.sede.Sede;
import jakarta.persistence.*;

@Entity
@Table(name = "item_equipamiento")
public class ItemEquipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre; // pelota, pechera, botines, guantes de arquero, etc.

    @Column(nullable = false)
    private double precioPorUnidad;

    /** Unidades disponibles del ítem. La reserva valida cantidad <= stock. */
    @Column(nullable = false)
    private int stock = 0;

    /** Activo/inactivo: si no está disponible no se ofrece al reservar. */
    @Column(nullable = false)
    private boolean disponible = true;

    /**
     * Sede a la que pertenece el ítem. Si es null, el ítem es global
     * (se ofrece en todas las sedes).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id")
    @JsonIgnore
    private Sede sede;

    public ItemEquipamiento() {}

    public Long getId()                  { return id; }
    public String getNombre()            { return nombre; }
    public double getPrecioPorUnidad()   { return precioPorUnidad; }
    public int getStock()                { return stock; }
    public boolean isDisponible()        { return disponible; }

    @JsonIgnore
    public Sede getSede()                { return sede; }

    /** Se serializa el id de la sede (o null si es global) para el frontend. */
    @JsonProperty("sedeId")
    public Long getSedeId()              { return sede != null ? sede.getId() : null; }

    public void setNombre(String nombre)               { this.nombre = nombre; }
    public void setPrecioPorUnidad(double precio)      { this.precioPorUnidad = precio; }
    public void setStock(int stock)                    { this.stock = stock; }
    public void setDisponible(boolean disponible)      { this.disponible = disponible; }
    public void setSede(Sede sede)                     { this.sede = sede; }
}
