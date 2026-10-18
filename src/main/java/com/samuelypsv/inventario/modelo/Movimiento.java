package com.samuelypsv.inventario.modelo;

import java.time.LocalDateTime;

public record Movimiento(Integer id, int productoId, Tipo tipo, int cantidad, String nota, LocalDateTime fecha) {

    public enum Tipo { ENTRADA, SALIDA }

    public Movimiento {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
    }
}
