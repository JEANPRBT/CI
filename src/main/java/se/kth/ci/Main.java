package se.kth.ci;

public class Main {
    public static void main(String[] args) {

        // launch server
        CIServer server = new CIServer(8080, "/");
        Database mydb = new Database();
    }
}
