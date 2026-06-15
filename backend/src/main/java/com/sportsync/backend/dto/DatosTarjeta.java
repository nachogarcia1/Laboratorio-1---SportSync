package com.sportsync.backend.dto;

import com.sportsync.backend.model.admin.MetodoPago;

/**
 * Datos de tarjeta ingresados por el usuario (entrada de la pasarela).
 * NO se persisten: se usan solo para validar y simular el cobro. El número
 * completo y el CVV nunca se guardan ni se loguean.
 */
public class DatosTarjeta {
    private String titular;
    private String numero;
    private String cvv;
    private String vencimiento; // "MM/AA" o "MM/AAAA"
    private MetodoPago tipo;     // TARJETA_CREDITO | TARJETA_DEBITO

    public DatosTarjeta() {}

    public String getTitular()     { return titular; }
    public String getNumero()      { return numero; }
    public String getCvv()         { return cvv; }
    public String getVencimiento() { return vencimiento; }
    public MetodoPago getTipo()    { return tipo; }

    public void setTitular(String titular)         { this.titular = titular; }
    public void setNumero(String numero)           { this.numero = numero; }
    public void setCvv(String cvv)                 { this.cvv = cvv; }
    public void setVencimiento(String vencimiento) { this.vencimiento = vencimiento; }
    public void setTipo(MetodoPago tipo)           { this.tipo = tipo; }
}
