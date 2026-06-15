package com.sportsync.backend.model.reserva;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sportsync.backend.model.admin.EstadoPago;
import com.sportsync.backend.model.admin.MetodoPago;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Operación de pago asociada a una reserva.
 * NO almacena datos sensibles de tarjeta (número, CVV): solo el id de la
 * transacción en el proveedor (Mercado Pago) y el estado.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    @JsonIgnore
    private Reserva reserva;

    @Column(nullable = false)
    private double monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    /** Id de la preferencia de checkout creada en el proveedor (Mercado Pago). */
    @Column
    private String preferenciaId;

    /** Id del pago/transacción en el proveedor (se completa al confirmar por webhook). */
    @Column
    private String proveedorId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Pago() {}

    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Long getId()              { return id; }

    @JsonIgnore
    public Reserva getReserva()      { return reserva; }

    @JsonProperty("reservaId")
    public Long getReservaId()       { return reserva != null ? reserva.getId() : null; }

    public double getMonto()         { return monto; }
    public MetodoPago getMetodo()    { return metodo; }
    public EstadoPago getEstado()    { return estado; }
    public String getPreferenciaId() { return preferenciaId; }
    public String getProveedorId()   { return proveedorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setReserva(Reserva reserva)        { this.reserva = reserva; }
    public void setMonto(double monto)             { this.monto = monto; }
    public void setMetodo(MetodoPago metodo)       { this.metodo = metodo; }
    public void setEstado(EstadoPago estado)       { this.estado = estado; }
    public void setPreferenciaId(String preferenciaId) { this.preferenciaId = preferenciaId; }
    public void setProveedorId(String proveedorId) { this.proveedorId = proveedorId; }
}
