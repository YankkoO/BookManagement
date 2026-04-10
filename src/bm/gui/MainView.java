package bm.gui;

import bm.dao.LibraryDAO;
import bm.model.Book;
import bm.model.ReadingStatus;
import bm.util.Utilities;

import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for the main application window.
 *
 * <p>
 * Manages book table display, inline search, live navigation between panels
 * (library, favourites, filters, statistics), and delegates CRUD dialogs.
 *
 * @author Gómez Nido Gonzalo
 */
public class MainView {

    // ======================== INJECTED DAO ========================

    private LibraryDAO libraryDAO;

    /** Full book list cached to allow client-side search filtering. */
    private ObservableList<Book> allBooks = FXCollections.observableArrayList();

    // ======================== FXML — TABLE ========================

    @FXML private TableView<Book>          bookTable;
    @FXML private TableColumn<Book, String>  colTitle;
    @FXML private TableColumn<Book, String>  colAuthor;
    @FXML private TableColumn<Book, String>  colGenre;
    @FXML private TableColumn<Book, Integer> colYear;
    @FXML private TableColumn<Book, Integer> colPages;
    @FXML private TableColumn<Book, String>  colStatus;
    @FXML private TableColumn<Book, Double>  colRating;
    @FXML private TableColumn<Book, String>  colPublisher;

    // ======================== FXML — TOOLBAR ========================

    @FXML private TextField searchInput;
    @FXML private Button    addButton;
    @FXML private Button    editButton;
    @FXML private Button    deleteButton;
    @FXML private Button    detailsButton;
    @FXML private Label     resultCountLabel;
    @FXML private Label     libraryCountLabel;

    // ======================== FXML — NAVIGATION ========================

    @FXML private Button navLibrary;
    @FXML private Button navFavourites;
    @FXML private Button navFilters;
    @FXML private Button navStats;

    // ======================== FXML — PANEL STACK ========================

    @FXML private javafx.scene.layout.VBox libraryPanel;
    @FXML private javafx.scene.layout.VBox favouritesPanel;
    @FXML private javafx.scene.layout.VBox filtersPanel;
    @FXML private javafx.scene.layout.VBox statisticsPanel;

    // ======================== EMBEDDED SUB-CONTROLLERS ========================

    private FiltersView    filtersController;
    private FavouritesView favouritesController;
    private StatisticsView statisticsController;

    // ======================== LIFECYCLE ========================

