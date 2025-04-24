package com.control.controllers;

import com.control.database.Database;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

//* TODO: User feedback for actions being completed, parameters for fields, and better usage directions */

public class ExpensesController {
    private final int userId = MainController.getUserId();
    @FXML
    private Button addExpense;
    @FXML
    private Button removeExpense;
    @FXML
    private ComboBox<String> expenseChoice;
    @FXML
    private TextField expenseName;
    @FXML
    private TextField expenseAmount;
    private ArrayList<String> expenseList = new ArrayList<String>();

    public void initialize() throws SQLException { //runs when fxml is loaded.
        expenseAmount.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String input = expenseAmount.getText() + event.getCharacter();

            // Regular expression to match a valid number with up to two decimal places
            if (!input.matches("\\d*(\\.\\d{0,2})?")) {
                event.consume();
            }
        });

      loadExpenseList();
      expenseChoice.requestFocus();
    }

    @FXML
    private void onSubmitExpenseButtonClick() throws SQLException {
        String label = expenseName.getText();
        String amount = expenseAmount.getText();
        String expenseChoiceString = expenseChoice.getValue();
        if(!label.isEmpty() && !amount.isEmpty()){ //both fields not empty
            if(expenseChoiceString.equals("New Expense")) { //new expense
                Connection connection = Database.newConnection();
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO Expense (user, label, amount) " +
                        "VALUES (?, ?, ?)");

                stmt.setInt(1, userId);
                stmt.setString(2, label);
                stmt.setDouble(3, Double.parseDouble(amount));
                stmt.executeUpdate();

                expenseList.add(label); //add expense to list
                expenseChoice.getItems().add(label); //add expense to combo box
                expenseChoice.setValue(label);

            } else { //editing an existing expense
                int expId = getExpenseId(expenseChoiceString);
                Connection connection = Database.newConnection();
                PreparedStatement stmt = connection.prepareStatement("UPDATE Expense SET label = ?, amount = ? WHERE label = ? AND expenseId = ?");

                stmt.setString(1, label);
                stmt.setInt(2, Integer.parseInt(amount));
                stmt.setString(3, expenseChoiceString);
                stmt.setInt(4, expId);

                stmt.executeUpdate();
                //remove old expense from list incase the user changed name
                updateExpenses(expenseChoiceString, label);
                expenseName.setText(label);
                expenseAmount.setText(amount);
            }
        } else {
            //error handle here, message to user input is not valid
            System.out.println("All fields required");
        }
        }
        //if the combo box has an expense chosen
        //set the text values to that expense
        //allow user to update the label and amount

    @FXML
    private void onRemoveExpenseButtonClick() throws SQLException {
        //if the combo box is not on new expense
        String selectedExpense = expenseChoice.getValue();
        if(!selectedExpense.equals("New Expense")){
            int expenseId = getExpenseId(selectedExpense);
            Connection connection = Database.newConnection();
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM Expense WHERE expenseId = ?");
            stmt.setInt(1, expenseId);
            stmt.executeUpdate();
            expenseChoice.setValue("New Expense");
            updateExpenses(selectedExpense, "");
        }
        //default back to new expense
    }

    private void loadExpenseList() throws SQLException {
        ArrayList<String> expenses = new ArrayList<>(); //loads users existing expenses into the combo box
        expenses.add("New Expense");

        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT label FROM Expense WHERE user = ?");
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        while(rs.next()){
            expenses.add(rs.getString("label"));
        }

        expenseList = expenses;
        expenseChoice.getItems().addAll(expenseList);
        expenseChoice.setValue("New Expense"); //default when open
    }

    private void updateExpenses(String old, String newExpenseName){
        if(newExpenseName.isEmpty()){ //empty string will be passed in for deleting an expense
            expenseChoice.getItems().clear();
            expenseList.remove(old);
            expenseChoice.getItems().addAll(expenseList); //resets the dropdown list
            expenseChoice.setValue("New Expense");
        }
        else if(!old.equals(newExpenseName)){
            expenseChoice.getItems().remove(old); //removes old combo box option
            expenseList.remove(old); //removes old label from expense list
            expenseList.add(newExpenseName); //adds new label to expense list
            expenseChoice.getItems().add(newExpenseName); //adds new expense label to combo box
            expenseChoice.setValue(newExpenseName); //sets current selection to be the new expense
        }

    }

    @FXML
    private void onSelectedExpense() throws SQLException { //displays expense info into texts boxes
        if(expenseChoice.getValue() != null) {
            String selectedExpense = expenseChoice.getValue();
            if (!selectedExpense.equals("New Expense")) {
                Connection connection = Database.newConnection();
                PreparedStatement stmt = connection.prepareStatement("SELECT label, amount FROM Expense WHERE label = ? AND user = ?");

                stmt.setString(1, selectedExpense);
                stmt.setInt(2, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    expenseName.setText(rs.getString("label"));
                    if(rs.getDouble("amount") % 1 == 0) //shows no decimals if number is an exact integer
                        expenseAmount.setText(String.valueOf(rs.getInt("amount")));
                    else
                        expenseAmount.setText(String.valueOf(rs.getDouble("amount")));
                }
            } else {
                expenseName.clear();
                expenseAmount.clear();
            }
        }
    }
    private int getExpenseId(String expenseName) throws SQLException { //checks user id, do not need to check in update of expense
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT expenseId FROM Expense WHERE label = ? AND user = ?");

        stmt.setString(1, expenseName);
        stmt.setInt(2, userId);
        ResultSet rs = stmt.executeQuery();

        int expenseId = 0;
        if (rs.next()) {
          expenseId = rs.getInt("expenseId");
        }

        return expenseId;
    }
}
