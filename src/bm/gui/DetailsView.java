package bm.gui;

import bm.dao.HistoryDAO;
import bm.dao.LibraryDAO;
import bm.dao.ReviewDAO;
import bm.model.Book;
import bm.model.ReadingStatus;
import bm.model.Review;
import bm.util.Utilities;

import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the book detail window.
 *
 * <p>Displays complete information about a selected book including star rating,
 * book metadata, and user reviews. Allows adding new reviews, changing the
 * rating interactively, and marking the book as read.
 *
 * @author Gómez Nido Gonzalo
 */
public class DetailsView {

    // ======================== INJECTED DAOs ========================

    private LibraryDAO libraryDAO;
    private ReviewDAO  reviewDAO;
    private HistoryDAO historyDAO;

    /** The ID of the book currently being displayed. */
    private int bookId;

    // ======================== FXML CONTROLS ========================

    @FXML private Label    headerLabel;
    @FXML private Label    titleLabel;
    @FXML private Label    authorLabel;
    @FXML private HBox     starRatingBox;
    @FXML private Label    ratingValueLabel;
    @FXML private Label    statusBadge;
    @FXML private Button   markReadButton;
    @FXML private Button   backButton;

    @FXML private Label    genreLabel;
    @FXML private Label    publisherLabel;
    @FXML private Label    yearLabel;
    @FXML private Label    pagesLabel;
    @FXML private Label    dateAddedLabel;
    @FXML private Label    dateCompletedLabel;
    @FXML private Label    daysInLibraryLabel;

    @FXML private TextArea         reviewTextArea;
    @FXML private Spinner<Integer> reviewRatingSpinner;
    @FXML private ListView<Review> reviewList;

    // ======================== LIFECYCLE ========================

