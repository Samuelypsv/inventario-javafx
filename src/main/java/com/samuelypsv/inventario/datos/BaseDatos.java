package com.samuelypsv.inventario.datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Conexión a SQLite y creación del esquema la primera vez. */
public class BaseDatos implements AutoCloseable {

    private final Connection conexion;

    public BaseDatos(String url) throws SQLException {
        this.conexion = DriverManager.getConnection(url);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS productos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        codigo TEXT NOT NULL UNIQUE,
                        nombre TEXT NOT NULL,
                        categoria TEXT,
                        precio TEXT NOT NULL,
                        stock INTEGER NOT NULL CHECK (stock >= 0),
                        stock_minimo INTEGER NOT NULL DEFAULT 0
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS movimientos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        producto_id INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
                        tipo TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA')),
                        cantidad INTEGER NOT NULL,
                        nota TEXT,
                        fecha TEXT NOT NULL
                    )""");
        }
    }

    public static BaseDatos enArchivo(String ruta) throws SQLException {
        return new BaseDatos("jdbc:sqlite:" + ruta);
    }

    public static BaseDatos enMemoria() throws SQLException {
        return new BaseDatos("jdbc:sqlite::memory:");
    }

    public Connection conexion() {
        return conexion;
    }

    @Override
    public void close() throws SQLException {
        conexion.close();
    }
}
