package com.sportsync.backend.model.reserva;

import com.sportsync.backend.model.admin.EstadoReserva;
import com.sportsync.backend.model.cancha.Cancha;
import com.sportsync.backend.model.entidades.Usuario;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.ACTIVA;

    @Column(nullable = false)
    private double precioTotal;

    @Column(nullable = false)
    private boolean iluminacion = false;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservaEquipamiento> equipamiento;

    public Reserva() {}

    public Long getId()                              { return id; }
    public Usuario getUsuario()                      { return usuario; }
    public Cancha getCancha()                        { return cancha; }
    public LocalDate getFecha()                      { return fecha; }
    public LocalTime getHoraInicio()                 { return horaInicio; }
    public LocalTime getHoraFin()                    { return horaFin; }
    public EstadoReserva getEstado()                 { return estado; }
    public double getPrecioTotal()                   { return precioTotal; }
    public boolean isIluminacion()                   { return iluminacion; }
    public List<ReservaEquipamiento> getEquipamiento() { return equipamiento; }

    public void setUsuario(Usuario usuario)          { this.usuario = usuario; }
    public void setCancha(Cancha cancha)             { this.cancha = cancha; }
    public void setFecha(LocalDate fecha)            { this.fecha = fecha; }
    public void setHoraInicio(LocalTime horaInicio)  { this.horaInicio = horaInicio; }
    public void setHoraFin(LocalTime horaFin)        { this.horaFin = horaFin; }
    public void setEstado(EstadoReserva estado)      { this.estado = estado; }
    public void setPrecioTotal(double precioTotal)   { this.precioTotal = precioTotal; }
    public void setIluminacion(boolean iluminacion)  { this.iluminacion = iluminacion; }
}