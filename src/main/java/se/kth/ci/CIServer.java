package se.kth.ci;

// HTTP server utilities
import static spark.Spark.*;

// JSON parsing utilities
import org.json.*;

import java.io.File;
import java.io.IOException;


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

        String cloneCommand = "git clone --single-branch --branch ";

        try {

            Process cloneProcess = Runtime.getRuntime().exec(new String[]{cloneCommand});
            int cloneExitCode = cloneProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error cloning repository or executing build command: " + e.getMessage());
        }


    }

    /**
     * Private method for parsing JSON response from GitHub webhook into relevant
     * parameters for triggering build process.
     * @param response String : the request body to be parsed
     */
    private void parseResponse(String response){
        try {
            JSONObject obj = new JSONObject(response);

            // Retrieve the branch
            String branch = obj.getString("ref").substring("refs/heads/".length());

            String repoURL = obj.getJSONObject("repository").getString("url");
            System.out.println(repoURL);




        } catch (org.json.JSONException e){
            System.out.println("Error while parsing JSON.");
        }
    }
}
