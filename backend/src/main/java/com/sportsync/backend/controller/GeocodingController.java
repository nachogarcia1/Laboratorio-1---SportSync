package com.sportsync.backend.controller;

import com.sportsync.backend.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/geocoding")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    /**
     * Autocomplete: devuelve hasta 5 sugerencias para el texto ingresado.
     * GET /geocoding/search?q=Av+Corrientes
     * → [{ "displayName": "...", "lat": -34.6, "lng": -58.3 }, ...]
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam String q) {
        List<GeocodingService.Sugerencia> sugerencias = geocodingService.buscarSugerencias(q);
        List<Map<String, Object>> result = sugerencias.stream()
                .map(s -> Map.<String, Object>of(
                        "displayName", s.displayName(),
                        "lat", s.lat(),
                        "lng", s.lng()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Reverse geocoding: coordenadas → dirección.
     * GET /geocoding/reverse?lat=-34.60&lng=-58.38
     */
    @GetMapping("/reverse")
    public ResponseEntity<Map<String, String>> reverse(
            @RequestParam double lat,
            @RequestParam double lng) {
        String direccion = geocodingService.reverseGeocodificar(lat, lng);
        if (direccion == null) {
            return ResponseEntity.ok(Map.of("error", "No se pudo obtener la dirección."));
        }
        return ResponseEntity.ok(Map.of("direccion", direccion));
    }
}
