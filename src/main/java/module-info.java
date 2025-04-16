module com.control.controllers {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires org.bouncycastle.provider;


    opens com.control.controllers to javafx.fxml;
    exports com.control.controllers;
}