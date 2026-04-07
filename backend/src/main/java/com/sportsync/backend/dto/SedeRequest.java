package com.sportsync.backend.dto;

public class SedeRequest {

    private String nombre;
    private String direccion;
    private String horarios;

    public SedeRequest(){}

    public String getNombre()     { return nombre; }
    public String getDireccion()  { return direccion; }
    public String getHorarios()   { return horarios; }

    public void setNombre(String nombre)       { this.nombre = nombre; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public void setHorarios(String horarios)   { this.horarios = horarios; }




}
