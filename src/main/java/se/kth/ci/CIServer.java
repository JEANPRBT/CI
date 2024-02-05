package se.kth.ci;

import static spark.Spark.*;


/**
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 */
public final class CIServer {
    public CIServer(int port, String path){
        System.out.println("Server started...");
        port(port);
        get(path, (req, res) -> {
            System.out.println("GET request received.");
            return "";
        });
        post(path, (req, res) -> {
            System.out.println("POST request received.");
            System.out.println(req.body());
            return "";
        });
    }

}
