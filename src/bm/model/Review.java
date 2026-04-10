package bm.model;

import java.time.LocalDate;
import bm.util.Utilities;

/**
 * Represents a user-written review for a specific book.
 * Maps directly to the "reviews" table in SQLite.
 *
 * @author Gómez Nido Gonzalo
 */
public class Review {

    private int id;
    private int bookId;
    private String content;
    private int rating;
    private LocalDate date;
    private boolean favourite;

    // ======================== CONSTRUCTORS ========================

    /**
     * Full constructor used when loading a review from the database.
     */
    public Review(int id, int bookId, String content, int rating,
                  LocalDate date, boolean favourite) {
        Utilities.requireNotBlank(content, "content");
        Utilities.requireTrue(rating >= 0 && rating <= 10,
                "rating must be between 0 and 10");
        this.id = id;
        this.bookId = bookId;
        this.content = content;
        this.rating = rating;
        this.date = date;
        this.favourite = favourite;
    }

    /**
     * Constructor for creating a new review before persistence.
     */
    public Review(int bookId, String content, int rating) {
        this(0, bookId, content, rating, LocalDate.now(), false);
    }

    // ======================== GETTERS ========================

    /** @return the database primary key */
    public int getId() { return id; }

    /** @return the ID of the book this review belongs to */
    public int getBookId() { return bookId; }

    /** @return the review text */
    public String getContent() { return content; }

    /** @return the numeric rating (0 – 10) */
    public int getRating() { return rating; }

    /** @return the date this review was written */
    public LocalDate getDate() { return date; }

    /** @return true if this review is marked as a favourite */
    public boolean isFavourite() { return favourite; }

    // ======================== SETTERS ========================

    /**
     * Called by the DAO after an INSERT to store the auto-generated primary key.
     *
     * @param id the generated database ID
     */
    public void setGeneratedId(int id) {
        this.id = id;
    }

    /**
     * Updates the text content of this review.
     *
     * @param content new review text (must not be null or blank)
     */
    public void setContent(String content) {
        Utilities.requireNotBlank(content, "content");
        this.content = content;
    }

    /**
     * Updates the numeric rating of this review.
     *
     * @param rating new rating value (must be between 0 and 10)
     */
    public void setRating(int rating) {
        Utilities.requireTrue(rating >= 0 && rating <= 10,
                "rating must be between 0 and 10");
        this.rating = rating;
    }

    /**
     * Marks or unmarks this review as a favourite.
     *
     * @param favourite true to mark as favourite, false to unmark
     */
    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    // ======================== OVERRIDDEN ========================

    @Override
    public String toString() {
        return "Review[id=" + id + ", bookId=" + bookId
                + ", rating=" + rating + ", favourite=" + favourite
                + ", date=" + date + "]";
    }
}
