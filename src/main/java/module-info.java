module com.example.lr9 {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.lr9 to javafx.fxml;
    exports com.example.lr9;
}