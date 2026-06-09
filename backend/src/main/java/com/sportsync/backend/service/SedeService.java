package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.sede.Sede;
import com.sportsync.backend.repository.SedeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SedeService {

    private final SedeRepository repo;
    private final GeocodingService geocodingService;

    public SedeService(SedeRepository repo, GeocodingService geocodingService) {
        this.repo = repo;
        this.geocodingService = geocodingService;
    }

    // ── Listado público (solo activas) ────────────────────────────────────────

    public List<Sede> listarActivas() {
        return repo.findByActivaTrue();
    }

    // ── Listado admin (todas) ─────────────────────────────────────────────────

    public List<Sede> listarTodas() {
        return repo.findAll();
    }

    // ── Detalle ───────────────────────────────────────────────────────────────

    public Sede obtenerPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Sede no encontrada."));
    }

    // ── Crear ─────────────────────────────────────────────────────────────────

    public Sede crear(Sede sede) {
        sede.setActiva(false);
        geocodificarSiEsNecesario(sede);
        return repo.save(sede);
    }

    // ── Editar ────────────────────────────────────────────────────────────────

    public Sede editar(Long id, String nuevoNombre, String nuevaDireccion, String nuevaHoraApertura, String nuevaHoraCierre) {
        Sede sede = obtenerPorId(id);

        if (nuevoNombre != null && !nuevoNombre.isBlank()) {
            sede.setNombre(nuevoNombre);
        }
        if (nuevaDireccion != null && !nuevaDireccion.isBlank()) {
            sede.setDireccion(nuevaDireccion);
            // Forzar re-geocodificación si cambia la dirección
            sede.setUbicacion(null);
        }
        if (nuevaHoraApertura != null && !nuevaHoraApertura.isBlank()) {
            sede.setHoraApertura(nuevaHoraApertura);
        }
        if (nuevaHoraCierre != null && !nuevaHoraCierre.isBlank()) {
            sede.setHoraCierre(nuevaHoraCierre);
        }

        geocodificarSiEsNecesario(sede);
        return repo.save(sede);
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    public void eliminar(Long id) {
        if (!repo.existsById(id)) {
            throw new UsuarioNoEncontradoException("Sede no encontrada.");
        }
        repo.deleteById(id);
    }

    // ── Habilitar / deshabilitar ──────────────────────────────────────────────

    public Sede toggleActiva(Long id) {
        Sede sede = obtenerPorId(id);
        sede.setActiva(!sede.isActiva());
        return repo.save(sede);
    }

    // ── Geocodificar todas las sedes sin ubicación (batch) ────────────────────

    public record ResultadoGeocodificacion(int procesadas, int exitosas, int fallidas) {}

    public ResultadoGeocodificacion geocodificarTodas() {
        List<Sede> sinUbicacion = repo.findByUbicacionIsNull();
        int exitosas = 0, fallidas = 0;

        for (Sede sede : sinUbicacion) {
            if (sede.getDireccion() == null || sede.getDireccion().isBlank()) {
                fallidas++;
                continue;
            }
            GeocodingResult result = geocodingService.geocodificar(sede.getDireccion());
            if (result != null) {
                sede.setUbicacion(result.ubicacion());
                sede.setDireccion(result.direccionCanonica());
                repo.save(sede);
                exitosas++;
            } else {
                fallidas++;
            }
            // Nominatim exige máximo 1 solicitud por segundo
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
        }
        return new ResultadoGeocodificacion(sinUbicacion.size(), exitosas, fallidas);
    }

    // ── Geocodificar si no tiene coordenadas ──────────────────────────────────

    private void geocodificarSiEsNecesario(Sede sede) {
        if (sede.getUbicacion() == null && sede.getDireccion() != null) {
            GeocodingResult result = geocodingService.geocodificar(sede.getDireccion());
            if (result != null) {
                sede.setUbicacion(result.ubicacion());
                // Normalizar la dirección con el nombre canónico de Nominatim
                // para garantizar consistencia entre la dirección guardada y las coordenadas
                sede.setDireccion(result.direccionCanonica());
            }
        }
    }
}
