package com.samuelypsv.inventario.ui;

import com.samuelypsv.inventario.modelo.Producto;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;

/** Diálogo de alta y edición de productos con validación antes de cerrar. */
public class ProductoDialog extends Dialog<Producto> {

    private final TextField codigo = new TextField();
    private final TextField nombre = new TextField();
    private final ComboBox<String> categoria = new ComboBox<>();
    private final TextField precio = new TextField();
    private final Spinner<Integer> stock = new Spinner<>(0, 1_000_000, 0);
    private final Spinner<Integer> minimo = new Spinner<>(0, 1_000_000, 0);
    private final Label error = new Label();

    public ProductoDialog(Producto original, ObservableList<String> categorias) {
        setTitle(original == null ? "Nuevo producto" : "Editar producto");
        setHeaderText(original == null ? "Datos del nuevo producto" : original.nombre());

        categoria.setItems(categorias);
        categoria.setEditable(true);
        stock.setEditable(true);
        minimo.setEditable(true);
        error.setStyle("-fx-text-fill: #cf222e;");
        error.setWrapText(true);

        if (original != null) {
            codigo.setText(original.codigo());
            nombre.setText(original.nombre());
            categoria.setValue(original.categoria());
            precio.setText(original.precio().toPlainString());
            stock.getValueFactory().setValue(original.stock());
            minimo.getValueFactory().setValue(original.stockMinimo());
            // el stock se cambia con entradas y salidas para que quede en el historial
            stock.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        fila(grid, 0, "Código", codigo);
        fila(grid, 1, "Nombre", nombre);
        fila(grid, 2, "Categoría", categoria);
        fila(grid, 3, "Precio (€)", precio);
        fila(grid, 4, original == null ? "Stock inicial" : "Stock", stock);
        fila(grid, 5, "Stock mínimo", minimo);
        grid.add(error, 0, 6, 2, 1);
        getDialogPane().setContent(grid);

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        // si los datos no son válidos, consumimos el evento y el diálogo no se cierra
        getDialogPane().lookupButton(guardar).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (construir(original) == null) e.consume();
        });
        setResultConverter(bt -> bt == guardar ? construir(original) : null);
    }

    private Producto construir(Producto original) {
        try {
            String cat = categoria.getEditor().getText();
            BigDecimal valor = new BigDecimal(precio.getText().trim().replace(',', '.'));
            Producto p = new Producto(original == null ? null : original.id(),
                    codigo.getText().trim().toUpperCase(), nombre.getText().trim(),
                    cat == null || cat.isBlank() ? null : cat.trim(),
                    valor, stock.getValue(), minimo.getValue());
            error.setText("");
            return p;
        } catch (NumberFormatException e) {
            error.setText("El precio debe ser un número, por ejemplo 19,95");
        } catch (IllegalArgumentException e) {
            error.setText(e.getMessage());
        }
        return null;
    }

    private static void fila(GridPane grid, int fila, String etiqueta, Node campo) {
        grid.add(new Label(etiqueta), 0, fila);
        grid.add(campo, 1, fila);
        if (campo instanceof Control c) c.setPrefWidth(260);
    }
}
