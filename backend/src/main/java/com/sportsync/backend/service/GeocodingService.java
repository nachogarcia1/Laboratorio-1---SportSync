package com.sportsync.backend.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
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

    /**
     * Fábrica de geometrías JTS con SRID 4326 (WGS-84, mismo que GPS/Google Maps).
     * En JTS: Coordinate(x, y) → x = longitud, y = latitud.
     */
    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
        // Nominatim requiere un User-Agent identificable
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("User-Agent", "SportsSync/1.0 (lab-project)");
            return execution.execute(request, body);
        });
    }

    public record Sugerencia(String displayName, double lat, double lng) {}

    /**
     * Devuelve hasta 5 sugerencias de Nominatim para el texto ingresado.
     * Usado por el autocomplete del modal de creación de sedes.
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
            if (results != null) {
                return results.stream()
                        .map(r -> new Sugerencia(
                                r.getOrDefault("display_name", "").toString(),
                                Double.parseDouble(r.get("lat").toString()),
                                Double.parseDouble(r.get("lon").toString())
                        ))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Error al buscar sugerencias para '" + texto + "': " + e.getMessage());
        }
        return List.of();
    }

    /**
     * Convierte una dirección en un {@link GeocodingResult} con el Point PostGIS
     * y la dirección canónica devuelta por Nominatim (display_name).
     * Retorna {@code null} si no se puede geocodificar.
     */
    public GeocodingResult geocodificar(String direccion) {
        try {
            String query = URLEncoder.encode(direccion + ", Argentina", StandardCharsets.UTF_8);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://nominatim.openstreetmap.org/search?q=" + query + "&format=json&limit=1",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> results = response.getBody();
            if (results != null && !results.isEmpty()) {
                Map<String, Object> first = results.get(0);
                double lat = Double.parseDouble(first.get("lat").toString());
                double lng = Double.parseDouble(first.get("lon").toString());
                String displayName = first.getOrDefault("display_name", direccion).toString();
                // JTS usa (x=longitud, y=latitud)
                Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
                return new GeocodingResult(point, displayName);
            }
        } catch (Exception e) {
            System.err.println("Error al geocodificar '" + direccion + "': " + e.getMessage());
        }
        return null;
    }

    /**
     * Convierte coordenadas GPS en una dirección legible usando Nominatim reverse geocoding.
     * Retorna {@code null} si no se puede obtener la dirección.
     */
    public String reverseGeocodificar(double lat, double lng) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat
                    + "&lon=" + lng + "&format=json";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> result = response.getBody();
            if (result != null && result.containsKey("display_name")) {
                return result.get("display_name").toString();
            }
        } catch (Exception e) {
            System.err.println("Error al reverse geocodificar (" + lat + "," + lng + "): " + e.getMessage());
        }
        return null;
    }
}
