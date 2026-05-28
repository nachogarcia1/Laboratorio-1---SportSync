package com.sportsync.backend.service;

import org.locationtech.jts.geom.Point;

/**
 * Resultado de geocodificación: coordenadas PostGIS + dirección canónica de Nominatim.
 * La dirección canónica es la que Nominatim devuelve como display_name,
 * que garantiza consistencia entre lo almacenado y las coordenadas reales.
 */
public record GeocodingResult(Point ubicacion, String direccionCanonica) {}
