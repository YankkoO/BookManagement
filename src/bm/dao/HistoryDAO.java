package bm.dao;

import bm.db.DatabaseConnection;
import bm.model.Book;
import bm.model.ReadingHistory;
import bm.model.ReadingStatus;
import bm.model.SessionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object responsible for all operations on the
 * "reading_history" table in the SQLite database.
 *
 * <p>Manages the full lifecycle of reading sessions: starting, pausing,
 * resuming, completing, and tracking pages read.
 *
 * @author Gómez Nido Gonzalo
 */
public class HistoryDAO {

    /** Shared SQLite connection obtained from the singleton. */
    private Connection connection;

    // ======================== CONSTRUCTOR ========================

    /**
     * Creates a new {@code HistoryDAO} and retrieves the shared database connection.
     */
    public HistoryDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ======================== SCHEMA ========================

    /**
     * Creates the "reading_history" table if it does not already exist.
     */
    public void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS reading_history (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id      INTEGER NOT NULL,
                    start_date   TEXT,
                    end_date     TEXT,
                    pages_read   INTEGER DEFAULT 0,
                    status       TEXT    DEFAULT 'IN_PROGRESS',
                    FOREIGN KEY (book_id) REFERENCES books(id)
                )""";
        try {
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reading_history table: " + e.getMessage(), e);
        }
    }

    // ======================== SESSION LIFECYCLE ========================

    /**
     * Creates a new reading session for the specified book with status {@code IN_PROGRESS}.
     * Also updates the book's reading status to {@code READING} in the "books" table.
     *
     * @param bookId database ID of the book to start reading
     */
    public void startReading(int bookId) {
        String sql = "INSERT INTO reading_history (book_id, start_date, status) VALUES (?, ?, 'IN_PROGRESS')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setString(2, LocalDate.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start reading session for book id=" + bookId + ": " + e.getMessage(), e);
        }
        new LibraryDAO().updateStatus(bookId, ReadingStatus.READING.name());
    }

    /**
     * Marks the active reading session for the specified book as {@code COMPLETED}
     * and sets the end date to today.
     * Also updates the book's status to {@code COMPLETED}.
     *
     * @param bookId database ID of the book
     */
    public void completeReading(int bookId) {
        String sql = """
                UPDATE reading_history
                SET    status   = 'COMPLETED',
                       end_date = ?
                WHERE  book_id = ? AND status = 'IN_PROGRESS'""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to complete reading for book id=" + bookId + ": " + e.getMessage(), e);
        }
        new LibraryDAO().updateStatus(bookId, ReadingStatus.COMPLETED.name());
    }

    /**
     * Pauses the active reading session for the specified book.
     *
     * @param bookId database ID of the book
     */
    public void pauseReading(int bookId) {
        String sql = "UPDATE reading_history SET status = 'PAUSED' WHERE book_id = ? AND status = 'IN_PROGRESS'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to pause reading for book id=" + bookId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Resumes a paused reading session for the specified book.
     *
     * @param bookId database ID of the book
     */
    public void resumeReading(int bookId) {
        String sql = "UPDATE reading_history SET status = 'IN_PROGRESS' WHERE book_id = ? AND status = 'PAUSED'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resume reading for book id=" + bookId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates the pages-read counter in the active session for the specified book.
     *
     * @param bookId    database ID of the book
     * @param pagesRead new page count (must be >= 0)
     */
    public void updatePagesRead(int bookId, int pagesRead) {
        String sql = "UPDATE reading_history SET pages_read = ? WHERE book_id = ? AND status = 'IN_PROGRESS'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, pagesRead);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update pages read for book id=" + bookId + ": " + e.getMessage(), e);
        }
    }

    // ======================== READ ========================

    /**
     * Retrieves the most recent history entry for a specific book
     * (the last row ordered by ID descending).
     *
     * @param bookId database ID of the book
     * @return the latest {@link ReadingHistory} entry, or {@code null} if none found
     */
    public ReadingHistory findLatestByBook(int bookId) {
        String sql = "SELECT * FROM reading_history WHERE book_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find latest history for book id=" + bookId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns all books that currently have an active reading session ({@code IN_PROGRESS}).
     *
     * @return list of books currently being read
     */
    public List<Book> findBooksInProgress() {
        String sql = """
                SELECT b.*
                FROM   books b
                JOIN   reading_history h ON b.id = h.book_id
                WHERE  h.status = 'IN_PROGRESS'
                ORDER  BY b.title COLLATE NOCASE""";
        List<Book> books = new ArrayList<>();
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapBookRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch books in progress: " + e.getMessage(), e);
        }
        return books;
    }

    /**
     * Retrieves the full history of all reading sessions for a specific book,
     * ordered from most recent to oldest.
     *
     * @param bookId database ID of the book
     * @return list of all reading session entries for that book
     */
    public List<ReadingHistory> findAllByBook(int bookId) {
        String sql = "SELECT * FROM reading_history WHERE book_id = ? ORDER BY id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch history for book id=" + bookId + ": " + e.getMessage(), e);
        }
    }

    // ======================== AGGREGATE ========================

    /**
     * Calculates the average number of days it takes to complete a book
     * across all {@code COMPLETED} sessions that have a valid end date.
     *
     * @return average days per completed book, or 0.0 if no completed sessions exist
     */
    public double getAverageCompletionDays() {
        String sql = """
                SELECT AVG(julianday(end_date) - julianday(start_date))
                FROM   reading_history
                WHERE  status = 'COMPLETED'
                AND    end_date IS NOT NULL
                AND    start_date IS NOT NULL""";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate average completion days: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the total number of pages read across all completed sessions.
     *
     * @return total pages read, or 0 if no sessions exist
     */
    public int getTotalPagesRead() {
        String sql = "SELECT SUM(pages_read) FROM reading_history WHERE status = 'COMPLETED'";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate total pages read: " + e.getMessage(), e);
        }
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Maps a single positioned ResultSet row to a {@link ReadingHistory} instance.
     *
     * @param rs a ResultSet positioned on the row to map
     * @return the mapped {@link ReadingHistory}
     * @throws SQLException if any column access fails
     */
    private ReadingHistory mapRow(ResultSet rs) throws SQLException {
        String startStr = rs.getString("start_date");
        String endStr   = rs.getString("end_date");
        String statusStr = rs.getString("status");
        return new ReadingHistory(
                rs.getInt("id"),
                rs.getInt("book_id"),
                startStr != null ? LocalDate.parse(startStr) : null,
                endStr   != null ? LocalDate.parse(endStr)   : null,
                rs.getInt("pages_read"),
                statusStr != null ? SessionStatus.valueOf(statusStr) : SessionStatus.IN_PROGRESS
        );
    }

    /**
     * Maps a ResultSet row from a JOIN query that includes book columns
     * to a {@link Book} instance.
     *
     * @param rs a ResultSet with book columns available
     * @return the mapped {@link Book}
     * @throws SQLException if any column access fails
     */
    private Book mapBookRow(ResultSet rs) throws SQLException {
        String dateAddedStr    = rs.getString("date_added");
        String dateCompletedStr = rs.getString("date_completed");
        String statusStr       = rs.getString("status");
        return new Book(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getInt("year"),
                rs.getInt("pages"),
                rs.getString("genre"),
                rs.getString("publisher"),
                rs.getDouble("rating"),
                statusStr != null ? ReadingStatus.valueOf(statusStr) : ReadingStatus.NOT_READ,
                dateAddedStr    != null ? LocalDate.parse(dateAddedStr)    : null,
                dateCompletedStr != null ? LocalDate.parse(dateCompletedStr) : null
        );
    }

    /**
     * Executes a prepared SELECT statement and maps every row to a {@link ReadingHistory}.
     *
     * @param ps a PreparedStatement ready for execution
     * @return list of mapped history entries
     * @throws SQLException if query execution fails
     */
    private List<ReadingHistory> executeList(PreparedStatement ps) throws SQLException {
        List<ReadingHistory> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }
}
