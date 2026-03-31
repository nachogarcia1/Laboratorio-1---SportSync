package com.sportsync.backend.controller;

import com.sportsync.backend.model.Usuario;
import com.sportsync.backend.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Usuario> listar() {
        return service.listar();
    }

    @PostMapping("/create")
    public Usuario create(@RequestBody Usuario usuario) {
        return service.crearUsuario(usuario);
    }
}