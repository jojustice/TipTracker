package com.control.database;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
//* TODO: round decimals instead of format string */
public class StatsRetriever {
    private final int userId;
    private final int numJobs;
    private final int numExpenses;
    private final ArrayList<String> jobNameList = new ArrayList<>();

    public enum Day {
        Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    }

    public StatsRetriever(int userId) throws SQLException {
        this.userId = userId;
        this.numJobs = numJobs();
        this.numExpenses = numExpenses();
    }

    private int numJobs() throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(jobId) as numJobs FROM Job JOIN User ON " +
                "Job.userId = User.userId WHERE User.userId = ?");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("numJobs");
        }
        return -1;
    }

    private int numExpenses() throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(expenseId) as numExpenses FROM  Expense " +
                "JOIN User ON Expense.user = User.userId WHERE User.userId = ?");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("numExpenses");
        }
        return -1;
    }

    public ObservableList<PieChart.Data> earningsPieChartData() throws SQLException {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT jobName, SUM(amount) as sum FROM Job JOIN Log " +
                "ON Job.jobId = Log.jobId WHERE Job.userId = ? GROUP BY Job.jobId");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            pieChartData.add(new PieChart.Data(rs.getString("jobName"), rs.getDouble("sum")));
        }
        return pieChartData;
    }

    public ArrayList<XYChart.Series<String, Number>> earningsAreaChartData(LocalDate start, LocalDate end) throws SQLException {
        ArrayList<XYChart.Series<String, Number>> data = new ArrayList<>();

        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT jobName, jobId FROM Job WHERE userId = ?");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        jobNameList.clear();

        while (rs.next()) {
            jobNameList.add(rs.getString("jobName"));
            data.add(getEarningsSeries(rs.getInt("jobId"), start, end));
        }

        return data;
    }

    private XYChart.Series<String, Number> getEarningsSeries(int jobId, LocalDate start, LocalDate end) throws SQLException {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        Date startDateSQL = Date.valueOf(start);
        Date endDateSQL = Date.valueOf(end);

        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT date, amount FROM Log WHERE jobId = ? AND " +
                "date >= ? AND date <= ?");

        stmt.setInt(1, jobId);
        stmt.setDate(2, startDateSQL);
        stmt.setDate(3, endDateSQL);

        ResultSet rs = stmt.executeQuery();

        List<XYChart.Data<String, Number>> seriesData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        while (rs.next()) {
            LocalDate localDate = rs.getDate("date").toLocalDate();

            String formattedDate = localDate.format(formatter);
            seriesData.add(new XYChart.Data<>(formattedDate, rs.getDouble("amount")));
        }

        series.getData().addAll(seriesData);
        return series;
    }

    public double getAvgEarningsPerShift() throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT AVG(amount) as totalEarningsPerShift FROM Log WHERE jobId IN " +
                "(SELECT jobId FROM Job WHERE userId = ?)");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getDouble("totalEarningsPerShift");
        }
        return 0;
    }

    public double getAvgHourlyWage() throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT (SUM(amount) / SUM(hours)) as avgHourlyWage FROM" +
                " Log WHERE jobId IN (SELECT jobId FROM Job WHERE userId = ?)");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getDouble("avgHourlyWage");
        }
        return 0;
    }

    public LocalDate getEarliestLog() throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT MIN(date) as earliestLog FROM" +
                " Log WHERE jobID IN (SELECT jobId FROM Job WHERE userId = ?)");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getDate("earliestLog").toLocalDate();
        }
        return null;
    }

    //get average hourly per job
    public ArrayList<String> getAvgHourlyPerJob() throws SQLException {
        ArrayList<String> s = new ArrayList<>();
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT (SUM(amount)/SUM(hours)) as jobHourlyAvg, jobName " +
                "FROM Job JOIN Log on Job.jobId = Log.jobId WHERE userId = ? GROUP BY Job.jobId");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
             s.add(rs.getString("jobName")+": $"+String.format("%.2f", rs.getDouble("jobHourlyAvg"))+"\n");
        }
        return s;
    }

    //get average shift earnings per job
    public ArrayList<String> getAvgShiftEarningsPerJob() throws SQLException {
        ArrayList<String> s = new ArrayList<>();
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT (SUM(amount)/COUNT(hours)) as shiftAvg, jobName " +
                "FROM Job JOIN Log on Job.jobId = Log.jobId WHERE userId = ? GROUP BY Job.jobId");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            s.add(rs.getString("jobName")+": $"+String.format("%.2f", rs.getDouble("shiftAvg")));
        }
        return s;
    }

    public double getExpenseProgress(LocalDate first, LocalDate last) throws SQLException {
        double earn = getCurrentMonthEarnings(first, last);
        if(earn == 0){
            return 0.0;
        }

        double exp = getMonthlyExpenses();
        if(exp == 0){
            return 1.0;
        }

        return Math.min(earn/exp, 1.0);
    }
    public double getMonthlyExpenses() throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT SUM(amount) as totalExpenses FROM Expense " +
                "WHERE user = ? GROUP BY user");

        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getDouble("totalExpenses");
        }

        return 0.0;
    }

    public double getCurrentMonthEarnings(LocalDate first, LocalDate last) throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT SUM(amount) as totalEarnings FROM Log JOIN Job " +
                "ON Log.jobId = Job.jobId WHERE Job.userId = ? AND Log.date >= ? AND Log.date <= ?");

        stmt.setInt(1, userId);
        stmt.setDate(2, Date.valueOf(first));
        stmt.setDate(3, Date.valueOf(last));
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getDouble("totalEarnings");
        }

        return 0.0;
    }

    public ArrayList<XYChart.Series<String, Number>> getAvgEarningsPerWeekDayBarChartData() throws SQLException {
        ArrayList<XYChart.Series<String, Number>> data = new ArrayList<>();

        for(String job : jobNameList){
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(job);
            for(Day day : Day.values()){
                double avg = getJobsAvgEarningPerDayOfWeek(job, day);
                series.getData().add(new XYChart.Data<>(day.toString(), avg));
            }
            data.add(series);
        }

        return data;
    }

    public double getJobsAvgEarningPerDayOfWeek(String jobName, Day day) throws SQLException {
        Connection connection = Database.newConnection();
        PreparedStatement stmt = connection.prepareStatement("SELECT AVG(amount) as avgEarnings FROM Log JOIN Job " +
                "ON Log.jobId = Job.jobId WHERE Job.userId = ? AND Job.jobName = ? AND DAYOFWEEK(Log.date) = ?");

        stmt.setInt(1, userId);
        stmt.setString(2, jobName);
        stmt.setInt(3, day.ordinal()+1);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getDouble("avgEarnings");
        }

        return 0;
    }

    public int getNumJobs(){
        return numJobs;
    }

    public int getNumExpenses(){
        return numExpenses;
    }


    public ArrayList<String> getJobNameList() {
        return jobNameList;
    }
}
