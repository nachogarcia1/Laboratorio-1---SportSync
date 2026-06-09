package com.sportsync.backend.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate;

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("User-Agent", "SportsSync/1.0 (lab-project)");
            return execution.execute(request, body);
        });
    }

    public record Sugerencia(String displayName, double lat, double lng) {}

    /**
     * Devuelve hasta 5 sugerencias de Nominatim para el texto ingresado.
     */
    public List<Sugerencia> buscarSugerencias(String texto) {
        try {
            String query = URLEncoder.encode(texto + ", Argentina", StandardCharsets.UTF_8);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://nominatim.openstreetmap.org/search?q=" + query + "&format=json&limit=5&addressdetails=0",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> results = response.getBody();
            if (results == null) return List.of();
            return results.stream()
                    .map(r -> new Sugerencia(
                            r.get("display_name").toString(),
                            Double.parseDouble(r.get("lat").toString()),
                            Double.parseDouble(r.get("lon").toString())
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error buscando sugerencias para '" + texto + "': " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Reverse geocoding: coordenadas → dirección legible.
     */
    public String reverseGeocodificar(double lat, double lng) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat + "&lon=" + lng + "&format=json";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> result = response.getBody();
            if (result != null && result.containsKey("display_name")) {
                return result.get("display_name").toString();
            }
        } catch (Exception e) {
            System.err.println("Error en reverse geocoding (" + lat + "," + lng + "): " + e.getMessage());
        }
        return null;
    }
}
