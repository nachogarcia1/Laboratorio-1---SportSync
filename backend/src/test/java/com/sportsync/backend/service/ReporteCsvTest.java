package com.sportsync.backend.service;

import com.sportsync.backend.dto.ReporteMensualDTO;
import com.sportsync.backend.dto.ReservaReporteDTO;
import com.sportsync.backend.dto.TotalesReporteDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteCsvTest {

    private final ReporteCsv csv = new ReporteCsv();

    private ReservaReporteDTO fila(String usuario, String sede) {
        return new ReservaReporteDTO(1L, usuario, "a@b.com", sede, "Cancha 1",
                "2026-06-10", "19:00", "20:00", 1.0, 1000, 100, 500, 1400,
                "ACTIVA", "APROBADO", "TARJETA_CREDITO");
    }

    @Test
    void empiezaConBomYTieneEncabezados() {
        String out = csv.generar(new ReporteMensualDTO(6, 2026, List.of(fila("Juan", "Palermo")),
                new TotalesReporteDTO(1, 1, 0, 0, 1400, 100, 500, 1400)));
        assertTrue(out.startsWith(ReporteCsv.BOM), "debe arrancar con BOM");
        assertTrue(out.contains("ID,Usuario,Email"), "debe tener encabezados");
        assertTrue(out.contains("TOTALES"), "debe tener fila de totales");
    }

    @Test
    void escapaComasYComillas() {
        // Nombre con coma → debe ir entre comillas; comillas internas → duplicadas
        String out = csv.generar(new ReporteMensualDTO(6, 2026,
                List.of(fila("Perez, Juan \"JP\"", "Sede \"Centro\"")),
                new TotalesReporteDTO(1, 1, 0, 0, 1400, 100, 500, 1400)));
        assertTrue(out.contains("\"Perez, Juan \"\"JP\"\"\""), "debe escapar coma y comillas del usuario");
        assertTrue(out.contains("\"Sede \"\"Centro\"\"\""), "debe escapar comillas de la sede");
    }

    @Test
    void reporteVacioGeneraEncabezadosYTotalesEnCero() {
        String out = csv.generar(new ReporteMensualDTO(6, 2026, List.of(),
                new TotalesReporteDTO(0, 0, 0, 0, 0, 0, 0, 0)));
        assertTrue(out.contains("ID,Usuario,Email"));
        assertTrue(out.contains("TOTALES"));
    }
}
