package bm.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.Optional;

/**
 * Static utility class that centralises all shared helper methods and
 * parameter-validation logic for the Book Manager application.
 *
 * <p>This class merges the functionality of the external {@code ParamCheck}
 * library (originally from Kayanko_Util) so the application has no external
 * dependency for validation.
 *
 * <p>No instances should ever be created.
 *
 * @author Gómez Nido Gonzalo
 */
public final class Utilities {

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE CONSTRUCTOR — prevents instantiation
    // ─────────────────────────────────────────────────────────────

    private Utilities() { }

    // =================================================================
    //  SECTION 1 — PARAMETER VALIDATION  (migrated from ParamCheck)
    // =================================================================

    /**
     * Checks that the given boolean expression is {@code true}.
     * Throws an {@link IllegalArgumentException} with the provided message if it is not.
     *
     * @param expression the condition to assert
     * @param message    exception message to use if the assertion fails
     * @throws IllegalArgumentException if expression is false
     */
    public static void requireTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that the given boolean expression is {@code true}.
     * Throws an {@link IllegalArgumentException} with a default message if it is not.
     *
     * @param expression the condition to assert
     * @throws IllegalArgumentException if expression is false
     */
    public static void requireTrue(boolean expression) {
        requireTrue(expression, "Condition is false — invalid argument.");
    }

    /**
     * Checks that the given boolean expression is {@code false}.
     *
     * @param expression the condition that must be false
     * @param message    exception message to use if the assertion fails
     * @throws IllegalArgumentException if expression is true
     */
    public static void requireFalse(boolean expression, String message) {
        if (expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that the given boolean expression is {@code false}.
     *
     * @param expression the condition that must be false
     * @throws IllegalArgumentException if expression is true
     */
    public static void requireFalse(boolean expression) {
        requireFalse(expression, "Condition is true — invalid argument.");
    }

    /**
     * Checks that the given object is not {@code null}.
     *
     * @param object  the object to check
     * @param message exception message if null
     * @throws IllegalArgumentException if object is null
     */
    public static void requireNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that the given object is not {@code null}, using a default message.
     *
     * @param object the object to check
     * @throws IllegalArgumentException if object is null
     */
    public static void requireNotNull(Object object) {
        requireNotNull(object, "Argument must not be null.");
    }

    /**
     * Checks that the given string is neither {@code null} nor empty.
     *
     * @param value the string to check
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void requireNotEmpty(String value) {
        requireTrue(value != null && !value.isEmpty(),
                "String argument must not be null or empty.");
    }

    /**
     * Checks that the given string is neither {@code null} nor empty.
     *
     * @param value   the string to check
     * @param message exception message if the check fails
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void requireNotEmpty(String value, String message) {
        requireTrue(value != null && !value.isEmpty(), message);
    }

    /**
     * Checks that the given string is neither {@code null} nor blank.
     *
     * @param value the string to check
     * @throws IllegalArgumentException if value is null or blank
     */
    public static void requireNotBlank(String value) {
        requireTrue(value != null && !value.isBlank(),
                "String argument must not be null or blank.");
    }

    /**
     * Checks that the given string is neither {@code null} nor blank,
     * using a custom message that includes the field name.
     *
     * @param value     the string to check
     * @param fieldName the name of the field (used in the error message)
     * @throws IllegalArgumentException if value is null or blank
     */
    public static void requireNotBlank(String value, String fieldName) {
        requireTrue(value != null && !value.isBlank(),
                "'" + fieldName + "' must not be null or blank.");
    }

    /**
     * Checks that the given string is neither {@code null} nor empty,
     * and applies both null and empty checks in sequence.
     *
     * @param value   the string to check
     * @param message exception message for both checks
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void requireNotNullOrEmpty(String value, String message) {
        requireNotNull(value, message);
        requireNotEmpty(value, message);
    }

    /**
     * Checks that the given string is neither {@code null} nor empty,
     * using a default message.
     *
     * @param value the string to check
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void requireNotNullOrEmpty(String value) {
        requireNotNull(value);
        requireNotEmpty(value);
    }

    // =================================================================
    //  SECTION 2 — DATE UTILITIES
    // =================================================================

    /** Default date format used across the application. */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Formats a {@link LocalDate} to a human-readable string (dd/MM/yyyy).
     *
     * @param date the date to format (may be null)
     * @return formatted date string, or empty string if date is null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMAT);
    }

