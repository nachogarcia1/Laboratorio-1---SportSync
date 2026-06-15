package com.sportsync.backend.service;

import com.sportsync.backend.dto.ResultadoPago;
import com.sportsync.backend.dto.SolicitudPago;

/**
 * Abstracción de una pasarela de pagos con tarjeta. La lógica de negocio depende
 * de esta interfaz, no de una implementación concreta, para poder reemplazar el
 * mock por una pasarela real en el futuro.
 */
public interface PasarelaPago {

    /** Procesa un cobro y devuelve el resultado (aprobado / rechazado / datos inválidos). */
    ResultadoPago cobrar(SolicitudPago solicitud);
}
