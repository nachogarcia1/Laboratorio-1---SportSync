package com.sportsync.backend.service;

import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.reserva.DescuentoHorario;
import com.sportsync.backend.repository.CanchaRepository;
import com.sportsync.backend.repository.DescuentoHorarioRepository;
import com.sportsync.backend.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class PrecioInteligenteService {

    private final ReservaRepository    reservaRepo;
    private final CanchaRepository     canchaRepo;
    private final DescuentoHorarioRepository descuentoRepo;

    @Value("${sportsync.precios.min-reservas:5}")
    private int minReservas;

    @Value("${sportsync.precios.ventana-semanas:4}")
    private int ventanaSemanas;

    @Value("${sportsync.precios.umbral:0.75}")
    private double umbral;

    @Value("${sportsync.precios.max-descuento:0.30}")
    private double maxDescuento;

    @Value("${sportsync.precios.paso-ajuste:0.02}")
    private double pasoAjuste;

    @Value("${sportsync.precios.hora-apertura:08:00}")
    private String horaApertura;

    @Value("${sportsync.precios.hora-cierre:22:00}")
    private String horaCierre;

    @Value("${sportsync.precios.duracion-slot:60}")
    private int duracionSlot;

    @Value("${sportsync.precios.max-fraccion-descuento:0.5}")
    private double maxFraccionDescuento;

    public PrecioInteligenteService(ReservaRepository reservaRepo,
                                    CanchaRepository canchaRepo,
                                    DescuentoHorarioRepository descuentoRepo) {
        this.reservaRepo   = reservaRepo;
        this.canchaRepo    = canchaRepo;
        this.descuentoRepo = descuentoRepo;
    }

    // ── Consulta puntual: descuento activo para una cancha + hora ────────────
    public double obtenerDescuento(Long canchaId, LocalTime hora, LocalDate fecha) {
        // El descuento solo aplica si la reserva es de hoy hasta 5 días adelante
        if (fecha != null && fecha.isAfter(LocalDate.now().plusDays(5))) {
            return 0.0;
        }
        return descuentoRepo.findByCanchaIdAndHora(canchaId, hora)
                .map(DescuentoHorario::getDescuentoActual)
                .orElse(0.0);
    }

    // Overload sin fecha (para compatibilidad con llamadas existentes)
    public double obtenerDescuento(Long canchaId, LocalTime hora) {
        return obtenerDescuento(canchaId, hora, null);
    }

    // ── Genera todos los slots posibles del rango configurado ────────────────
    private List<LocalTime> generarSlots() {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime actual = LocalTime.parse(horaApertura);
        LocalTime cierre = LocalTime.parse(horaCierre);
        while (actual.isBefore(cierre)) {
            slots.add(actual);
            actual = actual.plusMinutes(duracionSlot);
        }
        return slots;
    }

    // ── Recalcula descuentos de UNA cancha ───────────────────────────────────
    @Transactional
    public void recalcularCancha(Long canchaId) {
        Cancha cancha = canchaRepo.findById(canchaId).orElse(null);
        if (cancha == null) return;

        LocalDate desde = LocalDate.now().minusWeeks(ventanaSemanas);
        List<Object[]> conteos = reservaRepo.contarReservasPorSlot(canchaId, desde);

        // Sin suficiente data histórica → no tocar nada
        long totalReservas = conteos.stream()
                .mapToLong(r -> (Long) r[1])
                .sum();
        if (totalReservas < minReservas) return;

        // Mapear historial: hora → cantidad de reservas
        Map<LocalTime, Long> conteoPorSlot = new HashMap<>();
        for (Object[] fila : conteos) {
            conteoPorSlot.put((LocalTime) fila[0], (Long) fila[1]);
        }

        // Rellenar con 0 los slots que nunca fueron reservados
        for (LocalTime slot : generarSlots()) {
            conteoPorSlot.putIfAbsent(slot, 0L);
        }

        // Pico: el slot con más reservas
        double peakCount = conteoPorSlot.values().stream()
                .mapToLong(v -> v)
                .average()
                .orElse(1.0);

        // Ordenar slots de menor a mayor demanda
        List<Map.Entry<LocalTime, Long>> slotsOrdenados = new ArrayList<>(conteoPorSlot.entrySet());
        slotsOrdenados.sort(Map.Entry.comparingByValue());

        // Solo la mitad inferior es elegible para descuento
        int maxConDescuento = (int) Math.floor(slotsOrdenados.size() * maxFraccionDescuento);
        Set<LocalTime> elegibles = new HashSet<>();
        for (int i = 0; i < maxConDescuento; i++) {
            elegibles.add(slotsOrdenados.get(i).getKey());
        }

        // Aplicar algoritmo a TODOS los slots del rango
        for (Map.Entry<LocalTime, Long> entry : conteoPorSlot.entrySet()) {
            LocalTime hora  = entry.getKey();
            long      count = entry.getValue();

            double demandRatio = (double) count / peakCount;

            double targetDescuento;
            if (!elegibles.contains(hora)) {
                targetDescuento = 0.0;  // top 50% → sin descuento siempre
            } else if (demandRatio >= umbral) {
                targetDescuento = 0.0;
            } else {
                targetDescuento = ((umbral - demandRatio) / umbral) * maxDescuento;
            }

            DescuentoHorario dh = descuentoRepo
                    .findByCanchaIdAndHora(canchaId, hora)
                    .orElseGet(() -> {
                        DescuentoHorario nuevo = new DescuentoHorario();
                        nuevo.setCancha(cancha);
                        nuevo.setHora(hora);
                        nuevo.setDescuentoActual(0.0);
                        return nuevo;
                    });

            double actual     = dh.getDescuentoActual();
            double diferencia = targetDescuento - actual;
            double paso       = Math.max(-pasoAjuste, Math.min(pasoAjuste, diferencia));
            double nuevo      = Math.max(0.0, Math.min(maxDescuento, actual + paso));

            dh.setDescuentoActual(nuevo);
            dh.setUpdatedAt(LocalDateTime.now());
            descuentoRepo.save(dh);
        }
    }

    // ── Recalcula TODAS las canchas (job nocturno) ───────────────────────────
    @Transactional
    public void recalcularTodas() {
        List<Cancha> canchas = canchaRepo.findAll();
        for (Cancha c : canchas) {
            recalcularCancha(c.getId());
        }
    }

    public List<Map<String, Object>> obtenerDescuentosPorCancha(Long canchaId) {
        return descuentoRepo.findByCanchaId(canchaId).stream()
                .filter(dh -> dh.getDescuentoActual() > 0)
                .map(dh -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("hora", dh.getHora().toString().substring(0, 5)); // "HH:mm"
                    m.put("descuentoPorcentaje", (int) Math.round(dh.getDescuentoActual() * 100));
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}