    /**
     * Called automatically by JavaFX after FXML fields are injected.
     * Initialises all three DAOs.
     */
    @FXML
    public void initialize() {
        libraryDAO = new LibraryDAO();
        reviewDAO  = new ReviewDAO();
        historyDAO = new HistoryDAO();

        // Configure spinner value factory if not set by FXML
        if (reviewRatingSpinner.getValueFactory() == null) {
            reviewRatingSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 8));
        }

        // Custom review cell
        reviewList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); return; }

                Label rating  = new Label(r.getRating() + "/10"
                    + (r.isFavourite() ? " ❤️" : ""));
                rating.setStyle("-fx-text-fill:#FBBF24; -fx-font-weight:600; -fx-font-size:12px;");

                Label date    = new Label(Utilities.formatDate(r.getDate()));
                date.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");

                Label content = new Label(r.getContent());
                content.setWrapText(true);
                content.setStyle("-fx-text-fill:#E2E8F0; -fx-font-size:13px;");

                Button delBtn = new Button("✕");
                delBtn.getStyleClass().add("btn-icon");
                delBtn.setOnAction(e -> deleteReview(r.getId()));

                Button favBtn = new Button(r.isFavourite() ? "💔" : "❤");
                favBtn.getStyleClass().add("btn-icon");
                favBtn.setOnAction(e -> toggleFavourite(r));

                HBox meta = new HBox(8, rating, date);
                meta.setAlignment(Pos.CENTER_LEFT);

                HBox actions = new HBox(4, favBtn, delBtn);
                actions.setAlignment(Pos.CENTER_RIGHT);

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                HBox topRow = new HBox(meta, spacer, actions);

                javafx.scene.layout.VBox box =
                    new javafx.scene.layout.VBox(4, topRow, content);
                box.setStyle("-fx-padding:8 0;");
                setGraphic(box);
                setText(null);
            }
        });
    }

    // ======================== DATA LOADING ========================

    /**
     * Loads the book identified by the given ID and refreshes all UI sections.
     * Call this method right after the controller is obtained from FXMLLoader.
     *
     * @param id database ID of the book to display
     */
    public void loadBook(int id) {
        this.bookId = id;
        Book book = libraryDAO.findById(id);
        if (book == null) return;

        headerLabel.setText(book.getTitle());
        titleLabel.setText(book.getTitle());
        authorLabel.setText("by " + book.getAuthor());
        genreLabel.setText(orDash(book.getGenre()));
        publisherLabel.setText(orDash(book.getPublisher()));
        yearLabel.setText(String.valueOf(book.getYear()));
        pagesLabel.setText(book.getPages() + " pages");
        dateAddedLabel.setText(Utilities.formatDate(book.getDateAdded()));
        dateCompletedLabel.setText(Utilities.formatDate(book.getDateCompleted()));
        daysInLibraryLabel.setText(book.getDaysInLibrary() + " days");

        renderStars(book.getRating());
        ratingValueLabel.setText(String.format("%.1f / 5.0", book.getRating()));
        updateStatusBadge(book.getStatus());
        updateMarkReadButton(book.getStatus());

        loadReviews();
    }

    /**
     * Loads and displays all reviews for the current book.
     */
    public void loadReviews() {
        List<Review> reviews = reviewDAO.findByBook(bookId);
        reviewList.getItems().setAll(reviews);
    }

    // ======================== ACTIONS ========================

    /**
     * Saves a new star rating for the current book and refreshes the stars.
     *
     * @param rating user-selected rating (1.0–5.0)
     */
    public void saveRating(double rating) {
        if (!Utilities.isValidRating(rating)) return;
        libraryDAO.updateRating(bookId, rating);
        renderStars(rating);
        ratingValueLabel.setText(String.format("%.1f / 5.0", rating));
    }

    /** Convenience rate methods wired to the 1–5 star buttons in the FXML. */
    @FXML public void rate1() { saveRating(1.0); }
    @FXML public void rate2() { saveRating(2.0); }
    @FXML public void rate3() { saveRating(3.0); }
    @FXML public void rate4() { saveRating(4.0); }
    @FXML public void rate5() { saveRating(5.0); }

    /**
     * Creates and persists a new review from the text area input.
     * Clears the input and refreshes the review list on success.
     */
    @FXML
    public void addReview() {
        String content = reviewTextArea.getText().trim();
        if (content.isEmpty()) {
            Utilities.showAlert("Warning", "Please write something before adding a review.");
            return;
        }
        int reviewRating = reviewRatingSpinner.getValue();
        Review newReview = new Review(bookId, content, reviewRating);
        reviewDAO.insert(newReview);
        reviewTextArea.clear();
        loadReviews();
    }

    /**
     * Deletes the specified review after user confirmation and refreshes the list.
     *
     * @param reviewId database ID of the review to delete
     */
    public void deleteReview(int reviewId) {
        boolean ok = Utilities.showConfirmation("Delete this review?");
        if (ok) {
            reviewDAO.delete(reviewId);
            loadReviews();
        }
    }

    /**
     * Toggles the favourite status of a review and refreshes the list.
     *
     * @param review the review to toggle
     */
    public void toggleFavourite(Review review) {
        reviewDAO.setFavourite(review.getId(), !review.isFavourite());
        loadReviews();
    }

    /**
     * Marks the current book as {@code COMPLETED}, updating status in the database.
     */
    @FXML
    public void markAsRead() {
        libraryDAO.updateStatus(bookId, ReadingStatus.COMPLETED.name());
        historyDAO.completeReading(bookId);
        loadBook(bookId);  // full refresh
    }

    /**
     * Closes the details window.
     */
    @FXML
    public void closeWindow() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    // ======================== UI HELPERS ========================

    /**
     * Renders the star rating display (5 stars, filled vs. empty) in {@code starRatingBox}.
     *
     * @param rating rating value (0.0–5.0)
     */
    public void renderStars(double rating) {
        starRatingBox.getChildren().clear();
        int filled = (int) Math.round(rating);
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= filled ? "★" : "☆");
            star.getStyleClass().add(i <= filled ? "star-filled" : "star-empty");
            starRatingBox.getChildren().add(star);
        }
    }

    /**
     * Updates the status badge label's text and CSS class.
     *
     * @param status the book's current reading status
     */
    private void updateStatusBadge(ReadingStatus status) {
        statusBadge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
        if (status == null) {
            statusBadge.setText("NOT READ");
            statusBadge.getStyleClass().add("badge-not-read");
            return;
        }
        switch (status) {
            case COMPLETED -> { statusBadge.setText("Completed");  statusBadge.getStyleClass().add("badge-completed"); }
            case READING   -> { statusBadge.setText("Reading");    statusBadge.getStyleClass().add("badge-reading");   }
            case ABANDONED -> { statusBadge.setText("Abandoned");  statusBadge.getStyleClass().add("badge-abandoned");  }
            default        -> { statusBadge.setText("Not Read");   statusBadge.getStyleClass().add("badge-not-read");   }
        }
    }

    /**
     * Shows or hides the "Mark as Read" button based on whether the book is already completed.
     *
     * @param status current reading status
     */
    private void updateMarkReadButton(ReadingStatus status) {
        markReadButton.setVisible(status != ReadingStatus.COMPLETED);
        markReadButton.setManaged(status != ReadingStatus.COMPLETED);
    }

    /**
     * Returns the given string or "—" if it is null or blank.
     *
     * @param value the string to check
     * @return the value or "—"
     */
    private String orDash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }
}
