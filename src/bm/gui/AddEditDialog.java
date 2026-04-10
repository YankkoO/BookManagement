package bm.gui;

import java.util.ArrayList;
import java.util.List;

import bm.dao.LibraryDAO;
import bm.model.Book;
import bm.model.ReadingStatus;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the Add / Edit Book dialog window.
 *
 * <p>When opened in <em>add mode</em> ({@link #setMode(Book)} called with
 * {@code null}), all fields are blank and a new book is inserted on save.
 * When opened in <em>edit mode</em>, fields are pre-populated and the
 * existing record is updated on save.</p>
 *
 * @author Gómez Nido Gonzalo
 */
public class AddEditDialog {

    // ======================== INJECTED DAO ========================

    private LibraryDAO libraryDAO;

    /** The book being edited, or {@code null} when adding a new book. */
    private Book book;

    // ======================== FXML CONTROLS ========================

    @FXML private Label     dialogTitleLabel;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField yearField;
    @FXML private TextField pagesField;
    @FXML private TextField publisherField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label     errorLabel;
    @FXML private Button    saveButton;
    @FXML private Button    cancelButton;

    // ======================== LIFECYCLE ========================

    /**
     * Called automatically by JavaFX after FXML injection.
     * Initialises the DAO and populates combo boxes.
     */
    @FXML
    public void initialize() {
        libraryDAO = new LibraryDAO();

        genreCombo.getItems().addAll(
            "Fiction", "Non-Fiction", "Science", "History",
            "Fantasy", "Biography", "Mystery", "Romance",
            "Self-Help", "Horror", "Adventure", "Other"
        );

        for (ReadingStatus s : ReadingStatus.values()) {
            statusCombo.getItems().add(s.name());
        }
        statusCombo.setValue(ReadingStatus.NOT_READ.name());
    }

    // ======================== MODE SETUP ========================

    /**
     * Configures the dialog for add or edit mode.
     * Must be called by the opener <em>before</em> the dialog is shown.
     *
     * @param book the book to edit, or {@code null} for add mode
     */
    public void setMode(Book book) {
        this.book = book;
        if (book == null) {
            dialogTitleLabel.setText("Add New Book");
            saveButton.setText("Save Book");
        } else {
            dialogTitleLabel.setText("Edit Book");
            saveButton.setText("Update Book");
            loadData(book);
        }
    }

    // ======================== DATA LOADING ========================

    /**
     * Pre-fills all form fields with data from an existing book (edit mode).
     *
     * @param book the book whose data will be loaded (must not be null)
     */
    public void loadData(Book book) {
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        yearField.setText(String.valueOf(book.getYear()));
        pagesField.setText(String.valueOf(book.getPages()));
        genreCombo.setValue(book.getGenre());
        publisherField.setText(book.getPublisher() != null ? book.getPublisher() : "");
        statusCombo.setValue(book.getStatus() != null ? book.getStatus().name() : ReadingStatus.NOT_READ.name());
    }

    // ======================== ACTIONS ========================

    /**
     * Validates the form and, if valid, persists the book (insert or update)
     * then closes the dialog.
     */
    @FXML
    public void save() {
        List<String> errors = new ArrayList<>();
        if (!validateForm(errors)) {
            showErrors(errors);
            return;
        }

        String title     = titleField.getText().trim();
        String author    = authorField.getText().trim();
        int    year      = Integer.parseInt(yearField.getText().trim());
        int    pages     = Integer.parseInt(pagesField.getText().trim());
        String genre     = genreCombo.getValue() != null ? genreCombo.getValue() : "Other";
        String publisher = publisherField.getText().trim();
        ReadingStatus status = ReadingStatus.valueOf(statusCombo.getValue());

        if (book == null) {
            // Add mode — no-args partial constructor then insert
            Book newBook = new Book(title, author, year, pages, genre, publisher);
            newBook.setStatus(status);
            libraryDAO.insert(newBook);
        } else {
            // Edit mode — mutate accessible fields and update
            // Note: Book only exposes setRating() and setStatus() publicly.
            // The canonical approach is to call libraryDAO.update() with a
            // freshly-constructed Book carrying the known id.
            Book updated = new Book(
                book.getId(), title, author, year, pages, genre, publisher,
                book.getRating(), status,
                book.getDateAdded(), book.getDateCompleted()
            );
            libraryDAO.update(updated);
        }

        closeWindow();
    }

    /**
     * Closes the dialog without saving any changes.
     */
    @FXML
    public void cancel() {
        closeWindow();
    }

    /**
     * Clears all input fields and resets combo boxes to their default state.
     */
    public void clearFields() {
        titleField.clear();
        authorField.clear();
        yearField.clear();
        pagesField.clear();
        publisherField.clear();
        genreCombo.getSelectionModel().clearSelection();
        statusCombo.setValue(ReadingStatus.NOT_READ.name());
        errorLabel.setText("");
    }

    // ======================== VALIDATION ========================

    /**
     * Validates all required form fields and populates the given error list.
     *
     * @param errors mutable list that will be populated with validation messages
     * @return true if there are no errors
     */
    public boolean validateForm(List<String> errors) {
        if (titleField.getText().isBlank())   errors.add("Title is required.");
        if (authorField.getText().isBlank())  errors.add("Author is required.");

        String yearStr  = yearField.getText().trim();
        String pagesStr = pagesField.getText().trim();

        if (yearStr.isBlank()) {
            errors.add("Year is required.");
        } else {
            try {
                int y = Integer.parseInt(yearStr);
                if (y <= 0) errors.add("Year must be a positive number.");
            } catch (NumberFormatException e) {
                errors.add("Year must be a valid integer (e.g. 1984).");
            }
        }

        if (pagesStr.isBlank()) {
            errors.add("Pages is required.");
        } else {
            try {
                int p = Integer.parseInt(pagesStr);
                if (p <= 0) errors.add("Pages must be a positive number.");
            } catch (NumberFormatException e) {
                errors.add("Pages must be a valid integer.");
            }
        }

        return errors.isEmpty();
    }

    /**
     * Displays validation error messages in the error label below the form.
     *
     * @param errors list of human-readable error strings
     */
    public void showErrors(List<String> errors) {
        errorLabel.setText(String.join("\n", errors));
    }

    // ======================== PRIVATE HELPERS ========================

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
