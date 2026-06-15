package com.sportsync.backend.dto;

import java.util.List;

/** Reporte mensual de reservas: filas + totales. */
public record ReporteMensualDTO(
        int mes,
        int anio,
        List<ReservaReporteDTO> reservas,
        TotalesReporteDTO totales
) {}
