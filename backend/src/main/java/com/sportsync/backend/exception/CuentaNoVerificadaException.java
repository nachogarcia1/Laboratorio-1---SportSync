package com.sportsync.backend.exception;

/**
 * Se lanza al intentar iniciar sesión con una cuenta cuyo email aún no fue
 * verificado. El controller la mapea a 403 con un flag para que el frontend
 * ofrezca reenviar el código.
 */
public class CuentaNoVerificadaException extends RuntimeException {
    public CuentaNoVerificadaException(String mensaje) {
        super(mensaje);
    }
}
