package com.sportsync.backend.service;

import com.sportsync.backend.exception.UsuarioNoEncontradoException;
import com.sportsync.backend.model.cancha.ItemEquipamiento;
import com.sportsync.backend.repository.ItemEquipamientoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemEquipamientoService {

    private final ItemEquipamientoRepository repo;

    public ItemEquipamientoService(ItemEquipamientoRepository repo) {
        this.repo = repo;
    }

    public List<ItemEquipamiento> listarDisponibles() {
        return repo.findByDisponibleTrue();
    }

    public List<ItemEquipamiento> listarTodos() {
        return repo.findAll();
    }

    public ItemEquipamiento crear(ItemEquipamiento item) {
        return repo.save(item);
    }

    public ItemEquipamiento editar(Long id, String nombre, Double precio) {
        ItemEquipamiento item = repo.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Item no encontrado."));
        if (nombre != null && !nombre.isBlank()) item.setNombre(nombre);
        if (precio != null && precio > 0) item.setPrecioPorUnidad(precio);
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