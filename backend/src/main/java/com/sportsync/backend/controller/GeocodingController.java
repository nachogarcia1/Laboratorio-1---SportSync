package com.sportsync.backend.controller;

import com.sportsync.backend.service.GeocodingResult;
import com.sportsync.backend.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/geocoding")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    /**
     * Reverse geocoding: convierte coordenadas GPS en una dirección legible.
     * Usado por el mapa picker del panel admin para sincronizar dirección ↔ ubicación.
     *
     * GET /geocoding/reverse?lat=-34.60&lng=-58.38
     * → { "direccion": "Av. Corrientes 1234, Buenos Aires, Argentina" }
     */
    /**
     * Reverse geocoding: convierte coordenadas GPS en una dirección legible.
     * GET /geocoding/reverse?lat=-34.60&lng=-58.38
     * → { "direccion": "Av. Corrientes 1234, Buenos Aires, Argentina" }
     */
    @GetMapping("/reverse")
    public ResponseEntity<Map<String, String>> reverse(
            @RequestParam double lat,
            @RequestParam double lng) {

        String direccion = geocodingService.reverseGeocodificar(lat, lng);
        if (direccion == null) {
            return ResponseEntity.ok(Map.of("error", "No se pudo obtener la dirección para esas coordenadas."));
        }
        return ResponseEntity.ok(Map.of("direccion", direccion));
    }

    /**
     * Forward geocoding: convierte una dirección de texto en coordenadas GPS.
     * GET /geocoding/forward?q=Av.+Corrientes+1234+Buenos+Aires
     * → { "lat": -34.60, "lng": -58.38 }  |  { "error": "..." }
     */
    @GetMapping("/forward")
    public ResponseEntity<Map<String, Object>> forward(@RequestParam String q) {
        GeocodingResult result = geocodingService.geocodificar(q);
        if (result == null) {
            return ResponseEntity.ok(Map.of("error", "No se encontraron resultados para esa dirección."));
        }
        return ResponseEntity.ok(Map.of(
                "lat", result.ubicacion().getY(),   // JTS: Y = latitud
                "lng", result.ubicacion().getX()    // JTS: X = longitud
        ));
    }
}
