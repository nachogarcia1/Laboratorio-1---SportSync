package com.sportsync.backend.testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Crea una base de datos descartable DENTRO del Postgres que ya usa el proyecto (el de
 * {@code docker compose up -d db}) y la elimina al terminar. Así los tests que necesitan escribir
 * datos o probar migraciones no tocan {@code sport_db}. T-18 lo reemplazará por Testcontainers.
 *
 * Las credenciales salen de las variables DB_URL, DB_USERNAME y DB_PASSWORD; si no están, se usan los
 * valores locales de siempre (localhost:5434, postgres/postgres). El usuario debe poder crear bases.
 */
public final class BaseDeDatosTemporal {

    private final String prefijoUrl;
    private final String usuario;
    private final String clave;
    private final String nombre;
    private boolean eliminada;

    private BaseDeDatosTemporal(String prefijoUrl, String usuario, String clave, String nombre) {
        this.prefijoUrl = prefijoUrl;
        this.usuario = usuario;
        this.clave = clave;
        this.nombre = nombre;
    }

    public static BaseDeDatosTemporal crear(String prefijo) {
        String url = valor("DB_URL", "jdbc:postgresql://localhost:5434/sport_db");
        String usuario = valor("DB_USERNAME", "postgres");
        String clave = valor("DB_PASSWORD", "postgres");
        // jdbc:postgresql://host:puerto/base[?params]  →  jdbc:postgresql://host:puerto
        String sinParametros = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String prefijoUrl = sinParametros.substring(0, sinParametros.lastIndexOf('/'));
        String nombre = prefijo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        BaseDeDatosTemporal bd = new BaseDeDatosTemporal(prefijoUrl, usuario, clave, nombre);
        try (Connection c = DriverManager.getConnection(prefijoUrl + "/postgres", usuario, clave);
             Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE " + nombre);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base temporal '" + nombre + "'. ¿Está Postgres arriba "
                    + "y el usuario puede crear bases? (" + e.getMessage() + ")", e);
        }
        // Red de seguridad: se elimina al terminar la JVM aunque el test falle o el contexto siga abierto.
        Runtime.getRuntime().addShutdownHook(new Thread(bd::eliminar));
        return bd;
    }

    public String url()      { return prefijoUrl + "/" + nombre; }
    public String usuario()  { return usuario; }
    public String clave()    { return clave; }
    public String nombre()   { return nombre; }

    public Connection conectar() throws SQLException {
        return DriverManager.getConnection(url(), usuario, clave);
    }

    /** Ejecuta un script completo (varias sentencias, bloques DO $$...$$) con una sola llamada. */
    public void ejecutar(String sql) throws SQLException {
        try (Connection c = conectar(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    public void ejecutarArchivo(Path archivo) throws SQLException, IOException {
        ejecutar(Files.readString(archivo, StandardCharsets.UTF_8));
    }

    public synchronized void eliminar() {
        if (eliminada) return;
        eliminada = true;
        try (Connection c = DriverManager.getConnection(prefijoUrl + "/postgres", usuario, clave);
             Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + nombre + " WITH (FORCE)");
        } catch (SQLException e) {
            System.err.println("No se pudo eliminar la base temporal " + nombre + ": " + e.getMessage());
        }
    }

    /** Busca un archivo de db/migrations subiendo desde el directorio actual (funciona desde backend/ o desde la raíz). */
    public static Path migracion(String archivo) {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 4 && dir != null; i++, dir = dir.getParent()) {
            Path candidato = dir.resolve("db").resolve("migrations").resolve(archivo);
            if (Files.exists(candidato)) return candidato;
        }
        throw new IllegalStateException("No se encontró db/migrations/" + archivo);
    }

    private static String valor(String clave, String porDefecto) {
        String v = System.getenv(clave);
        return v == null || v.isBlank() ? porDefecto : v;
    }
}
