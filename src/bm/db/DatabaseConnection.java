package bm.db;

import bm.dao.HistoryDAO;
import bm.dao.LibraryDAO;
import bm.dao.ReviewDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that manages a single shared SQLite database connection
 * for the entire application lifecycle.
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * Connection con = DatabaseConnection.getInstance().getConnection();
 * </pre>
 *
 * @author Gómez Nido Gonzalo
 */
public class DatabaseConnection {

    /** The single shared instance (lazy initialisation). */
    private static DatabaseConnection instance;

    /** The active SQLite JDBC connection. */
    private Connection connection;

    /** SQLite database URL. The file is created in the working directory. */
    private static final String URL = "jdbc:sqlite:book_manager.db";

    // ======================== CONSTRUCTOR (private) ========================

    /**
     * Private constructor — loads the SQLite JDBC driver and opens the connection.
     *
     * @throws RuntimeException if the JDBC driver is missing or the connection fails
     */
    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(URL);
            connection.setAutoCommit(true);

            // Wait up to 5 seconds if the database is locked by another process
            try (java.sql.Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout=5000;");
                
                // Enable WAL mode for better concurrent read performance
                stmt.execute("PRAGMA journal_mode=WAL;");
                
                // Enforce foreign key constraints
                stmt.execute("PRAGMA foreign_keys=ON;");
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found. "
                    + "Add sqlite-jdbc-*.jar to the classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open database connection: " + e.getMessage(), e);
        }
    }

    // ======================== SINGLETON ACCESS ========================

    /**
     * Returns the single shared instance of {@code DatabaseConnection},
     * creating it on the first call (lazy initialisation, not thread-safe).
     *
     * @return the singleton instance
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // ======================== PUBLIC API ========================

    /**
     * Provides the active {@link Connection} to callers (DAOs, etc.).
     *
     * @return the open database connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Gracefully closes the database connection.
     * Should be called when the application is shutting down.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        instance = null;
    }

    /**
     * Ensures that all required tables exist, delegating to each DAO's
     * {@code createTable()} method. Called once during application startup.
     */
    public void verifyTables() {
        new LibraryDAO().createTable();
        new ReviewDAO().createTable();
        new HistoryDAO().createTable();
    }

    /**
     * Checks whether the database connection is currently open and valid.
     *
     * @return true if the connection is open and not closed
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
