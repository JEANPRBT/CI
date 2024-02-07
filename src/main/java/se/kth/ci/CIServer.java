package se.kth.ci;

// HTTP server utilities
import static spark.Spark.*;

// JSON parsing utilities
import org.json.*;

// I/O
import java.io.File;
import java.io.IOException;

// Recursive directory deletion
import org.apache.commons.io.FileUtils;

/**
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 */
public final class CIServer {

    /**
     * Public constructor for a CI server.
     *
     * @param port the port number to listen traffic on
     * @param path String : the endpoint to send webhooks to
     */
    public CIServer(int port, String path) {
        System.out.println("Server started...");

        // Set up port to listen on
        port(port);

        // GET requests handler
        get(path, (req, res) -> {
            System.out.println("GET request received.");
            return "CI Server for Java Projects with Gradle.";
        });

        // POST requests handler
        post(path, (req, res) -> {
            System.out.println("POST request received.");
            try {
                String[] parameters = parseResponse(req.body());
                int exitCode = handleRequest(parameters[0], parameters[1]);
            } catch (org.json.JSONException e) {
                System.out.println("Error while parsing JSON.");
            }
            return "";
        });
    }

    /**
     * Method for handling POST request from GitHub webhook and trigger the build.
     * It clones the corresponding branch of the target repo, and then launches the build operation.
     *
     * @param branchName String : the branch on which push was made
     * @param repoURL    String : the repository to be build URL
     * @return exit code, i.e 1 if build succeeded and 0 otherwise
     */
    private int handleRequest(String branchName, String repoURL) {

        int exitCode = 0;

        String directory = "to_build";

        String[] cloneCommand = new String[]{"git", "clone", repoURL, "--branch", branchName, "--single-branch", directory};

        try {

            // Execute the clone command
            Process cloneProcess = Runtime.getRuntime().exec(cloneCommand);
            int cloneExitCode = cloneProcess.waitFor();

            // Check if the clone was successful
            if (cloneExitCode == 0) {
                System.out.println("Repository cloned successfully.");

                // Change to the repository directory
                File repoDirectory = new File(directory);

                if (repoDirectory.exists() && repoDirectory.isDirectory()) {
                    System.out.println("Directory exists.");

                    String[] buildCommand = new String[]{"./gradlew",  "build"};

                    // Execute the build command
                    Process buildProcess = Runtime.getRuntime().exec(buildCommand, null, repoDirectory);
                    int buildExitCode = buildProcess.waitFor();

                    // Check if the build process was successful
                    if (buildExitCode == 0) {
                        System.out.println("Build for branch " + branchName + " succeeded.");
                        exitCode = 1;
                    } else {
                        System.err.println("Build for branch " + branchName + " failed. Exit code: " + buildExitCode);
                    }

                } else {
                    System.err.println("Repository directory does not exist: " + directory);
                }

                FileUtils.deleteDirectory(repoDirectory);
                System.out.println("Repository deleted successfully");

            } else {
                System.err.println("Failed to clone repository. Exit code: " + cloneExitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error cloning repository or executing build cloneCommand: " + e.getMessage());
        }
        return exitCode;
    }

    /**
     * Method for parsing JSON response from GitHub webhook into relevant
     * parameters for triggering build process.
     * @param response String : the request body to be parsed
     * @return String[] : an array containing the branch name and the repository URL
     */
    public String[] parseResponse(String response) throws org.json.JSONException{
        JSONObject obj = new JSONObject(response);
        String branch = obj.getString("ref").substring("refs/heads/".length());
        String repoURL = obj.getJSONObject("repository").getString("url");
        return new String[]{branch, repoURL};
    }
}
