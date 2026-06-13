package com.sportsync.backend.controller;

import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.repository.CanchaRepository;
import com.sportsync.backend.repository.DescuentoHorarioRepository;
import com.sportsync.backend.service.PrecioInteligenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/precios")
public class PrecioInteligenteController {

    private final PrecioInteligenteService precioService;
    private final CanchaRepository        canchaRepo;
    private final DescuentoHorarioRepository descuentoRepo;

    public PrecioInteligenteController(PrecioInteligenteService precioService,
                                       CanchaRepository canchaRepo,
                                       DescuentoHorarioRepository descuentoRepo) {
        this.precioService  = precioService;
        this.canchaRepo     = canchaRepo;
        this.descuentoRepo  = descuentoRepo;
    }

    // GET /precios/descuentos-todos → tabla resumen para el admin
    @GetMapping("/descuentos-todos")
    public ResponseEntity<?> descuentosTodos() {
        List<Cancha> canchas = canchaRepo.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Cancha c : canchas) {
            List<Map<String, Object>> descuentos = descuentoRepo.findByCanchaId(c.getId()).stream()
                    .filter(dh -> dh.getDescuentoActual() > 0)
                    .map(dh -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("hora", dh.getHora().toString().substring(0, 5));
                        m.put("descuentoPorcentaje", (int) Math.round(dh.getDescuentoActual() * 100));
                        return m;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> item = new HashMap<>();
            item.put("canchaId",   c.getId());
            item.put("nombre",     c.getNombre());
            item.put("sede",       c.getSede().getNombre());
            item.put("tipo",       c.getTipo());
            item.put("precioBase", c.getPrecioBase());
            item.put("descuentos", descuentos);
            resultado.add(item);
        }

        return ResponseEntity.ok(resultado);
    }

    // POST /precios/recalcular → dispara recálculo manual
    @PostMapping("/recalcular")
    public ResponseEntity<?> recalcular() {
        precioService.recalcularTodas();
        return ResponseEntity.ok(Map.of("mensaje", "Recálculo completado."));
    }
}