module com.control.controllers {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires java.sql;
    requires javafx.graphics;
    requires mysql.connector.j;
    requires org.bouncycastle.provider;

    exports com.control.controllers;

    opens com.control.controllers to javafx.fxml;

}