    /**
     * Called automatically by JavaFX after FXML fields are injected.
     * Initialises the DAO, configures table columns, and loads all books.
     */
    @FXML
    public void initialize() {
        libraryDAO = new LibraryDAO();
        configureColumns();
        loadBooks();
        updateLibraryCount();

        // Double-click on a row opens the details window
        bookTable.setRowFactory(tv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetails();
                }
            });
            return row;
        });
    }

    // ======================== COLUMN SETUP ========================

    /**
     * Wires each {@code TableColumn} to the corresponding {@link Book} property
     * and configures custom cell factories for status badges and rating stars.
     */
    private void configureColumns() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colPages.setCellValueFactory(new PropertyValueFactory<>("pages"));
        colPublisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));

        colStatus.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStatus() != null
                    ? data.getValue().getStatus().name() : ""));

        // Status column — coloured badge labels
        colStatus.setCellFactory(col -> new TableCell<Book, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null); setText(null);
                    return;
                }
                Label badge = new Label(formatStatus(status));
                badge.getStyleClass().addAll("badge", badgeStyle(status));
                setGraphic(badge);
                setText(null);
            }
        });

        // Rating column — star text
        colRating.setCellFactory(col -> new TableCell<Book, Double>() {
            @Override
            protected void updateItem(Double rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) { setText(null); return; }
                setText(buildStarText(rating) + String.format(" %.1f", rating));
            }
        });
    }

    // ======================== DATA LOADING ========================

    /**
     * Loads all books from the database, caches them, and populates the table.
     */
    @FXML
    public void loadBooks() {
        allBooks.setAll(libraryDAO.findAll());
        displayBooks(allBooks);
        updateLibraryCount();
    }

    /**
     * Populates the {@code TableView} with the given list of books and updates
     * the result count label.
     *
     * @param books the books to display (may be empty)
     */
    public void displayBooks(List<Book> books) {
        bookTable.setItems(FXCollections.observableArrayList(books));
        resultCountLabel.setText(books.size() + " book" + (books.size() == 1 ? "" : "s"));
    }

    /**
     * Refreshes the table and all counters from the database.
     * Called after add, edit, or delete operations.
     */
    public void refreshTable() {
        loadBooks();
        updateLibraryCount();
        
        if (favouritesController != null) favouritesController.refreshList();
        if (statisticsController != null) statisticsController.computeStatistics();
        if (filtersController != null) filtersController.loadGenres();
    }

    private void updateLibraryCount() {
        int total = libraryDAO.count();
        libraryCountLabel.setText(total + " book" + (total == 1 ? "" : "s") + " in library");
    }

    // ======================== SEARCH ========================

    /**
     * Called on every key release in the search field.
     * Performs a live, client-side filter against title and author.
     */
    @FXML
    public void onSearchTyped() {
        filterSearch(searchInput.getText().trim());
    }

    /**
     * Clears the search field and restores the full list.
     */
    @FXML
    public void clearSearch() {
        searchInput.clear();
        displayBooks(allBooks);
    }

    /**
     * Filters the in-memory book list by title or author containing the query.
     * Shows all books when the query is blank.
     *
     * @param query the text the user typed
     */
    public void filterSearch(String query) {
        if (query.isEmpty()) {
            displayBooks(allBooks);
            return;
        }
        String lower = query.toLowerCase();
        List<Book> results = allBooks.stream()
            .filter(b -> b.getTitle().toLowerCase().contains(lower)
                      || b.getAuthor().toLowerCase().contains(lower))
            .collect(Collectors.toList());
        displayBooks(results);
    }

    // ======================== NAVIGATION ========================

    /** Shows the main library panel and highlights the nav button. */
    @FXML
    public void showLibrary() {
        setActivePanel(libraryPanel, navLibrary);
    }

    /** Shows (or loads) the favourites panel. */
    @FXML
    public void showFavourites() {
        if (!favouritesPanel.isVisible() || favouritesPanel.getChildren().isEmpty()) {
            favouritesController = (FavouritesView) loadEmbeddedView("/fxml/FavouritesView.fxml", favouritesPanel);
        } else if (favouritesController != null) {
            favouritesController.refreshList();
        }
        setActivePanel(favouritesPanel, navFavourites);
    }

    /** Shows (or loads) the filters panel. */
    @FXML
    public void showFilters() {
        if (!filtersPanel.isVisible() || filtersPanel.getChildren().isEmpty()) {
            filtersController = (FiltersView) loadEmbeddedView("/fxml/FiltersView.fxml", filtersPanel);
            if (filtersController != null) {
                filtersController.setResultCallback(this::displayBooks);
            }
        }
        setActivePanel(filtersPanel, navFilters);
    }

    /** Shows (or loads) the statistics panel. */
    @FXML
    public void showStatistics() {
        if (!statisticsPanel.isVisible() || statisticsPanel.getChildren().isEmpty()) {
            statisticsController = (StatisticsView) loadEmbeddedView("/fxml/StatisticsView.fxml", statisticsPanel);
        } else if (statisticsController != null) {
            statisticsController.computeStatistics();
        }
        setActivePanel(statisticsPanel, navStats);
    }

    /**
     * Hides all content panels, makes the target one visible,
     * and updates sidebar nav-button styles.
     *
     * @param target    the VBox panel to show
     * @param activeNav the nav button to highlight
     */
    private void setActivePanel(javafx.scene.layout.VBox target, Button activeNav) {
        libraryPanel.setVisible(false);   libraryPanel.setManaged(false);
        favouritesPanel.setVisible(false); favouritesPanel.setManaged(false);
        filtersPanel.setVisible(false);    filtersPanel.setManaged(false);
        statisticsPanel.setVisible(false); statisticsPanel.setManaged(false);

        target.setVisible(true);
        target.setManaged(true);

        // Update active nav style
        for (Button btn : new Button[]{navLibrary, navFavourites, navFilters, navStats}) {
            btn.getStyleClass().removeAll("nav-item-active");
            if (!btn.getStyleClass().contains("nav-item")) btn.getStyleClass().add("nav-item");
        }
        activeNav.getStyleClass().add("nav-item-active");
    }

    private Object loadEmbeddedView(String fxmlPath,
                                  javafx.scene.layout.VBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            javafx.scene.layout.VBox.setVgrow(node, javafx.scene.layout.Priority.ALWAYS);
            container.getChildren().setAll(node);
            return loader.getController();
        } catch (Exception e) {
            Utilities.showAlert("Navigation Error",
                "Could not load view: " + fxmlPath + "\n" + e.getMessage());
            return null;
        }
    }

    // ======================== CRUD DIALOGS ========================

    /**
     * Opens the {@link AddEditDialog} in "add new book" mode.
     */
    @FXML
    public void openAddDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/AddEditDialog.fxml"));
            Parent root = loader.load();
            AddEditDialog controller = loader.getController();
            controller.setMode(null);          // null → add mode

            Stage dialog = buildDialog(root, "Add New Book");
            dialog.showAndWait();
            refreshTable();
        } catch (Exception e) {
            Utilities.showAlert("Error", "Could not open Add dialog:\n" + e.getMessage());
        }
    }

    /**
     * Opens the {@link AddEditDialog} pre-filled with the selected book (edit mode).
     * Shows a warning if no book is selected.
     */
    @FXML
    public void openEditDialog() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utilities.showAlert("No Selection", "Please select a book to edit.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/AddEditDialog.fxml"));
            Parent root = loader.load();
            AddEditDialog controller = loader.getController();
            controller.setMode(selected);       // non-null → edit mode

            Stage dialog = buildDialog(root, "Edit Book");
            dialog.showAndWait();
            refreshTable();
        } catch (Exception e) {
            Utilities.showAlert("Error", "Could not open Edit dialog:\n" + e.getMessage());
        }
    }

    /**
     * Deletes the currently selected book after confirmation.
     */
    @FXML
    public void deleteSelected() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean confirm = Utilities.showConfirmation(
            "Delete \"" + selected.getTitle() + "\"?\nThis action cannot be undone.");
        if (confirm) {
            libraryDAO.delete(selected.getId());
            refreshTable();
        }
    }

    /**
     * Opens the {@link DetailsView} window for the currently selected book.
     */
    @FXML
    public void openDetails() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utilities.showAlert("No Selection", "Please select a book to view details.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/DetailsView.fxml"));
            Parent root = loader.load();
            DetailsView controller = loader.getController();
            controller.loadBook(selected.getId());

            Stage stage = new Stage();
            stage.setTitle("Details — " + selected.getTitle());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets()
                 .add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setOnHiding(e -> refreshTable());
            stage.show();
        } catch (Exception e) {
            Utilities.showAlert("Error", "Could not open Details:\n" + e.getMessage());
        }
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Creates and configures a modal {@link Stage} for dialogs.
     *
     * @param root  the scene root
     * @param title window title
     * @return the configured Stage (not yet shown)
     */
    private Stage buildDialog(Parent root, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setResizable(false);
        return stage;
    }

    /**
     * Converts a {@link ReadingStatus} name to a human-readable label.
     *
     * @param status the status name string
     * @return human readable string
     */
    private String formatStatus(String status) {
        return switch (status) {
            case "NOT_READ"  -> "Not Read";
            case "READING"   -> "Reading";
            case "COMPLETED" -> "Completed";
            case "ABANDONED" -> "Abandoned";
            default          -> status;
        };
    }

    /**
     * Returns the CSS style class for the status badge.
     *
     * @param status the status name string
     * @return CSS class name
     */
    private String badgeStyle(String status) {
        return switch (status) {
            case "COMPLETED" -> "badge-completed";
            case "READING"   -> "badge-reading";
            case "ABANDONED" -> "badge-abandoned";
            default          -> "badge-not-read";
        };
    }

    /**
     * Builds a star-text representation of a rating (e.g. "★★★☆☆").
     *
     * @param rating rating value (0.0–5.0)
     * @return a string of ★ and ☆ characters
     */
    private String buildStarText(double rating) {
        int filled = (int) Math.round(rating);
        return "★".repeat(filled) + "☆".repeat(5 - filled);
    }
}
