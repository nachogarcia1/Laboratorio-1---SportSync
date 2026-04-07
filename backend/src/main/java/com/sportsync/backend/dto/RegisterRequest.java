package com.sportsync.backend.dto;

public class RegisterRequest {

    private String nombre;
    private String email;
    private String password;
    private String dni;
    private String telefono; // opcional

    public RegisterRequest() {}

    public String getNombre()    { return nombre; }
    public String getEmail()     { return email; }
    public String getPassword()  { return password; }
    public String getDni()       { return dni; }
    public String getTelefono()  { return telefono; }

    public void setNombre(String nombre)       { this.nombre = nombre; }
    public void setEmail(String email)         { this.email = email; }
    public void setPassword(String password)   { this.password = password; }
    public void setDni(String dni)             { this.dni = dni; }
    public void setTelefono(String telefono)   { this.telefono = telefono; }
}