package com.sportsync.backend.controller;

import com.sportsync.backend.dto.ReporteMensualDTO;
import com.sportsync.backend.service.ReporteCsv;
import com.sportsync.backend.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Reportes administrativos. Protegido a ADMIN en Security (/reportes/**). */
@RestController
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteCsv reporteCsv;

    public ReporteController(ReporteService reporteService, ReporteCsv reporteCsv) {
        this.reporteService = reporteService;
        this.reporteCsv = reporteCsv;
    }

    // GET /reportes/reservas?mes=6&anio=2026
    @GetMapping("/reservas")
    public ResponseEntity<?> reservas(@RequestParam int mes, @RequestParam int anio) {
        try {
            return ResponseEntity.ok(reporteService.generar(mes, anio));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /reportes/reservas/csv?mes=6&anio=2026 → descarga
    @GetMapping("/reservas/csv")
    public ResponseEntity<?> reservasCsv(@RequestParam int mes, @RequestParam int anio) {
        try {
            ReporteMensualDTO reporte = reporteService.generar(mes, anio);
            byte[] cuerpo = reporteCsv.generar(reporte).getBytes(StandardCharsets.UTF_8);
            String nombre = String.format("reporte-%04d-%02d.csv", anio, mes);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombre)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(cuerpo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
