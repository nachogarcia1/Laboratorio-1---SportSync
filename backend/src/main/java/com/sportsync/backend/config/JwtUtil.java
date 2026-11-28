package com.sportsync.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final long EXPIRACION_MS = 1000L * 60 * 60 * 24;

    /** HS256 exige una clave de al menos 256 bits (32 bytes). */
    static final int MIN_BYTES_SECRETO = 32;

    private final Key key;

    /**
     * El secreto viene de la propiedad {@code sportsync.jwt.secret} (variable {@code JWT_SECRET}).
     * No tiene valor por defecto: si falta o es débil, la app NO arranca. Solo el perfil {@code dev}
     * define uno de prueba (application-dev.properties).
     */
    public JwtUtil(@Value("${sportsync.jwt.secret}") String secreto) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definido. Definilo en el entorno o en backend/.env "
                            + "(o activá el perfil 'dev' solo para desarrollo local).");
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_BYTES_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corto (" + bytes.length + " bytes). Debe tener al menos "
                            + MIN_BYTES_SECRETO + ". Generá uno con: openssl rand -base64 48");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generarToken(String email, String rol) {
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRACION_MS))
                .signWith(key)
                .compact();
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    public boolean esValido(String token) {
        try {
            extraerClaims(token);
            return !estaExpirado(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean estaExpirado(String token) {
        return extraerClaims(token).getExpiration().before(new Date());
    }
}