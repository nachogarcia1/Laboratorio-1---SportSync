package com.sportsync.backend.model.admin;

/** Resultado de un intento de cobro con tarjeta (la pasarela devuelve esto, no excepciones). */
public enum EstadoResultadoPago {
    APROBADO,
    RECHAZADO,
    DATOS_INVALIDOS
}
