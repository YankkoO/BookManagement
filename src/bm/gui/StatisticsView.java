package bm.gui;

import bm.dao.LibraryDAO;
import bm.model.Book;
import bm.model.ReadingStatus;
import bm.util.Utilities;

import java.util.Map;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

/**
 * Controller for the Statistics screen.
 *
 * <p>Aggregates data across all DAOs to display:
 * <ul>
 *   <li>Summary labels: total, read, unread, reading, average rating</li>
 *   <li>Pie chart broken down by genre</li>
 *   <li>Bar chart of books per author (top N)</li>
 *   <li>Bar chart of reading status distribution</li>
 * </ul>
 *
 * @author Gómez Nido Gonzalo
 */
public class StatisticsView {

    // ======================== INJECTED DAOs ========================

    private LibraryDAO libraryDAO;

    // ======================== FXML CONTROLS ========================

    @FXML private Label      totalBooksLabel;
    @FXML private Label      readBooksLabel;
    @FXML private Label      unreadBooksLabel;
    @FXML private Label      avgRatingLabel;
    @FXML private Label      readingBooksLabel;

    @FXML private PieChart                        genrePieChart;
    @FXML private BarChart<String, Number>        authorBarChart;
    @FXML private CategoryAxis                    authorAxis;
    @FXML private NumberAxis                      authorCountAxis;
    @FXML private BarChart<String, Number>        statusBarChart;
    @FXML private CategoryAxis                    statusAxis;
    @FXML private NumberAxis                      statusCountAxis;

    // ======================== LIFECYCLE ========================

    /**
     * Called automatically by JavaFX after FXML fields are injected.
     * Initialises all DAOs and triggers statistics calculation.
     */
    @FXML
    public void initialize() {
        libraryDAO = new LibraryDAO();
        computeStatistics();
    }

    // ======================== STATISTICS COMPUTATION ========================

    /**
     * Computes all statistics and updates the summary labels and charts.
     */
    @FXML
    public void computeStatistics() {
        totalBooksLabel.setText(Utilities.formatNumber(getTotalBooks()));
        readBooksLabel.setText(Utilities.formatNumber(getReadBooks()));
        unreadBooksLabel.setText(Utilities.formatNumber(getUnreadBooks()));
        readingBooksLabel.setText(Utilities.formatNumber(getReadingBooks()));
        avgRatingLabel.setText(String.format("%.2f / 5.00", getAverageRating()));
        renderCharts();
    }

    /**
     * Builds and populates all chart components.
     */
    public void renderCharts() {
        generateGenreChart();
        generateAuthorChart();
        generateStatusChart();
    }

    // ======================== DATA AGGREGATION ========================

    /**
     * Groups all books by literary genre and counts the books in each group.
     *
     * @return map of {@code genre → count}
     */
    public Map<String, Long> getBooksByGenre() {
        return libraryDAO.findAll().stream()
            .filter(b -> b.getGenre() != null && !b.getGenre().isBlank())
            .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()));
    }

    /**
     * Groups all books by author and counts the books per author (top 10).
     *
     * @return map of {@code author → count} (top 10 by count)
     */
    public Map<String, Long> getBooksByAuthor() {
        return libraryDAO.findAll().stream()
            .collect(Collectors.groupingBy(Book::getAuthor, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }

    /**
     * Counts books per reading status.
     *
     * @return map of {@code status.name() → count}
     */
    public Map<String, Long> getBooksByStatus() {
        return libraryDAO.findAll().stream()
            .filter(b -> b.getStatus() != null)
            .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));
    }

    /**
     * Calculates the average rating across all rated books (rating > 0).
     *
     * @return average rating, or 0.0 if no rated books exist
     */
    public double getAverageRating() {
        return libraryDAO.findAll().stream()
            .filter(b -> b.getRating() > 0)
            .mapToDouble(Book::getRating)
            .average()
            .orElse(0.0);
    }

    /**
     * Returns the total number of books in the library.
     *
     * @return total book count
     */
    public int getTotalBooks() {
        return libraryDAO.count();
    }

    /**
     * Returns the number of books with status {@code COMPLETED}.
     *
     * @return count of completed books
     */
    public int getReadBooks() {
        return (int) libraryDAO.findAll().stream()
            .filter(b -> b.getStatus() == ReadingStatus.COMPLETED)
            .count();
    }

    /**
     * Returns the number of books with status {@code NOT_READ}.
     *
     * @return count of unread books
     */
    public int getUnreadBooks() {
        return (int) libraryDAO.findAll().stream()
            .filter(b -> b.getStatus() == ReadingStatus.NOT_READ)
            .count();
    }

    /**
     * Returns the number of books currently being read.
     *
     * @return count of in-progress books
     */
    public int getReadingBooks() {
        return (int) libraryDAO.findAll().stream()
            .filter(b -> b.getStatus() == ReadingStatus.READING)
            .count();
    }

    // ======================== CHART BUILDERS ========================

    /**
     * Populates the {@code PieChart} with one slice per genre and its book count.
     */
    public void generateGenreChart() {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        getBooksByGenre().forEach((genre, count) ->
            pieData.add(new PieChart.Data(genre + " (" + count + ")", count)));
        genrePieChart.setData(pieData);
        genrePieChart.setTitle("Books by Genre");
        genrePieChart.setLegendVisible(true);
    }

    /**
     * Populates the {@code BarChart} with one bar per author (top 10 by book count).
     */
    public void generateAuthorChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Books");
        getBooksByAuthor().forEach((author, count) ->
            series.getData().add(new XYChart.Data<>(
                Utilities.truncate(author, 18), count)));
        authorBarChart.getData().setAll(java.util.List.of(series));
    }

    /**
     * Populates the status bar chart with a bar per reading status.
     */
    public void generateStatusChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Books");
        Map<String, Long> byStatus = getBooksByStatus();
        for (ReadingStatus s : ReadingStatus.values()) {
            long count = byStatus.getOrDefault(s.name(), 0L);
            series.getData().add(new XYChart.Data<>(formatStatus(s), count));
        }
        statusBarChart.getData().setAll(java.util.List.of(series));
    }

    // ======================== PRIVATE HELPERS ========================

    private String formatStatus(ReadingStatus s) {
        return switch (s) {
            case NOT_READ  -> "Not Read";
            case READING   -> "Reading";
            case COMPLETED -> "Completed";
            case ABANDONED -> "Abandoned";
        };
    }
}
