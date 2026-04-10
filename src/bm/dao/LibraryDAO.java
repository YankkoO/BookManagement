package bm.dao;

import bm.db.DatabaseConnection;
import bm.model.Book;
import bm.model.ReadingStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object responsible for all CRUD and query operations
 * on the "books" table in the SQLite database.
 *
 * <p>Every method obtains a {@link Connection} from {@link DatabaseConnection#getInstance()}.
 *
 * @author Gómez Nido Gonzalo
 */
public class LibraryDAO {

    /** Shared SQLite connection obtained from the singleton. */
    private Connection connection;

    // ======================== CONSTRUCTOR ========================

    /**
     * Creates a new {@code LibraryDAO} and retrieves the shared database connection.
     */
    public LibraryDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ======================== SCHEMA ========================

    /**
     * Creates the "books" table if it does not already exist.
     * Should be called once during application startup via
     * {@link DatabaseConnection#verifyTables()}.
     */
    public void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS books (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    title          TEXT    NOT NULL,
                    author         TEXT    NOT NULL,
                    year           INTEGER,
                    pages          INTEGER,
                    genre          TEXT,
                    publisher      TEXT,
                    rating         REAL    DEFAULT 0.0,
                    status         TEXT    DEFAULT 'NOT_READ',
                    date_added     TEXT,
                    date_completed TEXT
                )""";
        try {
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create books table: " + e.getMessage(), e);
        }
    }

    // ======================== CREATE ========================

    /**
     * Inserts a new book into the database.
     * The auto-generated primary key is written back to the {@code book} object
     * via reflection-free field access using a package-private setter convention.
     *
     * @param book the book to persist (must not be null)
     */
    public void insert(Book book) {
        String sql = """
                INSERT INTO books
                    (title, author, year, pages, genre, publisher, rating, status, date_added, date_completed)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, book.getYear());
            ps.setInt(4, book.getPages());
            ps.setString(5, book.getGenre());
            ps.setString(6, book.getPublisher());
            ps.setDouble(7, book.getRating());
            ps.setString(8, book.getStatus().name());
            ps.setString(9, book.getDateAdded() != null ? book.getDateAdded().toString() : null);
            ps.setString(10, book.getDateCompleted() != null ? book.getDateCompleted().toString() : null);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    // Reflect the generated ID back into the Book via the full constructor
                    // (Book does not expose a setId — reconstruct is done in the caller if needed)
                    // Store the generated ID for callers that need it
                    book.setGeneratedId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert book: " + e.getMessage(), e);
        }
    }

    // ======================== READ ========================

    /**
     * Retrieves every book stored in the library, ordered alphabetically by title.
     *
     * @return list of all books (never null; empty if the table has no rows)
     */
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title COLLATE NOCASE ASC";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all books: " + e.getMessage(), e);
        }
        return books;
    }

    /**
     * Retrieves a single book by its primary key.
     *
     * @param id the database ID of the book
     * @return the matching {@link Book}, or {@code null} if not found
     */
    public Book findById(int id) {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find book by id=" + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Searches for books whose title contains the given string (case-insensitive).
     *
     * @param title partial or full title to search for
     * @return list of matching books
     */
    public List<Book> findByTitle(String title) {
        String sql = "SELECT * FROM books WHERE title LIKE '%' || ? || '%' COLLATE NOCASE ORDER BY title";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search books by title: " + e.getMessage(), e);
        }
    }

    /**
     * Searches for books by author name (case-insensitive partial match).
     *
     * @param author author name to search for
     * @return list of matching books
     */
    public List<Book> findByAuthor(String author) {
        String sql = "SELECT * FROM books WHERE author LIKE '%' || ? || '%' COLLATE NOCASE ORDER BY author";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, author);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search books by author: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all books belonging to the specified literary genre (exact match, case-insensitive).
     *
     * @param genre genre to filter by
     * @return list of matching books
     */
    public List<Book> findByGenre(String genre) {
        String sql = "SELECT * FROM books WHERE genre = ? COLLATE NOCASE ORDER BY title";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, genre);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to filter books by genre: " + e.getMessage(), e);
        }
    }

    /**
     * Filters books by their reading status.
     *
     * @param status the {@link ReadingStatus} name string to filter by
     * @return list of books with the given status
     */
    public List<Book> findByStatus(String status) {
        String sql = "SELECT * FROM books WHERE status = ? ORDER BY title COLLATE NOCASE";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to filter books by status: " + e.getMessage(), e);
        }
    }

    /**
     * Filters books published within a year range (inclusive).
     *
     * @param yearMin minimum publication year
     * @param yearMax maximum publication year
     * @return list of books published between {@code yearMin} and {@code yearMax}
     */
    public List<Book> findByYearRange(int yearMin, int yearMax) {
        String sql = "SELECT * FROM books WHERE year BETWEEN ? AND ? ORDER BY year ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, yearMin);
            ps.setInt(2, yearMax);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to filter books by year range: " + e.getMessage(), e);
        }
    }

    /**
     * Filters books within a page-count range (inclusive).
     *
     * @param minPages minimum number of pages
     * @param maxPages maximum number of pages
     * @return list of books within the page range
     */
    public List<Book> findByPageRange(int minPages, int maxPages) {
        String sql = "SELECT * FROM books WHERE pages BETWEEN ? AND ? ORDER BY pages ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, minPages);
            ps.setInt(2, maxPages);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to filter books by page range: " + e.getMessage(), e);
        }
    }

    /**
     * Performs a global free-text search across title and author (case-insensitive).
     *
     * @param query search term
     * @return matching books ordered by title
     */
    public List<Book> search(String query) {
        String sql = """
                SELECT * FROM books
                WHERE title LIKE '%' || ? || '%' COLLATE NOCASE
                   OR author LIKE '%' || ? || '%' COLLATE NOCASE
                ORDER BY title COLLATE NOCASE""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, query);
            ps.setString(2, query);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search books: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all distinct genres stored in the library, sorted alphabetically.
     *
     * @return list of genre strings (never null)
     */
    public List<String> findAllGenres() {
        List<String> genres = new ArrayList<>();
        String sql = "SELECT DISTINCT genre FROM books WHERE genre IS NOT NULL ORDER BY genre COLLATE NOCASE";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                String g = rs.getString(1);
                if (g != null && !g.isBlank()) genres.add(g);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch genres: " + e.getMessage(), e);
        }
        return genres;
    }

    // ======================== UPDATE ========================

    /**
     * Updates all mutable fields of an existing book in the database.
     *
     * @param book the book with updated values (must have a valid ID)
     */
    public void update(Book book) {
        String sql = """
                UPDATE books
                SET    title          = ?,
                       author         = ?,
                       year           = ?,
                       pages          = ?,
                       genre          = ?,
                       publisher      = ?,
                       rating         = ?,
                       status         = ?,
                       date_completed = ?
                WHERE  id = ?""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, book.getYear());
            ps.setInt(4, book.getPages());
            ps.setString(5, book.getGenre());
            ps.setString(6, book.getPublisher());
            ps.setDouble(7, book.getRating());
            ps.setString(8, book.getStatus().name());
            ps.setString(9, book.getDateCompleted() != null ? book.getDateCompleted().toString() : null);
            ps.setInt(10, book.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update book id=" + book.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates only the rating of a specific book.
     *
     * @param id     database ID of the book
     * @param rating new rating value (0.0 – 5.0)
     */
    public void updateRating(int id, double rating) {
        String sql = "UPDATE books SET rating = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, rating);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update rating for book id=" + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates only the reading status of a specific book.
     *
     * @param id     database ID of the book
     * @param status new status string (must match a {@link ReadingStatus} name)
     */
    public void updateStatus(int id, String status) {
        String sql = "UPDATE books SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status for book id=" + id + ": " + e.getMessage(), e);
        }
    }

    // ======================== DELETE ========================

    /**
     * Permanently removes a book and all its associated reviews and reading history
     * from the database (cascaded manually since SQLite PRAGMA foreign_keys is ON).
     *
     * @param id database ID of the book to delete
     */
    public void delete(int id) {
        try {
            // Delete dependent rows first to respect foreign key constraints
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM reviews WHERE book_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM reading_history WHERE book_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM books WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete book id=" + id + ": " + e.getMessage(), e);
        }
    }

    // ======================== AGGREGATE ========================

    /**
     * Returns the total number of books in the library.
     *
     * @return book count, or 0 if none exist
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM books";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count books: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the number of books with the given reading status.
     *
     * @param status the {@link ReadingStatus} to count
     * @return count of books matching the status
     */
    public int countByStatus(ReadingStatus status) {
        String sql = "SELECT COUNT(*) FROM books WHERE status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count books by status: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the average rating across all books that have a rating > 0.
     *
     * @return average rating, or 0.0 if no rated books exist
     */
    public double getAverageRating() {
        String sql = "SELECT AVG(rating) FROM books WHERE rating > 0";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate average rating: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the total number of pages across all books in the library.
     *
     * @return sum of pages, or 0 if library is empty
     */
    public int getTotalPages() {
        String sql = "SELECT SUM(pages) FROM books";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate total pages: " + e.getMessage(), e);
        }
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Maps a single positioned ResultSet row to a {@link Book} instance.
     *
     * @param rs a ResultSet positioned on the row to map
     * @return the mapped {@link Book}
     * @throws SQLException if any column access fails
     */
    private Book mapRow(ResultSet rs) throws SQLException {
        String dateAddedStr = rs.getString("date_added");
        String dateCompletedStr = rs.getString("date_completed");
        String statusStr = rs.getString("status");

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
                dateAddedStr != null ? LocalDate.parse(dateAddedStr) : null,
                dateCompletedStr != null ? LocalDate.parse(dateCompletedStr) : null
        );
    }

    /**
     * Executes a prepared SELECT statement and maps every resulting row to a {@link Book}.
     *
     * @param ps a PreparedStatement ready for execution
     * @return list of mapped books
     * @throws SQLException if query execution fails
     */
    private List<Book> executeList(PreparedStatement ps) throws SQLException {
        List<Book> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }
}
