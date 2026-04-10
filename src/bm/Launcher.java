package bm;

/**
 * Clean wrapper class to bootstrap the JavaFX application.
 * Required to bypass Java 11+ module restrictions when running from a shaded Fat JAR.
 *
 * @author Gómez Nido Gonzalo
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
