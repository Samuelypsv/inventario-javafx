package com.samuelypsv.inventario.ui;

import com.samuelypsv.inventario.modelo.Movimiento;
import com.samuelypsv.inventario.modelo.Producto;
import com.samuelypsv.inventario.servicio.InventarioException;
import com.samuelypsv.inventario.servicio.InventarioService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class MainController {

    private static final String TODAS = "Todas las categorías";
    private static final NumberFormat EUROS = NumberFormat.getCurrencyInstance(Locale.of("es", "ES"));
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InventarioService service;
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private final FilteredList<Producto> filtrados = new FilteredList<>(productos);

    @FXML private Label lblProductos, lblUnidades, lblValor, lblBajo;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private CheckBox chkStockBajo;
    @FXML private Button btnEditar, btnEliminar, btnEntrada, btnSalida;
    @FXML private TableView<Producto> tabla;
    @FXML private TableColumn<Producto, String> colCodigo, colNombre, colCategoria;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock, colMinimo;
    @FXML private ListView<Movimiento> lstHistorial;

    public MainController(InventarioService service) {
        this.service = service;
    }

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigo()));
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colCategoria.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().categoria()));
        colPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().precio()));
        colStock.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().stock()));
        colMinimo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().stockMinimo()));
        colPrecio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal precio, boolean vacio) {
                super.updateItem(precio, vacio);
                setText(vacio || precio == null ? null : EUROS.format(precio));
            }
        });

        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Producto p, boolean vacio) {
                super.updateItem(p, vacio);
                getStyleClass().remove("stock-bajo");
                if (!vacio && p != null && p.stockBajo()) getStyleClass().add("stock-bajo");
            }
        });

        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        tabla.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) editar();
        });

        var sinSeleccion = tabla.getSelectionModel().selectedItemProperty().isNull();
        btnEditar.disableProperty().bind(sinSeleccion);
        btnEliminar.disableProperty().bind(sinSeleccion);
        btnEntrada.disableProperty().bind(sinSeleccion);
        btnSalida.disableProperty().bind(sinSeleccion);

        txtBuscar.textProperty().addListener((o, a, b) -> aplicarFiltro());
        cmbCategoria.valueProperty().addListener((o, a, b) -> aplicarFiltro());
        chkStockBajo.selectedProperty().addListener((o, a, b) -> aplicarFiltro());

        lstHistorial.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Movimiento m, boolean vacio) {
                super.updateItem(m, vacio);
                getStyleClass().removeAll("entrada", "salida");
                if (vacio || m == null) {
                    setText(null);
                    return;
                }
                boolean entrada = m.tipo() == Movimiento.Tipo.ENTRADA;
                getStyleClass().add(entrada ? "entrada" : "salida");
                setText("%s  %s%d%s".formatted(FECHA.format(m.fecha()), entrada ? "+" : "−", m.cantidad(),
                        m.nota() == null || m.nota().isBlank() ? "" : "  · " + m.nota()));
            }
        });
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> cargarHistorial(p));

        recargar();
    }

    private void recargar() {
        Integer seleccionado = Optional.ofNullable(tabla.getSelectionModel().getSelectedItem())
                .map(Producto::id).orElse(null);
        try {
            productos.setAll(service.listar());
        } catch (InventarioException e) {
            Dialogos.error(e.getMessage());
        }

        String categoria = cmbCategoria.getValue();
        var categorias = FXCollections.observableArrayList(TODAS);
        productos.stream().map(Producto::categoria).filter(Objects::nonNull).distinct().sorted()
                .forEach(categorias::add);
        cmbCategoria.setItems(categorias);
        cmbCategoria.setValue(categorias.contains(categoria) ? categoria : TODAS);

        var r = InventarioService.resumen(productos);
        lblProductos.setText(String.valueOf(r.productos()));
        lblUnidades.setText(String.valueOf(r.unidades()));
        lblValor.setText(EUROS.format(r.valorTotal()));
        lblBajo.setText(String.valueOf(r.conStockBajo()));

        if (seleccionado != null) {
            productos.stream().filter(p -> seleccionado.equals(p.id())).findFirst()
                    .ifPresent(p -> tabla.getSelectionModel().select(p));
        }
    }

    private void aplicarFiltro() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String categoria = cmbCategoria.getValue();
        boolean soloBajo = chkStockBajo.isSelected();
        filtrados.setPredicate(p ->
                (texto.isEmpty() || p.codigo().toLowerCase().contains(texto) || p.nombre().toLowerCase().contains(texto))
                        && (categoria == null || TODAS.equals(categoria) || categoria.equals(p.categoria()))
                        && (!soloBajo || p.stockBajo()));
    }

    private void cargarHistorial(Producto p) {
        if (p == null) {
            lstHistorial.getItems().clear();
            return;
        }
        try {
            lstHistorial.getItems().setAll(service.historial(p.id()));
        } catch (InventarioException e) {
            Dialogos.error(e.getMessage());
        }
    }

    private Producto seleccionado() {
        return tabla.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void nuevo() {
        guardarDesdeDialogo(null);
    }

    @FXML
    private void editar() {
        if (seleccionado() != null) guardarDesdeDialogo(seleccionado());
    }

    private void guardarDesdeDialogo(Producto original) {
        new ProductoDialog(original, cmbCategoria.getItems().filtered(c -> !TODAS.equals(c)))
                .showAndWait()
                .ifPresent(p -> {
                    try {
                        Producto guardado = service.guardar(p);
                        recargar();
                        productos.stream().filter(x -> x.id().equals(guardado.id())).findFirst()
                                .ifPresent(x -> tabla.getSelectionModel().select(x));
                    } catch (InventarioException e) {
                        Dialogos.error(e.getMessage());
                    }
                });
    }

    @FXML
    private void eliminar() {
        Producto p = seleccionado();
        if (p != null && Dialogos.confirmar("¿Eliminar «" + p.nombre() + "» y su historial?")) {
            try {
                service.eliminar(p.id());
                recargar();
            } catch (InventarioException e) {
                Dialogos.error(e.getMessage());
            }
        }
    }

    @FXML
    private void entrada() {
        movimiento(Movimiento.Tipo.ENTRADA);
    }

    @FXML
    private void salida() {
        movimiento(Movimiento.Tipo.SALIDA);
    }

    private void movimiento(Movimiento.Tipo tipo) {
        Producto p = seleccionado();
        if (p == null) return;
        Dialogos.movimiento(p, tipo).ifPresent(m -> {
            try {
                service.registrarMovimiento(p.id(), tipo, m.cantidad(), m.nota());
                recargar();
                cargarHistorial(seleccionado());
            } catch (InventarioException e) {
                Dialogos.error(e.getMessage());
            }
        });
    }

    @FXML
    private void exportar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar inventario");
        chooser.setInitialFileName("inventario.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        var archivo = chooser.showSaveDialog(tabla.getScene().getWindow());
        if (archivo == null) return;
        try (var out = Files.newBufferedWriter(archivo.toPath(), StandardCharsets.UTF_8)) {
            InventarioService.exportarCsv(filtrados, out);
            Dialogos.info("Exportados " + filtrados.size() + " productos a " + archivo.getName());
        } catch (IOException e) {
            Dialogos.error("No se pudo escribir el archivo: " + e.getMessage());
        }
    }

}
