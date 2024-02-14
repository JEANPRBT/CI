package se.kth.ci;

/**
 * Main class to launch the CI server.
 */
public class Main {

    /**
     * Private constructor to prevent instantiation.
     */
    private Main() {}

    /**
     * Instantiates a new CI server and launches it.
     * Triggered with the Gradle task ./gradlew run.
     * @param args String[] : command line arguments, useless here
     */
    public static void main(String[] args) {
        // launch server
        CIServer server = new CIServer(8029, "/", "to_build");
    }
}
