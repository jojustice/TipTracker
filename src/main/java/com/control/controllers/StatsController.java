package com.control.controllers;

//* TODO: make variables and methods private */
//* TODO: round decimals instead of format string */
//* TODO: display expense stats */

import com.control.database.StatsRetriever;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

public class StatsController {
    private final int userId = MainController.getUserId();
    public AnchorPane anchorPane;
    private final StatsRetriever s = new StatsRetriever(userId);

    private final LocalDate firstOfMonth = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1);
    //ZoneId gets the systems time zone
    private final LocalDate lastOfMonth = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(firstOfMonth.lengthOfMonth());
    @FXML
    public ScrollPane scrollPane;
    @FXML
    private Label averagePerShift;
    @FXML
    private VBox shiftStatsBox;
    @FXML
    private VBox hourlyStatsBox;
    @FXML
    private Label averageShiftEarnings;
    @FXML
    private Label averageHourly;
    @FXML
    private Label averageHourlyPerJob;
    @FXML
    private VBox avgWeekdayEarningsBarChartContainer;
    @FXML
    private VBox expenseProgressBarContainer;
    @FXML
    private VBox pieChartContainer;
    @FXML
    private Button submitEarningsAreaChartDates;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private VBox areaChartContainer;

    private LocalDate firstLog;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public StatsController() throws SQLException {
    }

    public void initialize() throws SQLException {
        firstLog = s.getEarliestLog();
        startDatePicker.setValue(firstOfMonth);
        endDatePicker.setValue(lastOfMonth);
        setStatsText();
        earningsAreaChart(firstOfMonth, lastOfMonth);
        setDateConstraints(startDatePicker);
        setDateConstraints(endDatePicker);
        showTotalEarningsPieChart();
        showExpenseProgressBar();
        showAvgEarningsPerDayOfWeek();



        /*TODO: check the earliest log is not null to make sure that the user has logs available*/
        //can make controller in main screen and use getEarliestLog to check if not null
        //if not, load page like normal
        //if it is, load another FXML page that says the user must have logs first to view the page
    }



    private void earningsAreaChart(LocalDate start, LocalDate end) throws SQLException {
        CategoryAxis xAxis = new CategoryAxis(getDateRange(start, end));
        xAxis.setLabel("Date");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount");

        AreaChart<String, Number> areaChart = new AreaChart<>(xAxis, yAxis);

        ArrayList<XYChart.Series<String, Number>> data = s.earningsAreaChartData(start, end);
        int i = 0;
        for(XYChart.Series<String, Number> series : data){
            series.setName(s.getJobNameList().get(i));
            areaChart.getData().add(series);
            ++i;
        }

        if(!areaChartContainer.getChildren().isEmpty()){
            areaChartContainer.getChildren().clear();
        }
        areaChartContainer.getChildren().add(areaChart);
    }

    @FXML
    private void onSubmitNewEarningsDateClick(ActionEvent actionEvent) throws SQLException {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        earningsAreaChart(start, end);
    }

    private void showTotalEarningsPieChart() throws SQLException {
        PieChart earningsPieChart = new PieChart(s.earningsPieChartData());
        earningsPieChart.setClockwise(true);
        earningsPieChart.setLabelLineLength(40);
        earningsPieChart.setLabelsVisible(true);
        earningsPieChart.setStartAngle(180);

        pieChartContainer.getChildren().add(earningsPieChart);
    }

    private void showExpenseProgressBar() throws SQLException {
        double progress = s.getExpenseProgress(firstOfMonth, lastOfMonth);
        ProgressBar p = new ProgressBar(progress);
        p.setPrefSize(300,20);
        p.setStyle(
                "-fx-accent: #808080; "
        );

        expenseProgressBarContainer.getChildren().add(p);

        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMinimumFractionDigits(2);
        String percentString = percentFormat.format(progress);

        Label l = new Label();
        l.setText(percentString);

        expenseProgressBarContainer.getChildren().add(l);
        expenseProgressBarContainer.setAlignment(Pos.CENTER);
    }

    //for getting a date range that is not exactly one month
    public static ObservableList<String> getDateRange(LocalDate start, LocalDate end) {
        ObservableList<String> dates = FXCollections.observableArrayList();

        while (!start.isAfter(end)) {
            dates.add(start.format(DATE_FORMATTER));
            start = start.plusDays(1); // Increment the date by one day
        }

        return dates;
    }

    private void showAvgEarningsPerDayOfWeek() throws SQLException {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setCategories(FXCollections.observableArrayList(Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")));
        xAxis.setLabel("Day of Week");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Earnings");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.getData().addAll(s.getAvgEarningsPerWeekDayBarChartData());

        avgWeekdayEarningsBarChartContainer.getChildren().add(barChart);
    }

    //custom DateCell to restrict the selectable dates using earliestLog and the last day of the current month
    private void setDateConstraints(DatePicker p){
        p.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(firstLog) || date.isAfter(lastOfMonth)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;"); // highlight disabled dates
                }
            }
        });
    }

    private void setStatsText() throws SQLException {
        averagePerShift.setText(averagePerShift.getText() + String.format("%.2f", s.getAvgEarningsPerShift()));
        for(String s : s.getAvgShiftEarningsPerJob()){
            Label l = new Label(s);
            shiftStatsBox.getChildren().add(l);
        }

        averageHourly.setText(averageHourly.getText() + String.format("%.2f", s.getAvgHourlyWage()));
        for(String s : s.getAvgHourlyPerJob()){
            Label l = new Label(s);
            hourlyStatsBox.getChildren().add(l);
        }
    }
}
