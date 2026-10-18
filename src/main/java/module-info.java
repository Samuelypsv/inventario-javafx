module com.samuelypsv.inventario {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.samuelypsv.inventario.ui to javafx.fxml;
    exports com.samuelypsv.inventario;
    exports com.samuelypsv.inventario.modelo;
}
