package com.samuelypsv.inventario.servicio;

import com.samuelypsv.inventario.datos.BaseDatos;
import com.samuelypsv.inventario.datos.MovimientoDao;
import com.samuelypsv.inventario.datos.ProductoDao;
import com.samuelypsv.inventario.modelo.Movimiento;
import com.samuelypsv.inventario.modelo.Producto;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Reglas del inventario. Las operaciones que tocan varias tablas van en una transacción. */
public class InventarioService {

    public record Resumen(int productos, int unidades, BigDecimal valorTotal, long conStockBajo) {
    }

    private final BaseDatos bd;
    private final ProductoDao productos;
    private final MovimientoDao movimientos;
    private final Clock clock;

    public InventarioService(BaseDatos bd, Clock clock) {
        this.bd = bd;
        this.productos = new ProductoDao(bd);
        this.movimientos = new MovimientoDao(bd);
        this.clock = clock;
    }

    public List<Producto> listar() throws InventarioException {
        try {
            return productos.listar();
        } catch (SQLException e) {
            throw new InventarioException("No se pudo leer el inventario", e);
        }
    }

    public Producto guardar(Producto p) throws InventarioException {
        try {
            if (productos.existeCodigo(p.codigo(), p.id())) {
                throw new InventarioException("Ya existe un producto con el código " + p.codigo());
            }
            if (p.id() == null) {
                return productos.insertar(p);
            }
            productos.actualizar(p);
            return p;
        } catch (SQLException e) {
            throw new InventarioException("No se pudo guardar el producto", e);
        }
    }

    public void eliminar(int id) throws InventarioException {
        try {
            productos.eliminar(id);
        } catch (SQLException e) {
            throw new InventarioException("No se pudo eliminar el producto", e);
        }
    }

    /** Suma o resta stock y deja constancia del movimiento, todo o nada. */
    public Producto registrarMovimiento(int productoId, Movimiento.Tipo tipo, int cantidad, String nota)
            throws InventarioException {
        if (cantidad <= 0) {
            throw new InventarioException("La cantidad debe ser mayor que cero");
        }
        Connection cx = bd.conexion();
        try {
            cx.setAutoCommit(false);
            Producto p = productos.buscarPorId(productoId)
                    .orElseThrow(() -> new InventarioException("El producto ya no existe"));
            int nuevoStock = tipo == Movimiento.Tipo.ENTRADA ? p.stock() + cantidad : p.stock() - cantidad;
            if (nuevoStock < 0) {
                throw new InventarioException("Stock insuficiente: quedan " + p.stock() + " unidades de " + p.nombre());
            }
            productos.actualizarStock(productoId, nuevoStock);
            movimientos.insertar(new Movimiento(null, productoId, tipo, cantidad, nota, LocalDateTime.now(clock)));
            cx.commit();
            return p.conStock(nuevoStock);
        } catch (SQLException | InventarioException e) {
            rollback(cx);
            if (e instanceof InventarioException ie) throw ie;
            throw new InventarioException("No se pudo registrar el movimiento", e);
        } finally {
            try {
                cx.setAutoCommit(true);
            } catch (SQLException ignored) {
                // la conexión queda inutilizable; el siguiente acceso informará del error
            }
        }
    }

    public List<Movimiento> historial(int productoId) throws InventarioException {
        try {
            return movimientos.deProducto(productoId);
        } catch (SQLException e) {
            throw new InventarioException("No se pudo leer el historial", e);
        }
    }

    public static Resumen resumen(List<Producto> lista) {
        return new Resumen(
                lista.size(),
                lista.stream().mapToInt(Producto::stock).sum(),
                lista.stream().map(Producto::valorEnStock).reduce(BigDecimal.ZERO, BigDecimal::add),
                lista.stream().filter(Producto::stockBajo).count());
    }

    /** Exporta a CSV con separador ';' para que Excel en español lo abra bien. */
    public static void exportarCsv(List<Producto> lista, Writer out) throws IOException {
        out.write("codigo;nombre;categoria;precio;stock;stock_minimo\n");
        for (Producto p : lista) {
            out.write(String.join(";", csv(p.codigo()), csv(p.nombre()), csv(p.categoria()),
                    p.precio().toPlainString(), String.valueOf(p.stock()), String.valueOf(p.stockMinimo())));
            out.write("\n");
        }
    }

    private static String csv(String valor) {
        if (valor == null) return "";
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private static void rollback(Connection cx) {
        try {
            cx.rollback();
        } catch (SQLException ignored) {
            // no hay nada más que hacer
        }
    }
}
