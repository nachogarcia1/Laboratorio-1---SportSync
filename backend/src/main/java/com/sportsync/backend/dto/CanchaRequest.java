package com.sportsync.backend.dto;

public class CanchaRequest {
    private String nombre;
    private int tipo;
    private double precioBase;

    public CanchaRequest() {}

    public String getNombre()      { return nombre; }
    public int getTipo()           { return tipo; }
    public double getPrecioBase()  { return precioBase; }

    public void setNombre(String nombre)         { this.nombre = nombre; }
    public void setTipo(int tipo)                { this.tipo = tipo; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
}