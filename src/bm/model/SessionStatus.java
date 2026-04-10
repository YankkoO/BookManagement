package bm.model;

/**
 * Represents the status of a reading session stored in the reading history.
 *
 * @author Gómez Nido Gonzalo
 */
public enum SessionStatus {

    /** The reading session is currently active. */
    IN_PROGRESS,

    /** The reading session has been paused. */
    PAUSED,

    /** The reading session has been completed. */
    COMPLETED
}
