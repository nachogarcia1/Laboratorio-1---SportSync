package com.sportsync.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reserva_equipamiento")
public class ReservaEquipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEquipamiento item;

    @Column(nullable = false)
    private int cantidad;

    // Precio guardado al momento de la reserva (por si cambia después)
    @Column(nullable = false)
    private double precioUnitario;

    public ReservaEquipamiento() {}

    public Long getId()                    { return id; }
    public Reserva getReserva()            { return reserva; }
    public ItemEquipamiento getItem()      { return item; }
    public int getCantidad()               { return cantidad; }
    public double getPrecioUnitario()      { return precioUnitario; }

    public void setReserva(Reserva reserva)         { this.reserva = reserva; }
    public void setItem(ItemEquipamiento item)       { this.item = item; }
    public void setCantidad(int cantidad)            { this.cantidad = cantidad; }
    public void setPrecioUnitario(double precio)     { this.precioUnitario = precio; }
}