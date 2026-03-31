package com.sportsync.backend.service;

import com.sportsync.backend.model.Usuario;
import com.sportsync.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;

    public UsuarioService(UsuarioRepository repo) {
        this.repo = repo;
    }

    public List<Usuario> listar() {
        return repo.findAll();
    }
    public Usuario crearUsuario(Usuario usuario) {
        return repo.save(usuario);
    }
}