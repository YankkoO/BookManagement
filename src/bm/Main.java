package bm;

import bm.db.DatabaseConnection;
import bm.gui.MainView;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application entry point for the Book Manager desktop application.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Bootstrap the SQLite database (create tables if they are missing).</li>
 *   <li>Launch the JavaFX runtime and display {@link MainView}.</li>
 *   <li>Load the application stylesheet.</li>
 * </ol>
 *
 * @author Gómez Nido Gonzalo
 */
public class Main extends Application {

    // ======================== JAVAFX STAGE ========================

    private Stage stage;
    private Scene scene;

    // ======================== ENTRY POINT ========================

    /**
     * Application entry point.
     * Initialises the database and launches the JavaFX runtime.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        initDatabase();
        launch(args);
    }

    // ======================== JAVAFX LIFECYCLE ========================

    /**
     * Called by the JavaFX runtime after {@code launch()} has been invoked.
     * Sets up the primary stage and shows the main window.
     *
     * @param primaryStage the top-level JavaFX container provided by the runtime
     * @throws Exception if the FXML fails to load
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        showMainWindow();
    }

    /**
     * Called by the JavaFX runtime when the application is about to stop.
     * Closures the database connection gracefully.
     */
    @Override
    public void stop() {
        DatabaseConnection.getInstance().closeConnection();
    }

    // ======================== NAVIGATION ========================

    /**
     * Loads and shows {@link MainView} as the primary application window.
     *
     * @throws Exception if the FXML resource cannot be located or parsed
     */
    public void showMainWindow() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        scene = new Scene(root, 1280, 800);
        loadStylesheet();
        stage.setTitle("Book Manager");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    // ======================== SETUP HELPERS ========================

    /**
     * Applies the application CSS stylesheet to the current scene.
     * Must be called after the scene has been created.
     */
    public void loadStylesheet() {
        String css = getClass().getResource("/css/styles.css").toExternalForm();
        scene.getStylesheets().add(css);
    }

    /**
     * Bootstraps the database: obtains the singleton connection and
     * creates any missing tables by calling {@link DatabaseConnection#verifyTables()}.
     */
    public static void initDatabase() {
        DatabaseConnection.getInstance().verifyTables();
    }
}
