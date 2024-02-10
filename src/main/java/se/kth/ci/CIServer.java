package se.kth.ci;

// HTTP server utilities
import static spark.Spark.*;

// I/O
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
     * @param path String : the endpoint to send webhooks to
     */
    public CIServer(int port, String path) {
        System.out.println("Server started...");

        // Set up port to listen on
        port(port);

        // GET requests handler
        // Modify to get list of build from data base. 
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
                triggerTesting(parameters[0]);
                deleteDir();
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
    public int handleRequest(String branchName, String repoURL) {
        
        int exitCode = 0;

        String directory = "to_build";
        

        String[] cloneCommand = new String[]{"git", "clone", repoURL, "--branch", branchName, "--single-branch", directory};
        for (String command : cloneCommand) {
            System.out.print(command + " ");
        }
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

                    String[] buildCommand = new String[]{"./gradlew.bat",  "build"};

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

            } else {
                System.err.println("Failed to clone repository. Exit code: " + cloneExitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error cloning repository or executing build cloneCommand: " + e.getMessage());
        }
        return exitCode;
    }
    /**
     * Method for running Junit tests.  
     * @throws InterruptedException 
     * @throws IOException 
     */
    public int triggerTesting(String branchName) throws InterruptedException, IOException{   
        int exitCode = 0;

        if (!Files.isDirectory(Paths.get("to_build/src/test"))) {
            System.out.println("Project does not contain tests.");
            return exitCode;
        }
        else{
            File testDir = new File("to_build");
            if (testDir.exists() && testDir.isDirectory()){
                System.out.println("Test directory exists, running tests.");
    
                String[] testCommand = new String[]{"./gradlew.bat",  "test"};
    
                // Execute the build command
                Process testProcess = Runtime.getRuntime().exec(testCommand, null, testDir);
                int testExitCode = testProcess.waitFor();
    
                if (testExitCode == 0) {
                    System.out.println("tests for branch " + branchName + " succeeded.");
                    exitCode = 1;
                } else {
                    System.err.println("tests for branch " + branchName + " failed. Exit code: " + testExitCode);
                }
            }
            
            return exitCode;
        }
    }
    
    /**
     * Delete Directory
     * @throws IOException 
     */
    public void deleteDir() throws IOException{
        File repoDirectory = new File("to_build");
        FileUtils.deleteDirectory(repoDirectory);
        System.out.println("Repository deleted successfully");
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
