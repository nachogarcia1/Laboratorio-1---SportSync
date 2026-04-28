package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.sede.Sede;
import com.sportsync.backend.repository.SedeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SedeService {

    private final SedeRepository repo;

    public SedeService(SedeRepository repo) {
        this.repo = repo;
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
        }
        if (nuevaHoraApertura != null && !nuevaHoraApertura.isBlank()) {
            sede.setHoraApertura(nuevaHoraApertura);
        }
        if (nuevaHoraCierre != null && !nuevaHoraCierre.isBlank()) {
            sede.setHoraCierre(nuevaHoraCierre);
        }

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
}