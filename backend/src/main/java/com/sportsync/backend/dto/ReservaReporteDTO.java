package com.sportsync.backend.dto;

/** Una fila del reporte mensual (valores ya congelados al momento de la reserva). */
public record ReservaReporteDTO(
        Long id,
        String usuario,
        String email,
        String sede,
        String cancha,
        String fecha,         // ISO yyyy-MM-dd
        String horaInicio,    // HH:mm
        String horaFin,       // HH:mm
        double duracionHoras,
        double precioBase,
        double descuento,     // monto del descuento por precio inteligente
        double extras,        // iluminación + equipamiento
        double precioTotal,
        String estadoReserva,
        String estadoPago,    // "—" si no hay pago registrado
        String metodoPago     // "—" si no aplica
) {}
