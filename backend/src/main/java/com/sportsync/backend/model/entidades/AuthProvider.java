package com.sportsync.backend.model.entidades;

/**
 * Origen de autenticación de la cuenta.
 * LOCAL  = registro con email + contraseña (requiere verificación por código).
 * GOOGLE = alta vía Google OAuth (el email ya viene verificado por Google).
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
