package com.sportsync.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Valida el id_token de Google contra el endpoint público `tokeninfo`.
 * No requiere librerías extra: Google verifica firma y expiración del lado del
 * servidor, y nosotros chequeamos que el `aud` coincida con nuestro Client ID.
 */
@Component
public class GoogleTokenVerifier {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sportsync.oauth.google.client-id:}")
    private String clientId;

    public record GoogleUser(String email, String nombre, boolean emailVerified) {}

    public GoogleUser verificar(String idToken) {
        if (clientId == null || clientId.isBlank())
            throw new IllegalStateException("Login con Google no configurado (falta GOOGLE_OAUTH_CLIENT_ID).");
        if (idToken == null || idToken.isBlank())
            throw new IllegalArgumentException("Falta el token de Google.");

        Map<String, Object> resp;
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            resp = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token de Google inválido o expirado.");
        }
        if (resp == null)
            throw new IllegalArgumentException("Token de Google inválido.");

        // El token debe haber sido emitido para NUESTRA aplicación
        if (!clientId.equals(resp.get("aud")))
            throw new IllegalArgumentException("El token no corresponde a esta aplicación.");

        String email = (String) resp.get("email");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("El token de Google no contiene email.");

        boolean emailVerified = "true".equalsIgnoreCase(String.valueOf(resp.get("email_verified")));
        String nombre = (String) resp.getOrDefault("name", email);
        return new GoogleUser(email, nombre, emailVerified);
    }
}
