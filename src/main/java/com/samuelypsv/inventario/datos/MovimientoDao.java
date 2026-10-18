package com.samuelypsv.inventario.datos;

import com.samuelypsv.inventario.modelo.Movimiento;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDao {

    private final Connection cx;

    public MovimientoDao(BaseDatos bd) {
        this.cx = bd.conexion();
    }

    public void insertar(Movimiento m) throws SQLException {
        String sql = "INSERT INTO movimientos (producto_id, tipo, cantidad, nota, fecha) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, m.productoId());
            ps.setString(2, m.tipo().name());
            ps.setInt(3, m.cantidad());
            ps.setString(4, m.nota());
            ps.setString(5, m.fecha().toString());
            ps.executeUpdate();
        }
    }

    public List<Movimiento> deProducto(int productoId) throws SQLException {
        String sql = "SELECT * FROM movimientos WHERE producto_id = ? ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Movimiento> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new Movimiento(rs.getInt("id"), rs.getInt("producto_id"),
                            Movimiento.Tipo.valueOf(rs.getString("tipo")), rs.getInt("cantidad"),
                            rs.getString("nota"), LocalDateTime.parse(rs.getString("fecha"))));
                }
                return lista;
            }
        }
    }
}
