package com.sportsync.backend.model.cancha;

import com.sportsync.backend.model.admin.EstadoCancha;
import com.sportsync.backend.model.sede.Sede;
import jakarta.persistence.*;

@Entity
@Table(name = "cancha")
public class Cancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    // 5, 7 u 11
    @Column(nullable = false)
    private int tipo;

    @Column(nullable = false)
    private double precioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCancha estado = EstadoCancha.HABILITADA;

    // ── Horario propio de la cancha (independiente del de la sede) ──
    @Column(nullable = false)
    private String horaApertura = "08:00";

    @Column(nullable = false)
    private String horaCierre = "22:00";

    /** Duración (minutos) de cada turno / intervalo entre horarios. */
    @Column(nullable = false)
    private int duracionTurnoMin = 60;

    /** Días de la semana disponibles, ISO (1=Lun … 7=Dom), CSV. Ej: "1,2,3,4,5,6,7". */
    @Column(nullable = false)
    private String diasSemana = "1,2,3,4,5,6,7";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    // Constructor vacío (obligatorio para JPA)
    public Cancha() {}

    // Getters
    public Long getId()              { return id; }
    public String getNombre()        { return nombre; }
    public int getTipo()             { return tipo; }
    public double getPrecioBase()    { return precioBase; }
    public EstadoCancha getEstado()  { return estado; }
    public Sede getSede()            { return sede; }
    public String getHoraApertura()  { return horaApertura; }
    public String getHoraCierre()    { return horaCierre; }
    public int getDuracionTurnoMin() { return duracionTurnoMin; }
    public String getDiasSemana()    { return diasSemana; }

    // Setters
    public void setNombre(String nombre)          { this.nombre = nombre; }
    public void setTipo(int tipo)                 { this.tipo = tipo; }
    public void setPrecioBase(double precioBase)  { this.precioBase = precioBase; }
    public void setEstado(EstadoCancha estado)     { this.estado = estado; }
    public void setSede(Sede sede)                { this.sede = sede; }
    public void setHoraApertura(String horaApertura) { this.horaApertura = horaApertura; }
    public void setHoraCierre(String horaCierre)     { this.horaCierre = horaCierre; }
    public void setDuracionTurnoMin(int min)         { this.duracionTurnoMin = min; }
    public void setDiasSemana(String diasSemana)     { this.diasSemana = diasSemana; }
}