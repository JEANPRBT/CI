package se.kth.ci;

// HTTP server utilities
import static spark.Spark.*;

// JSON parsing utilities
import org.json.*;


/**
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 */
public final class CIServer {
    public CIServer(int port, String path) {
        System.out.println("Server started...");
        port(port);
        get(path, (req, res) -> {
            System.out.println("GET request received.");
            return "";
        });
        post(path, (req, res) -> {
            System.out.println("POST request received.");
            parseResponse(req.body());
            return "";
        });
    }

    private void handleRequest(){



    }

    private void parseResponse(String response){

        try {
            JSONObject obj = new JSONObject(response);
            String branch = obj.getString("refs").substring("refs/heads/".length());
            System.out.println(branch);

        } catch (org.json.JSONException e){
            System.out.println("Error while parsing JSON.");
            // TODO : implement a better exception handling
        }
        System.out.println("exited the loop");


    }
}
