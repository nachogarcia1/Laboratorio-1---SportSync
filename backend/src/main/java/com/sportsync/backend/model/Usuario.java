package com.sportsync.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String nombre; //usuario se define por nombre y mail

    @Column
    private String email;

    // 🔹 Constructor vacío (OBLIGATORIO para JPA)
    public Usuario() {
    }


    // 🔹 GETTERS

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    // 🔹 SETTERS (MUY IMPORTANTES)

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}