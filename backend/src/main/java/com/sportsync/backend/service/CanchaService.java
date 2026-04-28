package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.admin.EstadoCancha;
import com.sportsync.backend.model.sede.Sede;
import com.sportsync.backend.repository.CanchaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CanchaService {

    private final CanchaRepository repo;
    private final SedeService sedeService;

    public CanchaService(CanchaRepository repo, SedeService sedeService) {
        this.repo = repo;
        this.sedeService = sedeService;
    }

    // ── Listado por sede ──────────────────────────────────────────────────────

    public List<Cancha> listarPorSede(Long sedeId) {
        return repo.findBySedeId(sedeId);
    }

    public List<Cancha> listarHabilitadasPorSede(Long sedeId) {
        return repo.findBySedeIdAndEstado(sedeId, EstadoCancha.HABILITADA);
    }

    // ── Filtros públicos (UC-21) ──────────────────────────────────────────────

    public List<Cancha> filtrar(Long sedeId, Integer tipo) {
        if (sedeId != null && tipo != null) {
            return repo.findBySedeIdAndTipoAndEstado(sedeId, tipo, EstadoCancha.HABILITADA);
        }
        if (sedeId != null) {
            return repo.findBySedeIdAndEstado(sedeId, EstadoCancha.HABILITADA);
        }
        if (tipo != null) {
            validarTipo(tipo);
            return repo.findByTipo(tipo);
        }
        return repo.findAll();
    }

    // ── Detalle ───────────────────────────────────────────────────────────────

    public Cancha obtenerPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Cancha no encontrada."));
    }

    // ── Crear ─────────────────────────────────────────────────────────────────

    public Cancha crear(Long sedeId, Cancha cancha) {
        validarTipo(cancha.getTipo());

        Sede sede = sedeService.obtenerPorId(sedeId);
        cancha.setSede(sede);
        cancha.setEstado(EstadoCancha.NO_HABILITADA);

        return repo.save(cancha);
    }

    // ── Editar ────────────────────────────────────────────────────────────────

    public Cancha editar(Long id, String nuevoNombre, Integer nuevoTipo, Double nuevoPrecio) {
        Cancha cancha = obtenerPorId(id);

        if (nuevoNombre != null && !nuevoNombre.isBlank()) {
            cancha.setNombre(nuevoNombre);
        }
        if (nuevoTipo != null) {
            validarTipo(nuevoTipo);
            cancha.setTipo(nuevoTipo);
        }
        if (nuevoPrecio != null && nuevoPrecio > 0) {
            cancha.setPrecioBase(nuevoPrecio);
        }

        return repo.save(cancha);
    }

    // ── Cambiar estado ────────────────────────────────────────────────────────

    public Cancha cambiarEstado(Long id, EstadoCancha nuevoEstado) {
        Cancha cancha = obtenerPorId(id);
        cancha.setEstado(nuevoEstado);
        return repo.save(cancha);
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    public void eliminar(Long id) {
        if (!repo.existsById(id)) {
            throw new UsuarioNoEncontradoException("Cancha no encontrada.");
        }
        repo.deleteById(id);
    }

    // ── Validación tipo ───────────────────────────────────────────────────────

    private void validarTipo(int tipo) {
        if (tipo != 5 && tipo != 7 && tipo != 11) {
            throw new IllegalArgumentException("Tipo de cancha inválido. Debe ser 5, 7 u 11.");
        }
    }
}