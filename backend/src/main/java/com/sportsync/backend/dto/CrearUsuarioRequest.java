package com.sportsync.backend.dto;


public class CrearUsuarioRequest {
    private String nombre;
    private String email;

    public CrearUsuarioRequest() {
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}