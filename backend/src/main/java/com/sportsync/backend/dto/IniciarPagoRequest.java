package com.sportsync.backend.dto;

import com.sportsync.backend.model.admin.MetodoPago;

/** Body de POST /pagos/iniciar: los datos de la reserva + el método de pago elegido. */
public class IniciarPagoRequest {
    private CrearReservaRequest reserva;
    private MetodoPago metodo;

    public CrearReservaRequest getReserva() { return reserva; }
    public MetodoPago getMetodo()           { return metodo; }

    public void setReserva(CrearReservaRequest reserva) { this.reserva = reserva; }
    public void setMetodo(MetodoPago metodo)            { this.metodo = metodo; }
}
