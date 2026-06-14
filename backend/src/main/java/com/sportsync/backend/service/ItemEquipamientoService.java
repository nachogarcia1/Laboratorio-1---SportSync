package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.cancha.ItemEquipamiento;
import com.sportsync.backend.repository.ItemEquipamientoRepository;
import com.sportsync.backend.repository.SedeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemEquipamientoService {

    private final ItemEquipamientoRepository repo;
    private final SedeRepository sedeRepo;

    public ItemEquipamientoService(ItemEquipamientoRepository repo, SedeRepository sedeRepo) {
        this.repo = repo;
        this.sedeRepo = sedeRepo;
    }

    public List<ItemEquipamiento> listarDisponibles() {
        return repo.findByDisponibleTrue();
    }

    /** Extras ofrecidos en una sede: los propios + los globales. */
    public List<ItemEquipamiento> listarParaSede(Long sedeId) {
        return repo.findDisponiblesParaSede(sedeId);
    }

    public List<ItemEquipamiento> listarTodos() {
        return repo.findAll();
    }

    public ItemEquipamiento crear(String nombre, double precio, int stock, boolean disponible, Long sedeId) {
        ItemEquipamiento item = new ItemEquipamiento();
        item.setNombre(nombre);
        item.setPrecioPorUnidad(precio);
        item.setStock(stock);
        item.setDisponible(disponible);
        if (sedeId != null) {
            item.setSede(sedeRepo.findById(sedeId)
                    .orElseThrow(() -> new UsuarioNoEncontradoException("Sede no encontrada.")));
        }
        return repo.save(item);
    }

    public ItemEquipamiento editar(Long id, String nombre, Double precio, Integer stock) {
        ItemEquipamiento item = repo.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Item no encontrado."));
        if (nombre != null && !nombre.isBlank()) item.setNombre(nombre);
        if (precio != null && precio > 0)        item.setPrecioPorUnidad(precio);
        if (stock != null && stock >= 0)         item.setStock(stock);
        return repo.save(item);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public ItemEquipamiento toggleDisponible(Long id) {
        ItemEquipamiento item = repo.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Item no encontrado."));
        item.setDisponible(!item.isDisponible());
        return repo.save(item);
    }
}
