package com.sportsync.backend.dto;

import com.sportsync.backend.model.admin.EstadoResultadoPago;

/**
 * Resultado de un intento de cobro. Solo contiene información segura:
 * nunca el número completo de tarjeta ni el CVV.
 */
public class ResultadoPago {
    private final EstadoResultadoPago estado;
    private final String transaccionId; // id simulado (null si no se procesó)
    private final String ultimos4;       // últimos 4 dígitos (null si datos inválidos)
    private final String mensaje;

    private ResultadoPago(EstadoResultadoPago estado, String transaccionId, String ultimos4, String mensaje) {
        this.estado = estado;
        this.transaccionId = transaccionId;
        this.ultimos4 = ultimos4;
        this.mensaje = mensaje;
    }

    public static ResultadoPago aprobado(String transaccionId, String ultimos4, String mensaje) {
        return new ResultadoPago(EstadoResultadoPago.APROBADO, transaccionId, ultimos4, mensaje);
    }

    public static ResultadoPago rechazado(String ultimos4, String mensaje) {
        return new ResultadoPago(EstadoResultadoPago.RECHAZADO, null, ultimos4, mensaje);
    }

    public static ResultadoPago datosInvalidos(String mensaje) {
        return new ResultadoPago(EstadoResultadoPago.DATOS_INVALIDOS, null, null, mensaje);
    }

    public boolean fueAprobado() { return estado == EstadoResultadoPago.APROBADO; }

    public EstadoResultadoPago getEstado() { return estado; }
    public String getTransaccionId()       { return transaccionId; }
    public String getUltimos4()            { return ultimos4; }
    public String getMensaje()             { return mensaje; }
}
