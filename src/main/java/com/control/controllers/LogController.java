package com.control.controllers;

import com.control.database.Database;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

//* TODO: User feedback for actions being completed, parameters for fields, and better usage directions */
//* TODO: Delete a job, which will confirm action from user and delete all data associated with job */

public class LogController {
    @FXML
    private TextField newJobName;
    @FXML
    private VBox newJobBox;
    @FXML
    private VBox logBox;
    @FXML
    private Button backButton;
    @FXML
    private Button submitJobButton;
    @FXML
    private DatePicker newJobStartDate;
    @FXML
    private TextField logHours;
    @FXML
    private ComboBox<String> jobChoice;
    @FXML
    private DatePicker logDate;
    @FXML
    private TextField logAmount;
    @FXML
    private CheckBox outlierBox;
    @FXML
    private Button enterLogButton;
    private final int userId = MainController.getUserId();
    private final ArrayList<String> jobList = new ArrayList<>();

    public void initialize() throws SQLException {
        logHours.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String input = logHours.getText() + event.getCharacter();

            // Regular expression to match a valid number with up to two decimal places
            if (!input.matches("\\d*(\\.\\d?)?")) {
                event.consume();
            }
        });

        logAmount.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String input = logAmount.getText() + event.getCharacter();

            if (!input.matches("\\d*(\\.\\d{0,2})?")) {
                event.consume();
            }
        });

        // TODO: regex for date selectorS
        loadJobList();
        newJobStartDate.setValue(LocalDate.now());
        logDate.setValue(LocalDate.now());

        jobChoice.requestFocus();
    }

    @FXML
    private void onEnterLogButtonClick () throws SQLException {
        if(!jobChoice.getValue().equals("Jobs")) {
            String selectedJob = jobChoice.getValue();
            if (logDate.getValue() != null && !logHours.getText().isEmpty() && !logAmount.getText().isEmpty()) {
                int jobId = getJobId(selectedJob);
                Date sqlDate = Date.valueOf(logDate.getValue());
                double hours = Double.parseDouble(logHours.getText());
                double amount = Double.parseDouble(logAmount.getText());
                int outlier = outlierBox.isSelected() ? 1 : 0;

                Connection connection = Database.newConnection();
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO log (jobId, date, hours, amount, outlier) VALUES (?, ?, ?, ?, ?)");

                stmt.setInt(1, jobId);
                stmt.setDate(2, sqlDate);
                stmt.setDouble(3, hours);
                stmt.setDouble(4, amount);
                stmt.setInt(5, outlier);
                stmt.executeUpdate();

                showFieldPromptText();
            }
        }
    }

    @FXML
    private void onNewJobButtonClick (){
        newJobBox.setVisible(true);
        logBox.setVisible(false);
    }

    @FXML
    private void onSubmitNewJobButtonClick () throws SQLException {
        if(newJobName.getText() != null && newJobStartDate.getValue() != null){
            String newJobNameInput = newJobName.getText();
            if(!jobList.contains(newJobNameInput)) {
                Date sqlDate = Date.valueOf(newJobStartDate.getValue());
                Connection connection = Database.newConnection();
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO job (userId, jobName, startDate) VALUES (?, ?, ?)");

                stmt.setInt(1, userId);
                stmt.setString(2, newJobNameInput);
                stmt.setDate(3, sqlDate);
                stmt.executeUpdate();

                newJobName.clear();
                newJobStartDate.setValue(LocalDate.now());
                jobList.add(newJobNameInput);
                jobChoice.getItems().add(newJobNameInput);
                jobChoice.setValue(newJobNameInput);
                newJobBox.setVisible(false);
                logBox.setVisible(true);
                showFieldPromptText();
            }
        }
    }

    private void loadJobList () throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT jobName FROM job WHERE userId = ?");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            jobList.remove("Jobs");
            jobList.add(rs.getString("jobName"));
        }

        if(jobList.isEmpty()){
            jobList.add("Jobs");
        }
        jobChoice.getItems().addAll(jobList);
        jobChoice.setValue("Jobs"); //default when open
    }

    @FXML
    private void onBackButtonClick() {
        logBox.setVisible(true);
        newJobBox.setVisible(false);
        jobChoice.requestFocus();
        logDate.setValue(LocalDate.now());
        showFieldPromptText();
    }

    private int getJobId(String jobName) throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT jobId FROM job WHERE jobName = ? AND userId = ?");

        stmt.setString(1, jobName);
        stmt.setInt(2, userId);
        ResultSet rs = stmt.executeQuery();

        int jobId = 0;
        if (rs.next()) {
            jobId = rs.getInt("jobId");
        }
        return jobId;
    }

    @FXML
    private void onJobSelection() {
        showFieldPromptText();
        jobChoice.getItems().remove("Jobs");
    }

    private void showFieldPromptText() {
        logDate.setValue(null);
        logHours.clear();
        logAmount.clear();
        outlierBox.setSelected(false);
    }
}
