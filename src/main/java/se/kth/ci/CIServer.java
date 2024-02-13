package se.kth.ci;

// HTTP server utilities
import static spark.Spark.*;

import java.io.BufferedReader;
// I/O
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

// Recursive directory deletion
import org.apache.commons.io.FileUtils;
// JSON parsing utilities
import org.json.JSONObject;

// JSON parsing utilities
import org.json.JSONObject;

/**
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 */
public final class CIServer {

    /**
     * Public constructor for a CI server.
     * @param port the port number to listen traffic on
     * @param endpoint String : the endpoint to send webhooks to (e.g. "/webhook")
     * @param buildDirectory String : the directory in which to clone, build and test repositories
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
                        if (exitCode == ErrorCode.SUCCESS) {
                            setCommitStatus(exitCode, parameters[2], parameters[3], "ci", "build and testing succeeded");
                        } else {
                            setCommitStatus(exitCode, parameters[2], parameters[3], "ci", "testing failed");
                        }
                    } else {
                        setCommitStatus(exitCode, parameters[2], parameters[3], "ci", "build failed");
                    }
                    FileUtils.deleteDirectory(new File(buildDirectory));
                    System.out.println("Build directory deleted.");
                }
                else {
                    setCommitStatus(exitCode, parameters[2], parameters[3], "ci", "cloning repo failed");
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
        String repoName = obj.getJSONObject("repository").getString("full_name");
        String mostRecentCommit = obj.getString("after");
        return new String[]{repoURL, branch, repoName, mostRecentCommit};
    }

    /**
     * Method for cloning the repository corresponding to the given URL and branch name.
     * It clones the repository in the folder `buildDirectory`.
     * @param repoURL String : URL of the repository to be built
     * @param branchName String : branch on which push was made
     * @param buildDirectory String : the directory in which to clone the repository
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

    /**
     * Method for setting status of a commit
     * @param code result of previous operations
     * @param repoName name of repositroy
     * @param sha id of commit
     * @param context a string label to differentiate this status from the status of other systems. 
     * @param desc a short description of the status.
     * @return ErrorCode : exit code of the operation
     */
    ErrorCode setCommitStatus(ErrorCode code, String repoName, String sha, String context, String desc){
        String token = "11AEWS3IY0ovpurtpDfnRs_pmqMIWPl5eZvqUhf2lvlhxeryIMZgJLPsYKJzfQbNo6M4VTRWM6eKL5Wqn0",
            auth = "Authorization: Bearer github_pat_"+token,
            state = code == ErrorCode.SUCCESS ? "success" : "failure",
            mes = "{\"state\":\"" + state + "\",\"context\":\""+context+"\",\"description\":\""+desc+"\"}",
            url = "https://api.github.com/repos/" + repoName+ "/statuses/" + sha;

        try {
            String[] command = {
                "curl",
                "-v",
                "-Li",
                "-X",
                "POST",
                "-H",
                "Accept: application/vnd.github+json",
                "-H",
                auth,
                "-H",
                "X-GitHub-Api-Version: 2022-11-28",
                url,
                "-d",
                mes
            };
            // Create ProcessBuilder instance
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Redirect error stream to output stream for debugging
            // processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            // Read all input for debugging
            /* while ((line = reader.readLine()) != null) {
                System.out.println(line);
            } */
            line = reader.readLine();

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode == 0 && line.contains("HTTP/2 201")) {
                return ErrorCode.SUCCESS;
            } else {
                System.out.println("Curl command executed with exit code: " + exitCode);
                if (line != null) {
                    System.out.println("Http status: " + line);
                }
                return ErrorCode.ERROR_STATUS;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ErrorCode.ERROR_IO;
        }
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


