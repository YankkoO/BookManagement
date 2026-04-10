package bm.util;

import bm.dao.LibraryDAO;
import bm.model.Book;
import bm.model.ReadingStatus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to import books from a CSV file into the SQLite database.
 *
 * <p>Expected CSV format (header row required):
 * <pre>
 * title,author,year,pages,genre,publisher,rating,status,date_added,date_completed
 * </pre>
 *
 * <p>Rules:
 * <ul>
 *   <li>The first row must be a header (it is skipped).</li>
 *   <li>Fields may be surrounded by double-quotes.</li>
 *   <li>Only {@code title}, {@code author}, {@code year} and {@code pages} are mandatory.</li>
 *   <li>{@code rating} defaults to 0.0 if missing or invalid.</li>
 *   <li>{@code status} must match a {@link ReadingStatus} name; defaults to {@code NOT_READ}.</li>
 *   <li>Dates are expected in {@code yyyy-MM-dd} or {@code dd/MM/yyyy} format.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * CsvImporter.importFromFile("C:/path/to/books.csv");
 * </pre>
 *
 * @author Gómez Nido Gonzalo
 */
public final class CsvImporter {

    /** Accepted date patterns for parsing date columns. */
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    private CsvImporter() { }

    // ======================== PUBLIC API ========================

    /**
     * Reads a CSV file and inserts each valid row as a {@link Book} into the database.
     *
     * @param filePath absolute or relative path to the CSV file
     * @return an {@link ImportResult} containing counts of imported and skipped rows
     */
    public static ImportResult importFromFile(String filePath) {
        LibraryDAO dao = new LibraryDAO();
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped  = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Skip header row
            String headerLine = br.readLine();
            if (headerLine == null) {
                return new ImportResult(0, 0, List.of("File is empty."));
            }

            String[] headers = splitCsvLine(headerLine);
            int titleIdx       = findColumn(headers, "title");
            int authorIdx      = findColumn(headers, "author");
            int yearIdx        = findColumn(headers, "year");
            int pagesIdx       = findColumn(headers, "pages");
            int genreIdx       = findColumn(headers, "genre");
            int publisherIdx   = findColumn(headers, "publisher");
            int ratingIdx      = findColumn(headers, "rating");
            int statusIdx      = findColumn(headers, "status");
            int dateAddedIdx   = findColumn(headers, "date_added");
            int dateCompletedIdx = findColumn(headers, "date_completed");

            if (titleIdx < 0 || authorIdx < 0) {
                return new ImportResult(0, 0,
                        List.of("CSV header must contain at least 'title' and 'author' columns."));
            }

            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    String[] cols = splitCsvLine(line);

                    String title  = get(cols, titleIdx);
                    String author = get(cols, authorIdx);

                    if (title.isBlank() || author.isBlank()) {
                        errors.add("Line " + lineNum + ": skipped — title or author is blank.");
                        skipped++;
                        continue;
                    }

                    int year  = parseIntSafe(get(cols, yearIdx), 0);
                    int pages = parseIntSafe(get(cols, pagesIdx), 1);

                    if (year <= 0) {
                        errors.add("Line " + lineNum + ": skipped — invalid year '" + get(cols, yearIdx) + "'.");
                        skipped++;
                        continue;
                    }
                    if (pages <= 0) pages = 1; // default to 1 to avoid validation failure

                    String genre     = get(cols, genreIdx);
                    String publisher = get(cols, publisherIdx);
                    double rating    = parseDoubleSafe(get(cols, ratingIdx), 0.0);
                    if (rating < 0.0 || rating > 5.0) rating = 0.0;

                    ReadingStatus status = parseStatus(get(cols, statusIdx));
                    LocalDate dateAdded     = parseDate(get(cols, dateAddedIdx));
                    LocalDate dateCompleted = parseDate(get(cols, dateCompletedIdx));

                    if (dateAdded == null) dateAdded = LocalDate.now();

                    Book book = new Book(0, title, author, year, pages, genre, publisher,
                            rating, status, dateAdded, dateCompleted);
                    dao.insert(book);
                    imported++;

                } catch (Exception e) {
                    errors.add("Line " + lineNum + ": error — " + e.getMessage());
                    skipped++;
                }
            }

        } catch (IOException e) {
            errors.add("Could not open file: " + e.getMessage());
        }

        return new ImportResult(imported, skipped, errors);
    }

    // ======================== PRIVATE HELPERS ========================

    private static int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private static String get(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return "";
        return cols[idx].trim();
    }

    private static int parseIntSafe(String s, int defaultVal) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return defaultVal; }
    }

    private static double parseDoubleSafe(String s, double defaultVal) {
        try { return Double.parseDouble(s.trim().replace(",", ".")); } catch (Exception e) { return defaultVal; }
    }

    private static ReadingStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return ReadingStatus.NOT_READ;
        try { return ReadingStatus.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return ReadingStatus.NOT_READ; }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(s.trim(), fmt); }
            catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    /**
     * Splits a CSV line respecting double-quoted fields that may contain commas.
     *
     * @param line a raw CSV line
     * @return array of field values with surrounding quotes stripped
     */
    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Handle escaped double-quote ("")
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    // ======================== RESULT ========================

    /**
     * Immutable value object returned by {@link #importFromFile(String)}.
     */
    public static final class ImportResult {

        private final int imported;
        private final int skipped;
        private final List<String> errors;

        public ImportResult(int imported, int skipped, List<String> errors) {
            this.imported = imported;
            this.skipped  = skipped;
            this.errors   = List.copyOf(errors);
        }

        /** @return number of books successfully inserted */
        public int getImported() { return imported; }

        /** @return number of rows that were skipped due to errors */
        public int getSkipped() { return skipped; }

        /** @return list of error/warning messages (never null) */
        public List<String> getErrors() { return errors; }

        /** @return true if at least one book was imported without fully aborting */
        public boolean isSuccess() { return imported > 0; }

        @Override
        public String toString() {
            return "ImportResult[imported=" + imported + ", skipped=" + skipped
                    + ", errors=" + errors.size() + "]";
        }
    }
}
