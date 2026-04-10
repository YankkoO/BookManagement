package bm.gui;

import bm.dao.LibraryDAO;
import bm.model.Book;
import bm.model.ReadingStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the Filters panel.
 *
 * <p>Allows the user to narrow down the book list by genre, reading status,
 * publication year range, page count, and minimum rating. Results are pushed
 * back to the main library by delegating to {@link MainView} (or via a shared
 * callback — here the results are applied to an embedded TableView or printed
 * to a results label for simplicity until MainView wires a callback).</p>
 *
 * @author Gómez Nido Gonzalo
 */
public class FiltersView {

    // ======================== INJECTED DAO ========================

    private LibraryDAO libraryDAO;

    // ======================== FXML CONTROLS ========================

    @FXML private ComboBox<String>  genreCombo;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private Spinner<Integer>  yearMinSpinner;
    @FXML private Spinner<Integer>  yearMaxSpinner;
    @FXML private Slider            pagesSlider;
    @FXML private Label             pagesSliderLabel;
    @FXML private Slider            minRatingSlider;
    @FXML private Label             ratingSliderLabel;
    @FXML private Label             filterResultCount;

    /** Optional callback set by MainView to receive filtered results. */
    private java.util.function.Consumer<List<Book>> resultCallback;

    // ======================== LIFECYCLE ========================

    /**
     * Called automatically by JavaFX after FXML fields are injected.
     * Initialises the DAO and populates filter controls.
     */
    @FXML
    public void initialize() {
        libraryDAO = new LibraryDAO();

        // Populate status options
        statusCombo.getItems().add("ALL");
        for (ReadingStatus s : ReadingStatus.values()) {
            statusCombo.getItems().add(s.name());
        }
        statusCombo.setValue("ALL");

        // Spinner factories
        yearMinSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 2100, 1900));
        yearMaxSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1000, 2100, 2025));

        // Live slider labels
        pagesSlider.valueProperty().addListener((obs, old, val) ->
            pagesSliderLabel.setText((int) val.doubleValue() + " pages"));
        minRatingSlider.valueProperty().addListener((obs, old, val) ->
            ratingSliderLabel.setText(String.format("%.1f ★", val.doubleValue())));

        // Load genres from DB
        loadGenres();
    }

    // ======================== DATA LOADING ========================

    /**
     * Loads distinct genre values from the database and populates the genre combo.
     */
    public void loadGenres() {
        genreCombo.getItems().clear();
        genreCombo.getItems().add("ALL");

        // Derive distinct genres from all books
        libraryDAO.findAll().stream()
            .map(Book::getGenre)
            .filter(g -> g != null && !g.isBlank())
            .distinct()
            .sorted()
            .forEach(genreCombo.getItems()::add);

        genreCombo.setValue("ALL");
    }

    // ======================== ACTIONS ========================

    /**
     * Reads all active filter controls, applies them against the full book list,
     * and forwards the results via the result callback (if set) or updates the
     * count label.
     */
    @FXML
    public void applyFilters() {
        List<Book> results = libraryDAO.findAll();

        String genre  = genreCombo.getValue();
        String status = statusCombo.getValue();
        int    yMin   = yearMinSpinner.getValue();
        int    yMax   = yearMaxSpinner.getValue();
        int    maxPg  = (int) pagesSlider.getValue();
        double minRat = minRatingSlider.getValue();

        if (genre  != null && !genre.equals("ALL"))
            results = results.stream().filter(b -> genre.equals(b.getGenre())).collect(Collectors.toList());

        if (status != null && !status.equals("ALL"))
            results = results.stream().filter(b -> b.getStatus() != null && b.getStatus().name().equals(status)).collect(Collectors.toList());

        results = results.stream()
            .filter(b -> b.getYear() >= yMin && b.getYear() <= yMax)
            .filter(b -> b.getPages() <= maxPg)
            .filter(b -> b.getRating() >= minRat)
            .collect(Collectors.toList());

        String msg = results.size() + " book" + (results.size() == 1 ? "" : "s") + " match your filters.";
        filterResultCount.setText(msg);

        updateResults(results);
    }

    /**
     * Resets all filter controls to their default state and reloads all books.
     */
    @FXML
    public void clearFilters() {
        genreCombo.setValue("ALL");
        statusCombo.setValue("ALL");
        yearMinSpinner.getValueFactory().setValue(1900);
        yearMaxSpinner.getValueFactory().setValue(2025);
        pagesSlider.setValue(pagesSlider.getMax());
        minRatingSlider.setValue(0);
        filterResultCount.setText("");

        updateResults(libraryDAO.findAll());
    }

    // ======================== FILTER STATE ========================

    /**
     * Builds a map of currently active (non-default) filter values.
     *
     * @return map of filter key → value
     */
    public Map<String, Object> getActiveFilters() {
        Map<String, Object> filters = new HashMap<>();
        String genre  = genreCombo.getValue();
        String status = statusCombo.getValue();
        if (genre  != null && !genre.equals("ALL"))   filters.put("genre",  genre);
        if (status != null && !status.equals("ALL"))  filters.put("status", status);
        filters.put("yearMin",   yearMinSpinner.getValue());
        filters.put("yearMax",   yearMaxSpinner.getValue());
        filters.put("maxPages",  (int) pagesSlider.getValue());
        filters.put("minRating", minRatingSlider.getValue());
        return filters;
    }

    /**
     * Returns whether at least one filter is currently active.
     *
     * @return true if any non-default filter has been set
     */
    public boolean hasActiveFilters() {
        return !getActiveFilters().isEmpty();
    }

    /**
     * Registers a callback that receives the filtered book list whenever filters are applied.
     * Called by {@link MainView} so that results are reflected in the main table.
     *
     * @param callback consumer accepting the filtered list
     */
    public void setResultCallback(java.util.function.Consumer<List<Book>> callback) {
        this.resultCallback = callback;
    }

    /**
     * Forwards the filtered book list to the registered callback (if any).
     *
     * @param books the filtered list of books to display
     */
    public void updateResults(List<Book> books) {
        if (resultCallback != null) {
            resultCallback.accept(books);
        }
    }
}
