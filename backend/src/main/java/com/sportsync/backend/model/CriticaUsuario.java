package com.sportsync.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "critica_usuario")
public class CriticaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Usuario admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private int nota; // 1 a 5

    @Column
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public CriticaUsuario() {}

    public Long getId()              { return id; }
    public Usuario getAdmin()        { return admin; }
    public Usuario getUsuario()      { return usuario; }
    public int getNota()             { return nota; }
    public String getComentario()    { return comentario; }
    public LocalDateTime getFecha()  { return fecha; }

    public void setAdmin(Usuario admin)          { this.admin = admin; }
    public void setUsuario(Usuario usuario)      { this.usuario = usuario; }
    public void setNota(int nota)                { this.nota = nota; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}