package com.samuelypsv.inventario.datos;

import com.samuelypsv.inventario.modelo.Producto;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla productos con JDBC y sentencias preparadas. */
public class ProductoDao {

    private final Connection cx;

    public ProductoDao(BaseDatos bd) {
        this.cx = bd.conexion();
    }

    public List<Producto> listar() throws SQLException {
        try (Statement st = cx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM productos ORDER BY nombre COLLATE NOCASE")) {
            List<Producto> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        }
    }

    public Optional<Producto> buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = cx.prepareStatement("SELECT * FROM productos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public boolean existeCodigo(String codigo, Integer excluirId) throws SQLException {
        try (PreparedStatement ps = cx.prepareStatement(
                "SELECT 1 FROM productos WHERE codigo = ? AND id IS NOT ?")) {
            ps.setString(1, codigo);
            ps.setObject(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Producto insertar(Producto p) throws SQLException {
        String sql = """
                INSERT INTO productos (codigo, nombre, categoria, precio, stock, stock_minimo)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            rellenar(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return p.conId(keys.getInt(1));
            }
        }
    }

    public void actualizar(Producto p) throws SQLException {
        String sql = """
                UPDATE productos SET codigo = ?, nombre = ?, categoria = ?, precio = ?, stock = ?, stock_minimo = ?
                WHERE id = ?""";
        try (PreparedStatement ps = cx.prepareStatement(sql)) {
            rellenar(ps, p);
            ps.setInt(7, p.id());
            ps.executeUpdate();
        }
    }

    public void actualizarStock(int id, int stock) throws SQLException {
        try (PreparedStatement ps = cx.prepareStatement("UPDATE productos SET stock = ? WHERE id = ?")) {
            ps.setInt(1, stock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = cx.prepareStatement("DELETE FROM productos WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static void rellenar(PreparedStatement ps, Producto p) throws SQLException {
        ps.setString(1, p.codigo());
        ps.setString(2, p.nombre());
        ps.setString(3, p.categoria());
        ps.setString(4, p.precio().toPlainString());
        ps.setInt(5, p.stock());
        ps.setInt(6, p.stockMinimo());
    }

    private static Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(rs.getInt("id"), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("categoria"), new BigDecimal(rs.getString("precio")),
                rs.getInt("stock"), rs.getInt("stock_minimo"));
    }
}
