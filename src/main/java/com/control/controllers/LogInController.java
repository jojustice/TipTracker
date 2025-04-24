package com.control.controllers;

import com.control.cryptography.Argon2;
import com.control.database.Database;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Objects;

public class LogInController {
    @FXML
    private Button logInButton;
    @FXML
    private TextField usernameLogIn;
    @FXML
    private TextField passwordLogIn;
    @FXML
    private VBox logInBox;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Label welcomeText;
    private int userId;
    private Stage currentStage;
    public void initialize(){
        welcomeText.setFocusTraversable(true);
        welcomeText.requestFocus();
    }

    @FXML
    private void onLogInClick() throws IOException, SQLException {
        if(!usernameLogIn.getText().isEmpty() && !passwordLogIn.getText().isEmpty()) {
            String usernameValue = usernameLogIn.getText();
            String userSaltString = getUserSalt(usernameValue);
            if(userSaltString != null){
                byte[] userSalt = Base64.getDecoder().decode(userSaltString);
                Argon2 argon2 = new Argon2(userSalt);
                String params = argon2.generateParameterizedHash(passwordLogIn.getText());
                String[] split = params.split("\\$");
                String hash = split[8];

                Connection connection = Database.newConnection();
                PreparedStatement stmt = connection.prepareStatement("SELECT userId, username, fName, lName " +
                            "FROM User WHERE username = ? AND password = ?");

                stmt.setString(1, usernameValue);
                stmt.setString(2, hash);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) { // only expect 1 row, so using if instead of while
                    String userString = rs.getString("fName") + " " + rs.getString("lName");
                    this.userId = rs.getInt("userId");
                    mainBorderPane = openMainWindow(userString);
                    Pane newContent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("logContent.fxml")));
                    newContent.setPrefSize(mainBorderPane.getWidth() - 200, mainBorderPane.getHeight() - 100);
                    mainBorderPane.setCenter(newContent);
                } else {
                    welcomeText.setTextFill(Paint.valueOf("red"));
                    welcomeText.setText("Username and password combination invalid");
                }
            } else {
                welcomeText.setTextFill(Paint.valueOf("red"));
                welcomeText.setText("User not found");
            }
        } else {
            welcomeText.setTextFill(Paint.valueOf("red"));
            welcomeText.setText("All fields required");
        }
    }

    @FXML
    private void openCreateNewUser() throws IOException {
        currentStage.close();
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("createAccount.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 320);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        CreateController c = fxmlLoader.getController();
        Stage stage = new Stage();
        stage.setTitle("New TipTracker User");
        stage.setScene(scene);
        stage.show();
        c.setCurrentStage(stage);
    }

    @FXML
    private BorderPane openMainWindow(String userName) throws IOException {
        currentStage.close();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainScreen.fxml"));
        BorderPane mainBorderPane = loader.load();
        MainController mainController = loader.getController();
        mainController.setUserId(userId);

        Scene scene = new Scene(mainBorderPane);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        Stage stage = new Stage();
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.setTitle("TipTracker");
        stage.setScene(scene);
        stage.show();
        mainController.updateUserLabel(userName);
        mainController.setCurrentStage(stage);
        return mainBorderPane;
    }

    public void setCurrentStage (Stage stage){
        currentStage = stage;
    }

    private String getUserSalt(String username) throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT salt FROM User WHERE username = ?");

        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getString("salt");
        }

        return null;
    }

    @FXML
    private void onEnterKeyLogIn(KeyEvent e){
        if(e.getCode() == KeyCode.ENTER){
            logInButton.fire();
        }
    }
}