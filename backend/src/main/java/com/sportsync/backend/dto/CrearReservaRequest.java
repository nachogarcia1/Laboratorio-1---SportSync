package com.sportsync.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CrearReservaRequest {

    private Long usuarioId;
    private Long canchaId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean iluminacion;
    private List<ItemCantidad> equipamiento;

    public static class ItemCantidad {
        private Long itemId;
        private int cantidad;

        public Long getItemId()      { return itemId; }
        public int getCantidad()     { return cantidad; }
        public void setItemId(Long itemId)    { this.itemId = itemId; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    }

    public Long getUsuarioId()                        { return usuarioId; }
    public Long getCanchaId()                         { return canchaId; }
    public LocalDate getFecha()                       { return fecha; }
    public LocalTime getHoraInicio()                  { return horaInicio; }
    public LocalTime getHoraFin()                     { return horaFin; }
    public boolean isIluminacion()                    { return iluminacion; }
    public List<ItemCantidad> getEquipamiento()       { return equipamiento; }

    public void setUsuarioId(Long usuarioId)          { this.usuarioId = usuarioId; }
    public void setCanchaId(Long canchaId)            { this.canchaId = canchaId; }
    public void setFecha(LocalDate fecha)             { this.fecha = fecha; }
    public void setHoraInicio(LocalTime horaInicio)   { this.horaInicio = horaInicio; }
    public void setHoraFin(LocalTime horaFin)         { this.horaFin = horaFin; }
    public void setIluminacion(boolean iluminacion)   { this.iluminacion = iluminacion; }
    public void setEquipamiento(List<ItemCantidad> equipamiento) { this.equipamiento = equipamiento; }
}