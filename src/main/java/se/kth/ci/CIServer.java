package se.kth.ci;

// HTTP server utilities
import static spark.Spark.*;

import java.io.BufferedReader;
// I/O
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

// Library for timestamp
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

// Regex
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Recursive directory deletion
import org.apache.commons.io.FileUtils;



// JSON parsing utilities
import org.json.JSONObject;


/**
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 */
public final class CIServer {

    private final Database db;
    private final int port;

    /**
     * Public constructor for a CI server.
     * @param port the port number to listen traffic on
     * @param endpoint String : the endpoint to send webhooks to (e.g. "/webhook")
     * @param buildDirectory String : the directory in which to clone, build and test repositories
     */
    public CIServer(int port, String endpoint, String buildDirectory) {

        port(port);
        this.port = port;
        this.db = new Database("jdbc:sqlite:build_history.db");

        // ---------------------------- Launching the server by configuring routes ---------------------------- //

        // Route for a specific build
        get(endpoint + "/builds/:commitId", (req, res) -> {
            String commitId = req.params(":commitId");
            String[] buildInfo = db.getBuild(commitId);
            if (buildInfo != null) {
                System.out.println("Build info found for commitId: " + commitId);
                res.type("text/html");
                return String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 20px; }
                        table { width: 100%%; border-collapse: collapse; }
                        th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                        th { background-color: #4CAF50; color: white; }
                        tr:nth-child(even) { background-color: #f2f2f2; }
                        tr:hover { background-color: #ddd; }
                        pre { white-space: pre-wrap; word-wrap: break-word; }
                    </style>
                    </head>
                    <body>
                    <table>
                    <tr><th>Commit ID</th><th>Build Date</th><th>Build Logs</th></tr>
                    <tr>
                        <td>%s</td> <!-- Commit ID -->
                        <td>%s</td> <!-- Build Date -->
                        <td><pre>%s</pre></td> <!-- Build Logs -->
                    </tr>
                    </table>
                    </body>
                    </html>
                    """, buildInfo[0], buildInfo[1], buildInfo[2]);
            } else {
                System.out.println("No build info found for commitId: " + commitId);
                res.status(404);
                return "<!DOCTYPE html><html><head><title>Not Found</title></head>" +
                        "<body><h1>404 Not Found</h1>" +
                        "<p>Build information not found for commit ID: " + commitId + "</p>" +
                        "</body></html>";
            }
        });

        // Route for all build history
        get(endpoint + "/builds", (req, res) -> {
            List<String[]> allBuilds = db.getAllBuilds();
            Collections.reverse(allBuilds);
            StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html>
                <head>
                <title>Build history</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    table { width: 100%; border-collapse: separate; border-spacing: 0; } /* Adjusted */
                    th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; border-right: 1px solid #ddd; } /* Adjusted */
                    th { background-color: #4CAF50; color: white; border-right: 1px solid #ddd; } /* Adjusted */
                    tr:nth-child(even) { background-color: #f2f2f2; }
                    tr:hover { background-color: #ddd; }
                    pre { white-space: pre-wrap; word-wrap: break-word; }
                    th, tr:first-child th, tr:first-child td { border-top: 1px solid #ddd; } /* New */
                    td, th { border-left: 1px solid #ddd; } /* New */
                    td:first-child, th:first-child { border-left: none; } /* New */
                </style>
                </head>
                <body>
                <h1>All Build Information</h1>
                <table>
                <tr><th>Commit ID</th><th>Build Date</th><th>Build Logs</th></tr>
                """);

                for (String[] build : allBuilds) {
                    html.append(String.format("""
                        <tr>
                            <td><a href="./builds/%s">%s</a></td>
                            <td>%s</td>
                            <td><pre>%s</pre></td>
                        </tr>
                        """, build[0], build[0], build[1], build[2]));
                }

                html.append("""
                    </table>
                    </body>
                    </html>
                    """);
                res.type("text/html");
                return html.toString();
            });

        // Route for receiving webhooks
        post(endpoint, (req, res) -> {
            System.out.println("POST request received.");
            try {
                String[] parameters = parseResponse(req.body());
                String repoURL = parameters[0],
                        branchName = parameters[1],
                        commitID = parameters[2],
                        timestamp = parameters[3];
                String repoName = repoURL.substring("https://github.com/".length());
                System.out.println("Cloning repository...");
                ErrorCode exitCode = cloneRepository(repoURL, branchName, buildDirectory);
                if (exitCode == ErrorCode.SUCCESS) {
                    System.out.println("Running build...");
                    exitCode = triggerBuild(buildDirectory);
                    if (exitCode == ErrorCode.SUCCESS) {
                        setCommitStatus(exitCode, repoName, commitID, "ci_build", "build succeeded");
                        System.out.println("Running tests..");
                        exitCode = triggerTesting(buildDirectory);
                        if (exitCode == ErrorCode.SUCCESS) {
                            setCommitStatus(exitCode, repoName, commitID, "ci_test", "testing succeeded");
                        } else {
                            setCommitStatus(exitCode, repoName, commitID, "ci_test", "testing failed");
                        }
                    } else {
                        setCommitStatus(exitCode, repoName, commitID, "ci_build", "build failed");
                    }
                    System.out.println("Build directory deleted.");
                }
                String buildLog = Utils.readFromFile("build.log");
                db.insertBuild(commitID, timestamp, buildLog);
            } catch (org.json.JSONException e) {
                System.out.println("Error while parsing JSON. \n" + e.getMessage());
            } finally {
                FileUtils.deleteDirectory(new File(buildDirectory));
            }
            return "";
        });

        System.out.println("Server started...");
    }


    /**
     * Method for parsing JSON response from GitHub webhook
     * into relevant parameters.
     * @param response String : the request body to be parsed
     * @return String[] : an array containing the repository URL, the branch name, the commit ID and the timestamp
     */
    public String[] parseResponse(String response) throws org.json.JSONException {
        JSONObject obj = new JSONObject(response);
        String branch = obj.getString("ref").substring("refs/heads/".length());
        String repoURL = obj.getJSONObject("repository").getString("url");
        String url = obj.getJSONObject("head_commit").getString("url");

        // commit ID
        Pattern pattern = Pattern.compile("/commit/([a-fA-F0-9]+)");
        Matcher matcher = pattern.matcher(url);
        String commitID = "";
        if (matcher.find()) {
            commitID = matcher.group(1);
        }

        // timestamp
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = currentDateTime.format(formatter);

        return new String[]{repoURL, branch, commitID, timestamp};
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
        System.out.println("Building repository in " + buildDirectory);
        if (repoDirectory.exists() && repoDirectory.isDirectory()) {
            String[] buildCommand = new String[]{"./gradlew",  "build", "-x", "test"};
            try {
                Process buildProcess = Runtime.getRuntime().exec(buildCommand, null, repoDirectory);
                Utils.writeToFile(buildProcess.getInputStream(), "build.log");
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
                Utils.writeToFile(testProcess.getInputStream(), "build.log");
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
     * Method for setting the commit status after running the build and tests.
     * @param code ErrorCode: result of previous operations
     * @param repoName String: name of repository
     * @param sha String: id of commit
     * @param context String: a string label to differentiate this status from the status of other systems
     * @param desc String: a short description of the status
     * @return ErrorCode : exit code of the operation
     */
    ErrorCode setCommitStatus(ErrorCode code, String repoName, String sha, String context, String desc){
        String projectRepo = "JEANPRBT",
                testRepo = "rickardo-cornelli",
                projectToken = "11APXT2FI056N0OHxXZY0R_cIx1HsKGsaD07M7zppm6rWLSX33ULJLRXMAp4nADVTxF4D6K5ET0xmJSn62",
                testToken = "11ASH6MUI0Y23d08dPLkwi_WdzkEkEvNcLScCaUGus4EtHMPACst8VeXetnvLFIZX9CIUO74NYSiAnebtC";
        String token;
        if (repoName.contains(projectRepo)) {
             token = projectToken;
        } else if (repoName.contains(testRepo)) {
            token = testToken;
        } else {
            System.err.println("Missing authorization token");
            return ErrorCode.ERROR_STATUS;
        }
        String auth = "Authorization: Bearer github_pat_" + token,
            state = code == ErrorCode.SUCCESS ? "success" : "failure",
            mes =
                "{" +
                    "\"target_url\":\"http://localhost:" + port + "/builds/" + sha + "\"," +
                    "\"state\":\"" + state + "\"," +
                    "\"context\":\"" + context + "\"," +
                    "\"description\":\"" + desc + "\"" +
                "}",
            url = "https://api.github.com/repos/" + repoName + "/statuses/" + sha;

        try {
            String[] command = {
                "curl", "-v", "-Li",
                "-X", "POST",
                "-H", "Accept: application/vnd.github+json",
                "-H", auth,
                "-H", "X-GitHub-Api-Version: 2022-11-28", url,
                "-d", mes
            };

            Process process = Runtime.getRuntime().exec(command);

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode == 0 && line.contains("HTTP/2 201")) {
                return ErrorCode.SUCCESS;
            } else {
                System.out.println("Curl command executed with exit code : " + exitCode);
                if (line != null) {
                    System.out.println("HTTP status : " + line);
                }
                return ErrorCode.ERROR_STATUS;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error while setting commit status. " + e.getMessage());
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


