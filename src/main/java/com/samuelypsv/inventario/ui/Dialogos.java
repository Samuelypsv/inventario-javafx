package com.samuelypsv.inventario.ui;

import com.samuelypsv.inventario.modelo.Movimiento;
import com.samuelypsv.inventario.modelo.Producto;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

final class Dialogos {

    record DatosMovimiento(int cantidad, String nota) {
    }

    private Dialogos() {
    }

    static void error(String mensaje) {
        new Alert(Alert.AlertType.ERROR, mensaje).showAndWait();
    }

    static void info(String mensaje) {
        new Alert(Alert.AlertType.INFORMATION, mensaje).showAndWait();
    }

    static boolean confirmar(String mensaje) {
        return new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO)
                .showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    static Optional<DatosMovimiento> movimiento(Producto p, Movimiento.Tipo tipo) {
        boolean entrada = tipo == Movimiento.Tipo.ENTRADA;
        Dialog<DatosMovimiento> d = new Dialog<>();
        d.setTitle(entrada ? "Entrada de stock" : "Salida de stock");
        d.setHeaderText(p.nombre() + " · stock actual: " + p.stock());

        Spinner<Integer> cantidad = new Spinner<>(1, entrada ? 1_000_000 : Math.max(p.stock(), 1), 1);
        cantidad.setEditable(true);
        TextField nota = new TextField();
        nota.setPromptText(entrada ? "Pedido a proveedor" : "Venta, rotura…");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Cantidad"), cantidad);
        grid.addRow(1, new Label("Nota"), nota);
        d.getDialogPane().setContent(grid);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> bt == ButtonType.OK ? new DatosMovimiento(cantidad.getValue(), nota.getText()) : null);
        return d.showAndWait();
    }
}
