package com.sportsync.backend.model.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Código de verificación de email enviado a un usuario tras el registro local.
 * Vence a los pocos minutos y limita la cantidad de intentos fallidos.
 */
@Entity
@Table(name = "codigo_verificacion")
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    private int intentos = 0;

    @Column(nullable = false)
    private boolean consumido = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CodigoVerificacion() {}

    public Long getId()                 { return id; }
    public Usuario getUsuario()         { return usuario; }
    public String getCodigo()           { return codigo; }
    public LocalDateTime getExpiraEn()  { return expiraEn; }
    public int getIntentos()            { return intentos; }
    public boolean isConsumido()        { return consumido; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUsuario(Usuario usuario)        { this.usuario = usuario; }
    public void setCodigo(String codigo)           { this.codigo = codigo; }
    public void setExpiraEn(LocalDateTime expiraEn){ this.expiraEn = expiraEn; }
    public void setIntentos(int intentos)          { this.intentos = intentos; }
    public void setConsumido(boolean consumido)    { this.consumido = consumido; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** ¿El código sigue siendo usable (no consumido y no vencido)? */
    @Transient
    public boolean esVigente() {
        return !consumido && LocalDateTime.now().isBefore(expiraEn);
    }
}
