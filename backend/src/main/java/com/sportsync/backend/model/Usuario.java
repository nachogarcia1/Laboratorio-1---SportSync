package com.sportsync.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String dni;

    @Column
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.NO_SOCIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30) DEFAULT 'ACTIVO'")
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private boolean activo = true;

    public Usuario() {}

    public Long getId()           { return id; }
    public String getNombre()     { return nombre; }
    public String getEmail()      { return email; }
    public String getPassword()   { return password; }
    public String getDni()        { return dni; }
    public String getTelefono()   { return telefono; }
    public Rol getRol()           { return rol; }
    public EstadoUsuario getEstado() { return estado; }

    public void setNombre(String nombre)          { this.nombre = nombre; }
    public void setEmail(String email)            { this.email = email; }
    public void setPassword(String password)      { this.password = password; }
    public void setDni(String dni)                { this.dni = dni; }
    public void setTelefono(String telefono)      { this.telefono = telefono; }
    public void setRol(Rol rol)                   { this.rol = rol; }
    public void setEstado(EstadoUsuario estado)   { this.estado = estado; }
}
