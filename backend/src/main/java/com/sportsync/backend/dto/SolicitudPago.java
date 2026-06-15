package com.sportsync.backend.dto;

import java.math.BigDecimal;

/** Solicitud de cobro a la pasarela: cuánto, por qué concepto y con qué tarjeta. */
public class SolicitudPago {
    private final BigDecimal monto;
    private final String concepto;
    private final DatosTarjeta tarjeta;

    public SolicitudPago(BigDecimal monto, String concepto, DatosTarjeta tarjeta) {
        this.monto = monto;
        this.concepto = concepto;
        this.tarjeta = tarjeta;
    }

    public BigDecimal getMonto()    { return monto; }
    public String getConcepto()     { return concepto; }
    public DatosTarjeta getTarjeta() { return tarjeta; }
}
