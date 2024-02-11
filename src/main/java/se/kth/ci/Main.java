package se.kth.ci;

public class Main {
    public static void main(String[] args) {

        // launch server
        CIServer server = new CIServer(8029, "/", "to_build");
    }
}
