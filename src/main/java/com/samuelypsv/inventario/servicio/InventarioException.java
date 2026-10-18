package com.samuelypsv.inventario.servicio;

/** Error de negocio con un mensaje apto para mostrar al usuario. */
public class InventarioException extends Exception {

    public InventarioException(String mensaje) {
        super(mensaje);
    }

    public InventarioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
