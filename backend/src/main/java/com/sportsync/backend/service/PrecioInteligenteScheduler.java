package com.sportsync.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PrecioInteligenteScheduler {

    private final PrecioInteligenteService precioService;

    public PrecioInteligenteScheduler(PrecioInteligenteService precioService) {
        this.precioService = precioService;
    }

    // Todos los días a las 3:00 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void recalcularDescuentosDiario() {
        precioService.recalcularTodas();
    }
}