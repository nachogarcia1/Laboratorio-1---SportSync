package com.sportsync.backend.exception;

/**
 * El turno pedido ya está reservado (o tiene un pago en curso). Se mapea a HTTP 409 (Conflict).
 * No extiende IllegalStateException a propósito: los controladores atrapan esa excepción como 400
 * y este caso tiene que distinguirse.
 */
public class TurnoNoDisponibleException extends RuntimeException {
    public TurnoNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public TurnoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
