package bm.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import bm.util.Utilities;

/**
 * Represents a book stored in the personal library database.
 * Maps directly to the "books" table in SQLite.
 *
 * @author Gómez Nido Gonzalo
 */
public class Book {

    private int id;
    private String title;
    private String author;
    private int year;
    private int pages;
    private String genre;
    private String publisher;
    private double rating;
    private ReadingStatus status;
    private LocalDate dateAdded;
    private LocalDate dateCompleted;

    // ======================== CONSTRUCTORS ========================

    /**
     * Full constructor used when loading a book from the database (ID is known).
     */
    public Book(int id, String title, String author, int year, int pages,
                String genre, String publisher, double rating,
                ReadingStatus status, LocalDate dateAdded, LocalDate dateCompleted) {
        Utilities.requireNotBlank(title, "title");
        Utilities.requireNotBlank(author, "author");
        Utilities.requireNotNull(status, "status");
        Utilities.requireTrue(year >= 0, "year must be non-negative");
        Utilities.requireTrue(pages >= 0, "pages must be non-negative");
        Utilities.requireTrue(rating >= 0.0 && rating <= 5.0, "rating must be between 0.0 and 5.0");
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
        this.pages = pages;
        this.genre = genre;
        this.publisher = publisher;
        this.rating = rating;
        this.status = status;
        this.dateAdded = dateAdded;
        this.dateCompleted = dateCompleted;
    }

    /**
     * Constructor used when creating a new book before it is persisted (no ID yet).
     */
    public Book(String title, String author, int year, int pages,
                String genre, String publisher) {
        this(0, title, author, year, pages, genre, publisher,
             0.0, ReadingStatus.NOT_READ, LocalDate.now(), null);
    }

    // ======================== GETTERS ========================

    /** @return the database primary key */
    public int getId() { return id; }

    /** @return the book title */
    public String getTitle() { return title; }

    /** @return the book author */
    public String getAuthor() { return author; }

    /** @return the publication year */
    public int getYear() { return year; }

    /** @return the total number of pages */
    public int getPages() { return pages; }

    /** @return the literary genre */
    public String getGenre() { return genre; }

    /** @return the publisher name */
    public String getPublisher() { return publisher; }

    /** @return the user rating (0.0 – 5.0) */
    public double getRating() { return rating; }

    /** @return the current reading status */
    public ReadingStatus getStatus() { return status; }

    /** @return the date this book was added to the library */
    public LocalDate getDateAdded() { return dateAdded; }

    /** @return the date this book was completed, or null if not yet finished */
    public LocalDate getDateCompleted() { return dateCompleted; }

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
     * Updates the book title.
     *
     * @param title new title (must not be null or blank)
     */
    public void setTitle(String title) {
        Utilities.requireNotBlank(title, "title");
        this.title = title;
    }

    /**
     * Updates the book author.
     *
     * @param author new author (must not be null or blank)
     */
    public void setAuthor(String author) {
        Utilities.requireNotBlank(author, "author");
        this.author = author;
    }

    /**
     * Updates the publication year.
     *
     * @param year positive publication year
     */
    public void setYear(int year) {
        Utilities.requireTrue(year >= 0, "year must be non-negative");
        this.year = year;
    }

    /**
     * Updates the total page count.
     *
     * @param pages positive page count
     */
    public void setPages(int pages) {
        Utilities.requireTrue(pages >= 0, "pages must be non-negative");
        this.pages = pages;
    }

    /**
     * Updates the literary genre.
     *
     * @param genre genre string (may be null)
     */
    public void setGenre(String genre) {
        this.genre = genre;
    }

    /**
     * Updates the publisher name.
     *
     * @param publisher publisher string (may be null)
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * Updates the user rating for this book.
     *
     * @param rating new rating value (must be between 0.0 and 5.0)
     * @throws IllegalArgumentException if rating is out of range
     */
    public void setRating(double rating) {
        Utilities.requireTrue(rating >= 0.0 && rating <= 5.0,
                "rating must be between 0.0 and 5.0");
        this.rating = rating;
    }

    /**
     * Updates the reading status of this book.
     * If the new status is {@link ReadingStatus#COMPLETED} and {@code dateCompleted}
     * is not yet set, it is automatically set to today.
     *
     * @param status new reading status (must not be null)
     */
    public void setStatus(ReadingStatus status) {
        Utilities.requireNotNull(status, "status");
        if (status == ReadingStatus.COMPLETED && this.dateCompleted == null) {
            this.dateCompleted = LocalDate.now();
        }
        this.status = status;
    }

    /**
     * Sets the date when this book was added to the library.
     *
     * @param dateAdded date added (may be null)
     */
    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    /**
     * Sets the date this book was completed.
     *
     * @param dateCompleted completion date (may be null)
     */
    public void setDateCompleted(LocalDate dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    // ======================== SPECIFIC METHODS ========================

    /**
     * Checks whether this book has been fully read.
     *
     * @return true if reading status is {@link ReadingStatus#COMPLETED}
     */
    public boolean isCompleted() {
        return this.status == ReadingStatus.COMPLETED;
    }

    /**
     * Calculates how many days this book has been in the library.
     *
     * @return number of days since {@code dateAdded}, or 0 if {@code dateAdded} is null
     */
    public int getDaysInLibrary() {
        if (dateAdded == null) return 0;
        return (int) ChronoUnit.DAYS.between(dateAdded, LocalDate.now());
    }

    // ======================== OVERRIDDEN ========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Book)) return false;
        Book other = (Book) obj;
        return title.equalsIgnoreCase(other.title) && author.equalsIgnoreCase(other.author);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(title.toLowerCase(), author.toLowerCase());
    }

    @Override
    public String toString() {
        return "Book[id=" + id + ", title='" + title + "', author='" + author
                + "', year=" + year + ", status=" + status + ", rating=" + rating + "]";
    }
}
