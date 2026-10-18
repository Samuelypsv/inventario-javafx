package com.samuelypsv.inventario.modelo;

import java.math.BigDecimal;

public record Producto(
        Integer id,
        String codigo,
        String nombre,
        String categoria,
        BigDecimal precio,
        int stock,
        int stockMinimo) {

    public Producto {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("El código es obligatorio");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (precio == null || precio.signum() < 0) throw new IllegalArgumentException("El precio no puede ser negativo");
        if (stock < 0) throw new IllegalArgumentException("El stock no puede ser negativo");
        if (stockMinimo < 0) throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
    }

    public boolean stockBajo() {
        return stock <= stockMinimo;
    }

    public BigDecimal valorEnStock() {
        return precio.multiply(BigDecimal.valueOf(stock));
    }

    public Producto conId(int nuevoId) {
        return new Producto(nuevoId, codigo, nombre, categoria, precio, stock, stockMinimo);
    }

    public Producto conStock(int nuevoStock) {
        return new Producto(id, codigo, nombre, categoria, precio, nuevoStock, stockMinimo);
    }
}
