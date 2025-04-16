package com.control.controllers;

import com.control.cryptography.Argon2;
import com.control.database.Database;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

//* TODO: User feedback for actions being completed, parameters for fields, and better usage directions */

public class CreateController {
    @FXML
    private TextField lastName;
    @FXML
    private TextField firstName;
    @FXML
    private PasswordField passwordConfirm;
    @FXML
    private PasswordField password;
    @FXML
    private TextField username;
    @FXML
    private Label createText;
    @FXML
    private VBox createAccountBox;
    private Stage currentStage;
    private MainController mainController;

    public void initialize(){
        createText.setFocusTraversable(true);
        createText.requestFocus();
    }

    @FXML
    private void onCreateClick() throws SQLException, IOException {
        if(!username.getText().isEmpty() && !password.getText().isEmpty() && !passwordConfirm.getText().isEmpty() && !firstName.getText().isEmpty() && !lastName.getText().isEmpty()) {
            if (password.getText().equals(passwordConfirm.getText())) {
                String usernameValue = username.getText();
                if(getUserId(usernameValue) == -1) {
                    Argon2 argon2 = new Argon2();
                    String params = argon2.generateParameterizedHash(password.getText());
                    String[] split = params.split("\\$");
                    byte[] saltBytes = argon2.getSalt();
                    String salt = Base64.getEncoder().encodeToString(saltBytes);
                    String hash = split[8];
                    String firstNameValue = firstName.getText();
                    String lastNameValue = lastName.getText();
                    String userString = firstNameValue + " " + lastNameValue;

                    Connection connection = Database.newConnection();
                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO " +
                            "user(username, password, fName, lName, salt) VALUES (?, ?, ?, ?, ?)");

                    stmt.setString(1, usernameValue);
                    stmt.setString(2, hash);
                    stmt.setString(3, firstNameValue);
                    stmt.setString(4, lastNameValue);
                    stmt.setString(5, salt);

                    stmt.executeUpdate();

                    BorderPane mainBorderPane = openMainWindow(userString);
                    int userId = getUserId(usernameValue);
                    mainController.setUserId(userId);
                    Pane newContent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("logContent.fxml")));
                    newContent.setPrefSize(mainBorderPane.getWidth() - 200, mainBorderPane.getHeight() - 100);
                    mainBorderPane.setCenter(newContent);
                } else {
                    createText.setTextFill(Paint.valueOf("red"));
                    createText.setText("User already exists");
                }
            } else {
                createText.setTextFill(Paint.valueOf("red"));
                createText.setText("Passwords do not match!\nPlease ensure both fields are identical");
            }
        } else {
            createText.setTextFill(Paint.valueOf("red"));
            createText.setText("All fields required");
        }
    }

    private BorderPane openMainWindow(String userLabel) throws IOException {
        currentStage.close();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainScreen.fxml"));
        BorderPane mainBorderPane = loader.load();
        mainController = loader.getController();
        Scene scene = new Scene(mainBorderPane);
        Stage stage = new Stage();
        mainController.setCurrentStage(stage);
        stage.setTitle("TipTracker");
        stage.setScene(scene);
        stage.show();
        mainController.updateUserLabel(userLabel);
        return mainBorderPane;
    }

    private int getUserId(String userName) throws SQLException { //returns 0 if user does not exist
        int userId = -1;
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT userId FROM user WHERE username = ?");
        stmt.setString(1, userName);
        ResultSet rs = stmt.executeQuery();
        if(rs.next()){
            userId = rs.getInt("userId");
        }
        return userId;
    }

    public void setCurrentStage(Stage stage){
        this.currentStage = stage;
    }

    public void onBackClick(ActionEvent actionEvent) throws IOException {
        currentStage.close();
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("logIn.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 320);
        LogInController c = fxmlLoader.getController();
        Stage stage = new Stage();
        stage.setTitle("New TipTracker User");
        stage.setScene(scene);
        stage.show();
        c.setCurrentStage(stage);
    }
}
