package se.kth.ci;

// HTTP server utilities
import static spark.Spark.*;

// I/O
import java.io.File;
import java.io.IOException;

// Recursive directory deletion
import org.apache.commons.io.FileUtils;
// JSON parsing utilities
import org.json.JSONObject;

/**
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 */
public final class CIServer {

    /**
     * Public constructor for a CI server.
     *
     * @param port the port number to listen traffic on
     * @param endpoint String : the endpoint to send webhooks to
     */
    public CIServer(int port, String endpoint, String buildDirectory) {

        // Set up port to listen on
        port(port);

        // ------------------------------- Launching the server ------------------------------- //
        get(endpoint, (req, res) -> {
            System.out.println("GET request received.");
            return "CI Server for Java Projects with Gradle.";
        });

        post(endpoint, (req, res) -> {
            System.out.println("POST request received.");
            try {
                String[] parameters = parseResponse(req.body());
                ErrorCode exitCode = cloneRepository(parameters[1], parameters[0], buildDirectory);
                if (exitCode == ErrorCode.SUCCESS) {
                    exitCode = triggerBuild(buildDirectory);
                    if (exitCode == ErrorCode.SUCCESS) {
                        System.out.println("Build was successful.");
                    } else {
                        System.out.println("Build failed.");
                    }
                    FileUtils.deleteDirectory(new File(buildDirectory));
                    System.out.println("Build directory deleted.");
                }
            } catch (org.json.JSONException e) {
                System.out.println("Error while parsing JSON. \n" + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error while deleting build directory. \n" + e.getMessage());
            }
            return "";
        });

        System.out.println("Server started...");
    }

    /**
     * Method for cloning the repository corresponding to the given URL and branch name.
     * It clones the repository in the folder `to_build`.
     * @param repoURL String : URL of the repository to be built
     * @param branchName String : branch on which push was made
     * @return ErrorCode : exit code of the operation
     */
    public ErrorCode cloneRepository(String repoURL, String branchName, String buildDirectory){
        String[] cloneCommand = new String[]{
                "git",
                "clone", repoURL,
                "--branch", branchName,
                "--single-branch",
                buildDirectory};
        try {
            Process cloneProcess = Runtime.getRuntime().exec(cloneCommand);
            int cloneExitCode = cloneProcess.waitFor();
            if (cloneExitCode == 0) {
                System.out.println("Repository cloned successfully.");
                return ErrorCode.SUCCESS;
            } else {
                System.err.println("Failed to clone repository. Exit code: " + cloneExitCode);
                return ErrorCode.ERROR_CLONE;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error running shell commands " + e.getMessage());
            return ErrorCode.ERROR_IO;
        }
    }

    /**
     * Method for triggering the build process for the repository in the `to_build` directory.
     * @return ErrorCode : exit code of the operation
     */
    public ErrorCode triggerBuild(String buildDirectory){
        File repoDirectory = new File(buildDirectory);
        if (repoDirectory.exists() && repoDirectory.isDirectory()) {
            System.out.println("Directory exists.");
            String[] buildCommand = new String[]{"./gradlew",  "build", "testClasses", "-x", "test"};
            try {
                Process buildProcess = Runtime.getRuntime().exec(buildCommand, null, repoDirectory);
                int buildExitCode = buildProcess.waitFor();
                if (buildExitCode == 0) {
                    System.out.println("Build for branch succeeded.");
                    return ErrorCode.SUCCESS;
                } else {
                    System.err.println("Build for branch failed. Exit code: " + buildExitCode);
                    return ErrorCode.ERROR_BUILD;
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error running shell commands " + e.getMessage());
                return ErrorCode.ERROR_IO;
            }
        } else {
            System.err.println("Repository directory does not exist: " + buildDirectory);
            return ErrorCode.ERROR_FILE;
        }
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
