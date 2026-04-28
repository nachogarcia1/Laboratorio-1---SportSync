package com.sportsync.backend.dto;

public class SedeRequest {

    private String nombre;
    private String direccion;
    private String horaApertura;
    private String horaCierre;

    public SedeRequest(){}

    public String getNombre()        { return nombre; }
    public String getDireccion()     { return direccion; }
    public String getHoraApertura()  { return horaApertura; }
    public String getHoraCierre()    { return horaCierre; }

    public void setNombre(String nombre)              { this.nombre = nombre; }
    public void setDireccion(String direccion)        { this.direccion = direccion; }
    public void setHoraApertura(String horaApertura)  { this.horaApertura = horaApertura; }
    public void setHoraCierre(String horaCierre)      { this.horaCierre = horaCierre; }




}
