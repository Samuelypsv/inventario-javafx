package com.samuelypsv.inventario;

import com.samuelypsv.inventario.datos.BaseDatos;
import com.samuelypsv.inventario.modelo.Movimiento;
import com.samuelypsv.inventario.modelo.Producto;
import com.samuelypsv.inventario.servicio.InventarioException;
import com.samuelypsv.inventario.servicio.InventarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventarioServiceTest {

    private BaseDatos bd;
    private InventarioService service;

    @BeforeEach
    void setUp() throws Exception {
        bd = BaseDatos.enMemoria();
        service = new InventarioService(bd, Clock.fixed(Instant.parse("2026-05-10T09:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() throws Exception {
        bd.close();
    }

    private Producto teclado(int stock) throws InventarioException {
        return service.guardar(new Producto(null, "TEC-01", "Teclado mecánico", "Periféricos",
                new BigDecimal("49.90"), stock, 5));
    }

    @Test
    void guardaYListaProductos() throws Exception {
        Producto p = teclado(10);

        assertNotNull(p.id());
        assertEquals(List.of(p), service.listar());
    }

    @Test
    void noPermiteCodigosDuplicados() throws Exception {
        teclado(10);

        var ex = assertThrows(InventarioException.class, () -> teclado(3));
        assertTrue(ex.getMessage().contains("TEC-01"));
    }

    @Test
    void editarNoChocaConSuPropioCodigo() throws Exception {
        Producto p = teclado(10);

        service.guardar(new Producto(p.id(), "TEC-01", "Teclado editado", "Periféricos", p.precio(), 10, 5));

        assertEquals("Teclado editado", service.listar().getFirst().nombre());
    }

    @Test
    void lasEntradasYSalidasActualizanStockEHistorial() throws Exception {
        Producto p = teclado(10);

        service.registrarMovimiento(p.id(), Movimiento.Tipo.ENTRADA, 5, "Pedido proveedor");
        Producto tras = service.registrarMovimiento(p.id(), Movimiento.Tipo.SALIDA, 12, "Venta");

        assertEquals(3, tras.stock());
        assertEquals(3, service.listar().getFirst().stock());
        List<Movimiento> historial = service.historial(p.id());
        assertEquals(2, historial.size());
        assertEquals(Movimiento.Tipo.SALIDA, historial.getFirst().tipo());
    }

    @Test
    void unaSalidaSinStockSuficienteNoCambiaNada() throws Exception {
        Producto p = teclado(2);

        var ex = assertThrows(InventarioException.class,
                () -> service.registrarMovimiento(p.id(), Movimiento.Tipo.SALIDA, 5, null));

        assertTrue(ex.getMessage().startsWith("Stock insuficiente"));
        assertEquals(2, service.listar().getFirst().stock());
        assertTrue(service.historial(p.id()).isEmpty());
    }

    @Test
    void resumenCalculaValorYStockBajo() throws Exception {
        teclado(4);
        service.guardar(new Producto(null, "RAT-01", "Ratón", "Periféricos", new BigDecimal("15.00"), 20, 5));

        var r = InventarioService.resumen(service.listar());

        assertEquals(2, r.productos());
        assertEquals(24, r.unidades());
        assertEquals(new BigDecimal("499.60"), r.valorTotal());
        assertEquals(1, r.conStockBajo());
    }

    @Test
    void exportaCsvEscapandoSeparadores() throws Exception {
        service.guardar(new Producto(null, "CAB-01", "Cable HDMI; 2m", null, new BigDecimal("7.5"), 3, 1));
        StringWriter out = new StringWriter();

        InventarioService.exportarCsv(service.listar(), out);

        assertEquals("""
                codigo;nombre;categoria;precio;stock;stock_minimo
                CAB-01;"Cable HDMI; 2m";;7.5;3;1
                """, out.toString());
    }

    @Test
    void elModeloRechazaDatosNoValidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new Producto(null, "", "x", null, BigDecimal.ONE, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Producto(null, "A", "x", null, new BigDecimal("-1"), 0, 0));
    }
}
