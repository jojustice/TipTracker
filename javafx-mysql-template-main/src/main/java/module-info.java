module com.example.javafxmysqltemplate {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires org.bouncycastle.provider;


    opens com.example.javafxmysqltemplate to javafx.fxml;
    exports com.example.javafxmysqltemplate;
}