package bm.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import bm.util.Utilities;

/**
 * Represents a reading session entry in the reading history for a specific book.
 * Maps directly to the "reading_history" table in SQLite.
 *
 * @author Gómez Nido Gonzalo
 */
public class ReadingHistory {

    private int id;
    private int bookId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int pagesRead;
    private SessionStatus status;

    // ======================== CONSTRUCTORS ========================

    /**
     * Full constructor used when loading a history entry from the database.
     *
     * @param id        database primary key
     * @param bookId    foreign key referencing the associated book
     * @param startDate date the reading session started
     * @param endDate   date the reading session ended (may be null if still active)
     * @param pagesRead number of pages read so far in this session
     * @param status    current state of this reading session
     */
    public ReadingHistory(int id, int bookId, LocalDate startDate,
                          LocalDate endDate, int pagesRead, SessionStatus status) {
        Utilities.requireNotNull(status, "status");
        Utilities.requireTrue(pagesRead >= 0, "pagesRead must be non-negative");
        this.id = id;
        this.bookId = bookId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pagesRead = pagesRead;
        this.status = status;
    }

    /**
     * Constructor for starting a new reading session (no end date, no pages read yet).
     *
     * @param bookId foreign key referencing the book being read
     */
    public ReadingHistory(int bookId) {
        this(0, bookId, LocalDate.now(), null, 0, SessionStatus.IN_PROGRESS);
    }

    // ======================== GETTERS ========================

    /** @return the database primary key */
    public int getId() { return id; }

    /** @return the ID of the book this history entry belongs to */
    public int getBookId() { return bookId; }

    /** @return the date this reading session started */
    public LocalDate getStartDate() { return startDate; }

    /** @return the date this reading session ended, or null if still active */
    public LocalDate getEndDate() { return endDate; }

    /** @return the number of pages read in this session */
    public int getPagesRead() { return pagesRead; }

    /** @return the current state of this reading session */
    public SessionStatus getStatus() { return status; }

    // ======================== SETTERS ========================

    /**
     * Sets the end date of this reading session.
     *
     * @param endDate end date (must be equal to or after {@code startDate})
     * @throws IllegalArgumentException if endDate is before startDate
     */
    public void setEndDate(LocalDate endDate) {
        if (startDate != null && endDate != null) {
            Utilities.requireTrue(!endDate.isBefore(startDate),
                    "endDate cannot be before startDate");
        }
        this.endDate = endDate;
    }

    /**
     * Updates the number of pages read in this session.
     *
     * @param pagesRead new page count (must be >= 0)
     * @throws IllegalArgumentException if pagesRead is negative
     */
    public void setPagesRead(int pagesRead) {
        Utilities.requireTrue(pagesRead >= 0, "pagesRead must be non-negative");
        this.pagesRead = pagesRead;
    }

    /**
     * Updates the state of this reading session.
     *
     * @param status new reading session status (must not be null)
     */
    public void setStatus(SessionStatus status) {
        Utilities.requireNotNull(status, "status");
        this.status = status;
    }

    // ======================== SPECIFIC METHODS ========================

    /**
     * Calculates the number of days elapsed in this reading session.
     * If the session has ended, calculates days between start and end.
     * If still active, calculates days from start to today.
     *
     * @return number of days reading, or 0 if startDate is null
     */
    public int getDaysReading() {
        if (startDate == null) return 0;
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        return (int) ChronoUnit.DAYS.between(startDate, end);
    }

    /**
     * Calculates the reading progress as a percentage of the total book pages.
     *
     * @param totalPages total number of pages in the book (must be > 0)
     * @return percentage of pages read (0.0 – 100.0), or 0.0 if invalid input
     */
    public double getProgressPercent(int totalPages) {
        if (totalPages <= 0) return 0.0;
        return ((double) pagesRead / totalPages) * 100.0;
    }

    // ======================== OVERRIDDEN ========================

    @Override
    public String toString() {
        return "ReadingHistory[id=" + id + ", bookId=" + bookId
                + ", status=" + status + ", pagesRead=" + pagesRead
                + ", startDate=" + startDate + ", endDate=" + endDate + "]";
    }
}
