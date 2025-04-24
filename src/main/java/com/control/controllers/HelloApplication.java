package com.control.controllers;

import com.control.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class HelloApplication extends Application {
    static InputStream inputStream = HelloApplication.class.getClassLoader().getResourceAsStream("config.properties");
    static Properties properties = new Properties();


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("logIn.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 320);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        LogInController c = fxmlLoader.getController();
        c.setCurrentStage(stage);
        stage.setTitle("TipTracker Log In");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws IOException {
        properties.load(inputStream);
        try {
            Database.initialize(properties);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize database", e);
        }
        launch();
    }
}