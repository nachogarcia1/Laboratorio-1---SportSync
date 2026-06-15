package com.sportsync.backend.model.admin;

public enum EstadoReserva {
    PENDIENTE_PAGO,  // creada, esperando confirmación de pago
    ACTIVA,          // pago aprobado / confirmada
    CANCELADA,
    RECHAZADA
}