    /**
     * Parses a date string in the format dd/MM/yyyy into a {@link LocalDate}.
     *
     * @param text date string to parse
     * @return parsed {@link LocalDate}, or null if parsing fails or text is blank
     */
    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // =================================================================
    //  SECTION 3 — DATA VALIDATION
    // =================================================================

    /**
     * Validates that an email address follows the basic pattern local@domain.tld.
     *
     * @param email the string to validate (may be null)
     * @return true if the string looks like a valid email
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Validates that a rating is within the accepted range (0.0 – 5.0).
     *
     * @param rating the rating value to check
     * @return true if 0.0 &lt;= rating &lt;= 5.0
     */
    public static boolean isValidRating(double rating) {
        return rating >= 0.0 && rating <= 5.0;
    }

    // =================================================================
    //  SECTION 4 — STRING HELPERS
    // =================================================================

    /**
     * Truncates a string to the specified maximum length, appending "…" if truncated.
     *
     * @param text   the string to truncate (may be null)
     * @param maxLen maximum number of characters before truncation
     * @return the (possibly truncated) string, or empty string if text is null
     */
    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return (text.length() <= maxLen) ? text : text.substring(0, maxLen) + "…";
    }

    /**
     * Formats an integer with thousands separators for display.
     * Example: {@code 1234567} → {@code "1,234,567"}
     *
     * @param number the number to format
     * @return formatted string with grouping separators
     */
    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }

    // =================================================================
    //  SECTION 5 — JAVAFX UI HELPERS
    // =================================================================

    /**
     * Shows an informational JavaFX {@code Alert} dialog.
     *
     * @param title   dialog window title
     * @param message body text shown to the user
     */
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Apply dark theme via inline dialog pane style
        alert.getDialogPane().setStyle(
            "-fx-background-color:#161920; -fx-border-color:#2D3553;");
        alert.showAndWait();
    }

    /**
     * Shows an error JavaFX {@code Alert} dialog.
     *
     * @param title   dialog window title
     * @param message body text shown to the user
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle(
            "-fx-background-color:#161920; -fx-border-color:#2D3553;");
        alert.showAndWait();
    }

    /**
     * Shows a confirmation JavaFX {@code Alert} dialog and returns the user's choice.
     *
     * @param message question to present to the user
     * @return true if the user clicked OK / Confirm; false otherwise
     */
    public static boolean showConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle(
            "-fx-background-color:#161920; -fx-border-color:#2D3553;");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Returns a colour representing the visual quality of a rating value.
     * Green for high ratings, orange for average, red for low.
     *
     * @param rating rating value (0.0 – 5.0)
     * @return a JavaFX {@code Color}
     */
    public static Color getRatingColor(double rating) {
        if (rating >= 4.0) return Color.web("#4CAF50");
        if (rating >= 2.5) return Color.web("#FF9800");
        return Color.web("#F44336");
    }

    /**
     * Builds a JavaFX {@code HBox} containing filled/empty star icons for the rating.
     *
     * @param rating rating value (0.0 – 5.0); rounded to nearest integer for display
     * @return {@code HBox} with star Label nodes
     */
    public static HBox buildStarRating(double rating) {
        HBox box = new HBox(4);
        int filled = (int) Math.round(rating);
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= filled ? "★" : "☆");
            star.setStyle("-fx-text-fill: " + (i <= filled ? "#FBBF24" : "#2D3553")
                + "; -fx-font-size: 16px;");
            box.getChildren().add(star);
        }
        return box;
    }
}
