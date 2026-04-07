package com.sportsync.backend.model;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Setters
    @Setter
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String dni;

    @Setter
    @Column
    private String telefono;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.NO_SOCIO;

    @Setter
    @Column(nullable = false)
    private boolean activo = true;

    // Constructor vacío (obligatorio para JPA)
    public Usuario() {}

    // Getters
    public Long getId()         { return id; }
    public String getNombre()   { return nombre; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public String getDni()      { return dni; }
    public String getTelefono() { return telefono; }
    public Rol getRol()         { return rol; }
    public boolean isActivo()   { return activo; }

}