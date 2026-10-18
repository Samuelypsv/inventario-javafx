package com.samuelypsv.inventario;

import com.samuelypsv.inventario.datos.BaseDatos;
import com.samuelypsv.inventario.modelo.Producto;
import com.samuelypsv.inventario.servicio.InventarioException;
import com.samuelypsv.inventario.servicio.InventarioService;
import com.samuelypsv.inventario.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

public class InventarioApp extends Application {

    private BaseDatos bd;

    @Override
    public void start(Stage stage) throws Exception {
        Path carpeta = Path.of(System.getProperty("user.home"), ".inventario-javafx");
        Files.createDirectories(carpeta);
        bd = BaseDatos.enArchivo(carpeta.resolve("inventario.db").toString());
        InventarioService service = new InventarioService(bd, Clock.systemDefaultZone());
        cargarEjemploSiVacio(service);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        loader.setControllerFactory(c -> new MainController(service));
        Scene scene = new Scene(loader.load(), 1100, 680);
        scene.getStylesheets().add(getClass().getResource("estilos.css").toExternalForm());

        stage.setTitle("Inventario");
        stage.setMinWidth(900);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (bd != null) bd.close();
    }

    private static void cargarEjemploSiVacio(InventarioService service) throws InventarioException {
        if (!service.listar().isEmpty()) return;
        service.guardar(new Producto(null, "TEC-01", "Teclado mecánico", "Periféricos", new BigDecimal("49.90"), 12, 5));
        service.guardar(new Producto(null, "RAT-01", "Ratón inalámbrico", "Periféricos", new BigDecimal("19.95"), 3, 5));
        service.guardar(new Producto(null, "MON-27", "Monitor 27\" QHD", "Pantallas", new BigDecimal("229.00"), 7, 2));
        service.guardar(new Producto(null, "SSD-1T", "SSD NVMe 1 TB", "Almacenamiento", new BigDecimal("74.50"), 20, 6));
        service.guardar(new Producto(null, "CAB-HD", "Cable HDMI 2 m", "Cables", new BigDecimal("6.99"), 1, 10));
        service.guardar(new Producto(null, "AUR-BT", "Auriculares Bluetooth", "Audio", new BigDecimal("39.00"), 9, 3));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
