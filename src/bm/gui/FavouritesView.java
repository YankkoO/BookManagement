package bm.gui;

import java.util.List;

import bm.dao.LibraryDAO;
import bm.dao.ReviewDAO;
import bm.model.Book;
import bm.model.Review;
import bm.util.Utilities;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Controller for the Favourites view.
 *
 * <p>Displays all reviews that the user has marked as favourites,
 * allowing them to remove the favourite mark or view the associated book's
 * details in a popup.</p>
 *
 * @author Gómez Nido Gonzalo
 */
public class FavouritesView {

    // ======================== INJECTED DAOs ========================

    private ReviewDAO  reviewDAO;
    private LibraryDAO libraryDAO;

    // ======================== FXML CONTROLS ========================

    @FXML private ListView<Review> favouritesList;
    @FXML private Label            favCountLabel;
    @FXML private VBox             emptyState;

    // ======================== LIFECYCLE ========================

    /**
     * Called automatically by JavaFX after FXML fields are injected.
     * Initialises both DAOs, configures the list cell factory, and loads favourites.
     */
    @FXML
    public void initialize() {
        reviewDAO  = new ReviewDAO();
        libraryDAO = new LibraryDAO();

        // Custom cell factory: shows book title, review content, rating, and date
        favouritesList.setCellFactory(lv -> new ListCell<Review>() {
            @Override
            protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); return; }

                Book book = libraryDAO.findById(r.getBookId());
                String bookTitle = (book != null) ? book.getTitle() : "(Unknown Book)";

                Label title = new Label(bookTitle);
                title.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#E2E8F0;");

                Label rating = new Label("★ " + r.getRating() + "/10");
                rating.setStyle("-fx-text-fill:#FBBF24; -fx-font-size:12px; -fx-font-weight:600;");

                Label date = new Label(Utilities.formatDate(r.getDate()));
                date.setStyle("-fx-font-size:11px; -fx-text-fill:#475569;");

                Label content = new Label(Utilities.truncate(r.getContent(), 160));
                content.setWrapText(true);
                content.setStyle("-fx-text-fill:#94A3B8; -fx-font-size:12px;");

                Button removeBtn = new Button("💔 Remove");
                removeBtn.getStyleClass().add("btn-icon");
                removeBtn.setStyle("-fx-text-fill:#F87171;");
                removeBtn.setOnAction(e -> removeFavourite(r.getId()));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox topRow = new HBox(8, rating, date, spacer, removeBtn);
                topRow.setAlignment(Pos.CENTER_LEFT);

                VBox box = new VBox(4, title, topRow, content);
                box.setStyle("-fx-padding:10 0;");
                setGraphic(box);
                setText(null);
            }
        });

        loadFavourites();
    }

    // ======================== DATA LOADING ========================

    /**
     * Loads all favourite reviews from the database and populates the list view.
     * Shows an empty state if there are no favourites.
     */
    public void loadFavourites() {
        List<Review> favourites = reviewDAO.findFavourites();
        favouritesList.getItems().setAll(favourites);

        int count = favourites.size();
        favCountLabel.setText(count + " favourite" + (count == 1 ? "" : "s"));

        // Show/hide empty state
        boolean empty = favourites.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        favouritesList.setVisible(!empty);
        favouritesList.setManaged(!empty);
    }

    // ======================== ACTIONS ========================

    /**
     * Removes the favourite mark from the specified review after confirmation,
     * then refreshes the list.
     *
     * @param reviewId database ID of the review to un-favourite
     */
    public void removeFavourite(int reviewId) {
        boolean ok = Utilities.showConfirmation("Remove this review from favourites?");
        if (ok) {
            reviewDAO.setFavourite(reviewId, false);
            refreshList();
        }
    }

    /**
     * Refreshes the favourites list from the database.
     */
    public void refreshList() {
        loadFavourites();
    }

    // ======================== UI HELPERS ========================

    /**
     * Shows the full content and metadata of a selected favourite review
     * in an alert dialog.
     *
     * @param review the review to display (must not be null)
     */
    public void showReview(Review review) {
        Book book = libraryDAO.findById(review.getBookId());
        String title = (book != null) ? book.getTitle() : "(Unknown Book)";
        String msg = "Book: " + title
            + "\nDate: "    + Utilities.formatDate(review.getDate())
            + "\nRating: "  + review.getRating() + "/10"
            + "\n\n"        + review.getContent();
        Utilities.showAlert("Favourite Review", msg);
    }
}
