package com.sportsync.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expone un Clock como bean para poder inyectarlo donde se necesite la fecha/hora
 * actual (p.ej. validación de vencimiento de tarjeta) y reemplazarlo por uno fijo
 * en los tests.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
