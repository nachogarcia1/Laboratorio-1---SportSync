package com.sportsync.backend.model.cancha;

import jakarta.persistence.*;

@Entity
@Table(name = "item_equipamiento")
public class ItemEquipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre; // pelota, pechera, etc.

    @Column(nullable = false)
    private double precioPorUnidad;

    @Column(nullable = false)
    private boolean disponible = true;

    public ItemEquipamiento() {}

    public Long getId()                  { return id; }
    public String getNombre()            { return nombre; }
    public double getPrecioPorUnidad()   { return precioPorUnidad; }
    public boolean isDisponible()        { return disponible; }

    public void setNombre(String nombre)               { this.nombre = nombre; }
    public void setPrecioPorUnidad(double precio)      { this.precioPorUnidad = precio; }
    public void setDisponible(boolean disponible)      { this.disponible = disponible; }
}