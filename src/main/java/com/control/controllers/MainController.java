package com.control.controllers;

import com.control.database.Database;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class MainController {
    @FXML
    private Button logButton;
    @FXML
    private Button expensesButton;
    @FXML
    private Button statsButton;
    @FXML
    private BorderPane mainBorderPane;
    private static int userId;
    @FXML
    public Label userLabel;
    private Stage currentStage;

    //* TODO: account settings where you can switch accounts, delete account, change password, change username */

    public void initialize() throws IOException {
        onLogButtonClick(); //initial state, so it will set the log options for the user on load.
    }

    public void updateUserLabel(String username){
        userLabel.setText(username);
    }

    public void setUserId(int userId){
        MainController.userId = userId;
    }

   @FXML
    private void onLogButtonClick() throws IOException {
        FXMLLoader loader  = new FXMLLoader(Objects.requireNonNull(getClass().getResource("logContent.fxml")));
        Pane newContent = loader.load();
        newContent.setPrefSize(mainBorderPane.getWidth() - 200, mainBorderPane.getHeight() - 100);
        mainBorderPane.setCenter(newContent);
    }

    @FXML
    private void onExpenseButtonClick() throws IOException {
        FXMLLoader loader  = new FXMLLoader(Objects.requireNonNull(getClass().getResource("expensesContent.fxml")));
        Pane newContent = loader.load();
        newContent.setPrefSize(mainBorderPane.getWidth() - 200, mainBorderPane.getHeight() - 100);
        mainBorderPane.setCenter(newContent);
    }

    @FXML
    private void onStatsButtonClick() throws IOException, SQLException {
        if(numLogs() == 0){
            handleNoLogData();
        } else {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("statsContent.fxml")));
            ScrollPane newContent = loader.load();
            newContent.setPrefSize(mainBorderPane.getWidth() - 200, mainBorderPane.getHeight() - 100);
            mainBorderPane.setCenter(newContent);
        }
    }

    private void handleNoLogData(){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No Logs Found");
        alert.setHeaderText("You have not submitted any logs yet!");
        alert.setContentText("Submit logs to view your stats.");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("images/warning.png")));
        ImageView image = new ImageView(icon);
        image.setFitHeight(100);
        image.setFitWidth(100);
        alert.setGraphic(image);
        alert.showAndWait();
    }

    public static int getUserId(){
        return userId;
    }

    @FXML
    private void onLogOutClick(ActionEvent actionEvent) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText("Are you sure you want to log out?");
        alert.setContentText("Any unsaved changes will be lost.");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("images/decisions-making.png")));
        ImageView image = new ImageView(icon);
        image.setFitHeight(100);
        image.setFitWidth(100);
        alert.setGraphic(image);

        // Wait for user response
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            currentStage.close();
            FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("logIn.fxml")));
            Scene scene = new Scene(fxmlLoader.load(), 320, 320);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
            LogInController c = fxmlLoader.getController();
            Stage stage = new Stage();
            stage.setTitle("TipTracker Log In");
            stage.setScene(scene);
            stage.show();
            c.setCurrentStage(stage);
        }
    }

    @FXML
    private void onQuitClick() {
        handleExitRequest();
    }

    private void handleExitRequest() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved changes will be lost.");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("images/decisions-making.png")));
        ImageView image = new ImageView(icon);
        image.setFitHeight(100);
        image.setFitWidth(100);
        alert.setGraphic(image);

        // Wait for user response
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            currentStage.close();
            Platform.exit();
        }
    }

    public void setCurrentStage(Stage stage) {
        currentStage = stage;
        currentStage.setOnCloseRequest(e -> {
            e.consume();
            handleExitRequest();
        });
    }

    private int numLogs() throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(logId) as numLogs FROM Log " +
                "WHERE Log.jobId IN (SELECT jobId FROM Job WHERE userId = ?)");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("numLogs");
        }
        return -1;
    }
}
