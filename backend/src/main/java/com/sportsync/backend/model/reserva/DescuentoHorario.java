package com.sportsync.backend.model.reserva;

import com.sportsync.backend.model.cancha.Cancha;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "descuento_horario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cancha_id", "hora"})
)
public class DescuentoHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalTime hora;

    // 0.0 = sin descuento, 0.30 = 30% de descuento
    @Column(nullable = false)
    private double descuentoActual = 0.0;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public DescuentoHorario() {}

    public Long getId()                       { return id; }
    public Cancha getCancha()                 { return cancha; }
    public LocalTime getHora()                { return hora; }
    public double getDescuentoActual()        { return descuentoActual; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    public void setCancha(Cancha cancha)      { this.cancha = cancha; }
    public void setHora(LocalTime hora)       { this.hora = hora; }
    public void setDescuentoActual(double d)  { this.descuentoActual = d; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}