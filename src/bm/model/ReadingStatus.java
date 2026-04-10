package bm.model;

/**
 * Represents the reading status of a book in the personal library.
 *
 * @author Gómez Nido Gonzalo
 */
public enum ReadingStatus {

    /** The book has not been started yet. */
    NOT_READ,

    /** The book is currently being read. */
    READING,

    /** The book has been fully read and completed. */
    COMPLETED,

    /** The book was started but abandoned before finishing. */
    ABANDONED
}
