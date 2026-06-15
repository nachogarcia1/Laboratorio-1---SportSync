package com.sportsync.backend.service;

import com.sportsync.backend.dto.ReporteMensualDTO;
import com.sportsync.backend.dto.ReservaReporteDTO;
import com.sportsync.backend.dto.TotalesReporteDTO;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Genera el CSV del reporte mensual. UTF-8 con BOM (para que Excel detecte la
 * codificación) y escapado RFC-4180 (comillas, comas y saltos de línea).
 */
@Component
public class ReporteCsv {

    public static final String BOM = "﻿";

    private static final String[] CABECERAS = {
            "ID", "Usuario", "Email", "Sede", "Cancha", "Fecha", "Hora inicio", "Hora fin",
            "Duración (h)", "Precio base", "Descuento", "Extras", "Total",
            "Estado reserva", "Estado pago", "Método pago"
    };

    public String generar(ReporteMensualDTO reporte) {
        StringBuilder sb = new StringBuilder(BOM);
        fila(sb, (Object[]) CABECERAS);

        for (ReservaReporteDTO r : reporte.reservas()) {
            fila(sb,
                    r.id(), r.usuario(), r.email(), r.sede(), r.cancha(), r.fecha(),
                    r.horaInicio(), r.horaFin(), num(r.duracionHoras()),
                    num(r.precioBase()), num(r.descuento()), num(r.extras()), num(r.precioTotal()),
                    r.estadoReserva(), r.estadoPago(), r.metodoPago());
        }

        // Fila de totales
        TotalesReporteDTO t = reporte.totales();
        fila(sb, "TOTALES", "", "", "", "",
                "Reservas: " + t.cantidad(),
                "Confirmadas: " + t.confirmadas(),
                "Canceladas: " + t.canceladas(),
                "Pendientes: " + t.pendientes(),
                num(t.ingresosBrutos()), num(t.descuentos()), num(t.ingresosExtras()), num(t.ingresosCobrados()),
                "", "", "");
        return sb.toString();
    }

    private void fila(StringBuilder sb, Object... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escapar(campos[i] == null ? "" : campos[i].toString()));
        }
        sb.append("\r\n");
    }

    private String escapar(String valor) {
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private String num(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
