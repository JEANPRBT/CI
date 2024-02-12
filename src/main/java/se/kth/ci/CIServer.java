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
                System.out.println("Cloning repository...");
                ErrorCode exitCode = cloneRepository(parameters[0], parameters[1], buildDirectory);
                if (exitCode == ErrorCode.SUCCESS) {
                    System.out.println("Running build...");
                    exitCode = triggerBuild(buildDirectory);
                    if (exitCode == ErrorCode.SUCCESS) {
                        System.out.println("Running tests..");
                        exitCode = triggerTesting(buildDirectory);
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
     * Method for parsing JSON response from GitHub webhook into relevant
     * parameters for triggering build process.
     * @param response String : the request body to be parsed
     * @return String[] : an array containing the repository URL and the branch name
     */
    public String[] parseResponse(String response) throws org.json.JSONException{
        JSONObject obj = new JSONObject(response);
        String repoURL = obj.getJSONObject("repository").getString("url");
        String branch = obj.getString("ref").substring("refs/heads/".length());
        return new String[]{repoURL, branch};
    }

    /**
     * Method for cloning the repository corresponding to the given URL and branch name.
     * It clones the repository in the folder `buildDirectory`.
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
     * Method for triggering the build process for the repository in the `buildDirectory`.
     * @param buildDirectory String : the directory in which to build the repository
     * @return ErrorCode : exit code of the operation
     */
    public ErrorCode triggerBuild(String buildDirectory){
        File repoDirectory = new File(buildDirectory);
        if (repoDirectory.exists() && repoDirectory.isDirectory()) {
            String[] buildCommand = new String[]{"./gradlew",  "build", "-x", "test"};
            try {
                Process buildProcess = Runtime.getRuntime().exec(buildCommand, null, repoDirectory);
                int buildExitCode = buildProcess.waitFor();
                if (buildExitCode == 0) {
                    System.out.println("Build succeeded.");
                    return ErrorCode.SUCCESS;
                } else {
                    System.err.println("Build failed. Exit code: " + buildExitCode);
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
     * Method for running tests in the `testDirectory`.
     * @param testDirectory String : the directory in which to run the tests
     * @return ErrorCode : exit code of the operation
     */
    public ErrorCode triggerTesting(String testDirectory) {
        File testDir = new File(testDirectory);
        if(!repositoryContainsTests(testDirectory)){
            System.out.println("No tests found.");
            return ErrorCode.NO_TESTS;
        }
        if (testDir.exists() && testDir.isDirectory()){
            String[] testCommand = new String[]{"./gradlew",  "test"};
            try {
                Process testProcess = Runtime.getRuntime().exec(testCommand, null, testDir);
                int testExitCode = testProcess.waitFor();
                if (testExitCode == 0) {
                    System.out.println("Tests succeeded.");
                    return ErrorCode.SUCCESS;
                } else {
                    System.err.println("Tests failed. Exit code: " + testExitCode);
                    return ErrorCode.ERROR_TEST;
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Error running shell commands " + e.getMessage());
                return ErrorCode.ERROR_IO;
            }
        }
        return ErrorCode.ERROR_IO;
    }

    // ------------------------------- Helper methods ------------------------------- //

    /**
     * Method for checking if the repository contains tests.
     * @param repositoryPath String : the repository to check
     * @return boolean : true if the repository contains tests, false otherwise
     */
    private boolean repositoryContainsTests(String repositoryPath) {
        File testDirectory = new File(repositoryPath + "/src/test/java");
        if (testDirectory.exists() && testDirectory.isDirectory()) {
            String[] files = testDirectory.list();
            return files != null && files.length > 0;
        }
        return false;
    }
}


