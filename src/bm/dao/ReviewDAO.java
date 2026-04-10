package bm.dao;

import bm.db.DatabaseConnection;
import bm.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object responsible for all CRUD operations
 * on the "reviews" table in the SQLite database.
 *
 * @author Gómez Nido Gonzalo
 */
public class ReviewDAO {

    /** Shared SQLite connection obtained from the singleton. */
    private Connection connection;

    // ======================== CONSTRUCTOR ========================

    /**
     * Creates a new {@code ReviewDAO} and retrieves the shared database connection.
     */
    public ReviewDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ======================== SCHEMA ========================

    /**
     * Creates the "reviews" table if it does not already exist.
     */
    public void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS reviews (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id   INTEGER NOT NULL,
                    content   TEXT,
                    rating    INTEGER DEFAULT 0,
                    date      TEXT,
                    favourite INTEGER DEFAULT 0,
                    FOREIGN KEY (book_id) REFERENCES books(id)
                )""";
        try {
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reviews table: " + e.getMessage(), e);
        }
    }

    // ======================== CREATE ========================

    /**
     * Inserts a new review into the database.
     * The auto-generated primary key is written back to the {@code review} object.
     *
     * @param review the review to persist (must not be null)
     */
    public void insert(Review review) {
        String sql = "INSERT INTO reviews (book_id, content, rating, date, favourite) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getBookId());
            ps.setString(2, review.getContent());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getDate() != null ? review.getDate().toString() : LocalDate.now().toString());
            ps.setInt(5, review.isFavourite() ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    review.setGeneratedId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert review: " + e.getMessage(), e);
        }
    }

    // ======================== READ ========================

    /**
     * Retrieves all reviews written for a specific book, ordered by date descending.
     *
     * @param bookId database ID of the book
     * @return list of reviews for that book (never null)
     */
    public List<Review> findByBook(int bookId) {
        String sql = "SELECT * FROM reviews WHERE book_id = ? ORDER BY date DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return executeList(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reviews for book id=" + bookId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all reviews that have been marked as favourites, ordered by date descending.
     *
     * @return list of favourite reviews across all books
     */
    public List<Review> findFavourites() {
        String sql = "SELECT * FROM reviews WHERE favourite = 1 ORDER BY date DESC";
        try (ResultSet rs = connection.createStatement().executeQuery(sql)) {
            List<Review> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch favourite reviews: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a single review by its primary key.
     *
     * @param id the database ID of the review
     * @return the matching {@link Review}, or {@code null} if not found
     */
    public Review findById(int id) {
        String sql = "SELECT * FROM reviews WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find review id=" + id + ": " + e.getMessage(), e);
        }
    }

    // ======================== UPDATE ========================

    /**
     * Updates the content, rating, and favourite flag of an existing review.
     *
     * @param review the review with updated values (must have a valid ID)
     */
    public void update(Review review) {
        String sql = "UPDATE reviews SET content = ?, rating = ?, favourite = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, review.getContent());
            ps.setInt(2, review.getRating());
            ps.setInt(3, review.isFavourite() ? 1 : 0);
            ps.setInt(4, review.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update review id=" + review.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Marks or unmarks a specific review as a favourite.
     *
     * @param id        database ID of the review
     * @param favourite true to mark as favourite, false to unmark
     */
    public void setFavourite(int id, boolean favourite) {
        String sql = "UPDATE reviews SET favourite = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, favourite ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update favourite flag for review id=" + id + ": " + e.getMessage(), e);
        }
    }

    // ======================== DELETE ========================

    /**
     * Permanently deletes a review from the database.
     *
     * @param id database ID of the review to delete
     */
    public void delete(int id) {
        String sql = "DELETE FROM reviews WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete review id=" + id + ": " + e.getMessage(), e);
        }
    }

    // ======================== AGGREGATE ========================

    /**
     * Calculates the average user rating across all reviews for a specific book.
     *
     * @param bookId database ID of the book
     * @return average rating (0.0 if no reviews exist for that book)
     */
    public double getAverageRating(int bookId) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE book_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate average rating for book id=" + bookId + ": " + e.getMessage(), e);
        }
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Maps a single positioned ResultSet row to a {@link Review} instance.
     *
     * @param rs a ResultSet positioned on the row to map
     * @return the mapped {@link Review}
     * @throws SQLException if any column access fails
     */
    private Review mapRow(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("date");
        return new Review(
                rs.getInt("id"),
                rs.getInt("book_id"),
                rs.getString("content"),
                rs.getInt("rating"),
                dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now(),
                rs.getInt("favourite") == 1
        );
    }

    /**
     * Executes a prepared SELECT statement and maps every resulting row to a {@link Review}.
     *
     * @param ps a PreparedStatement ready for execution
     * @return list of mapped reviews
     * @throws SQLException if query execution fails
     */
    private List<Review> executeList(PreparedStatement ps) throws SQLException {
        List<Review> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }
}
