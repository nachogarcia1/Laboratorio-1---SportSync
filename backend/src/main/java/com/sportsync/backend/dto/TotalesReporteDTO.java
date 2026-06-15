package com.sportsync.backend.dto;

/** Totales mensuales del reporte. */
public record TotalesReporteDTO(
        long cantidad,
        long confirmadas,     // estado ACTIVA
        long canceladas,      // CANCELADA + RECHAZADA
        long pendientes,      // PENDIENTE_PAGO
        double ingresosBrutos,    // Σ precioTotal de todas
        double descuentos,        // Σ monto de descuentos
        double ingresosExtras,    // Σ extras
        double ingresosCobrados   // Σ precioTotal SOLO de pagos APROBADO
) {}
