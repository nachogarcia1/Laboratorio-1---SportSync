package com.sportsync.backend.service;

import com.sportsync.backend.dto.DatosTarjeta;
import com.sportsync.backend.dto.ResultadoPago;
import com.sportsync.backend.dto.SolicitudPago;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Pasarela de pagos SIMULADA (no se conecta a ningún proveedor real).
 * Resultado determinista y testeable:
 *  - datos inválidos        → DATOS_INVALIDOS
 *  - tarjeta "mágica" de rechazo → RECHAZADO
 *  - resto de tarjetas válidas   → APROBADO
 * Nunca expone el número completo ni el CVV.
 */
@Service
public class PasarelaPagoMock implements PasarelaPago {

    /** Tarjeta de prueba que siempre se rechaza (para demostrar el flujo de rechazo). */
    static final String TARJETA_RECHAZADA = "4000000000000002";

    private final ValidadorTarjeta validador;

    public PasarelaPagoMock(ValidadorTarjeta validador) {
        this.validador = validador;
    }

    @Override
    public ResultadoPago cobrar(SolicitudPago solicitud) {
        if (solicitud == null || solicitud.getTarjeta() == null) {
            throw new IllegalArgumentException("Solicitud de pago inválida.");
        }
        DatosTarjeta tarjeta = solicitud.getTarjeta();

        Optional<String> error = validador.validar(tarjeta);
        if (error.isPresent()) {
            return ResultadoPago.datosInvalidos(error.get());
        }

        String numero = validador.normalizarNumero(tarjeta.getNumero());
        String ultimos4 = numero.substring(numero.length() - 4);

        if (TARJETA_RECHAZADA.equals(numero)) {
            return ResultadoPago.rechazado(ultimos4, "El pago fue rechazado por el emisor de la tarjeta.");
        }

        String transaccionId = "MOCK-" + UUID.randomUUID();
        return ResultadoPago.aprobado(transaccionId, ultimos4, "Pago aprobado.");
    }
}
