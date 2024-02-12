package se.kth.ci;

// HTTP server utilities
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

// I/O
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
//db
import java.sql.Statement;
// library for timestamp
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
// regex
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Recursive directory deletion
import org.apache.commons.io.FileUtils;
// JSON parsing utilities
import org.json.JSONObject;

/**
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 */
public final class CIServer {
    
    // initilize new database 
    private Database mydb; 
    /**
     * Public constructor for a CI server.
     *
     * @param port the port number to listen traffic on
     * @param endpoint String : the endpoint to send webhooks to
     */
    public CIServer(int port, String endpoint, String buildDirectory) {

        // Set up port to listen on
        port(port);
        mydb = new Database();


        // ------------------------------- Launching the server ------------------------------- //
        get("/builds/:commitId", (req, res) -> {
            System.out.println("GET request received.");
            String commitId = req.params(":commitId");
            System.out.println("Fetching build info for commitId: " + commitId);

            // Fetch build info for specific build
            String[] buildInfo = getBuildInfoByCommitId(commitId);
            if (buildInfo != null) {
                System.out.println("Build info found for commitId: " + commitId);
                res.type("text/html");
                // This formats a nice table with green, white and grey colors.
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
        get("/builds", (req, res) -> {
            // Get build info for all commits
            List<String[]> allBuilds = mydb.getAllBuilds(); // Assuming mydb is accessible here and has a method getAllBuilds
            StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html>
                <head>
                <title>All Builds</title>
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
                            <td>%s</td>
                            <td>%s</td>
                            <td><pre>%s</pre></td>
                        </tr>
                        """, build[0], build[1], build[2]));
                }

                html.append("""
                    </table>
                    </body>
                    </html>
                    """);
                res.type("text/html");
                return html.toString();
            });

        post(endpoint, (req, res) -> {
            System.out.println("POST request received.");
            try {
             //   System.out.println("this is the request: " + req.body());
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
                String[] allBuildInfoExceptLog = getBuildInfo(req.body());
                String buildLog = readLogFileToString("build.log");
                System.out.println(buildLog);
                // kalla på databasen

                // allBuildInfo returns {commitID, timeStamp}
                mydb.insertBuild(mydb.getConnection(), allBuildInfoExceptLog[0], allBuildInfoExceptLog[1], buildLog);
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
                writeBuildLogToFile(buildProcess.getInputStream(), "build.log");
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
     * Writes the gradle build log to the specified file
     * @param inputStream
     * @param filePath
     */
    public static void writeBuildLogToFile(InputStream inputStream, String filePath){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath)); 
            String line;
             while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine(); // Add a newline character after each line
            }
            writer.close();
        }
        catch(Exception e){
            System.out.println("error when writing build log to file " + e);
        }
    }
    /**
     * Reads the specified log file
     * @param filePath
     * @return content - the contens of the file
     */
    public static String readLogFileToString(String filePath) {
        String content = "";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            content = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
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


    /**
     * primary key: commit id: 
     * build date
     * build logs
     */
    public String[] getBuildInfo(String response){
        
        JSONObject obj = new JSONObject(response);
        // Commit id can be found in url: linkToRepo/commit/commitID
        String url = obj.getJSONObject("head_commit").getString("url");

        // Define a regular expression pattern to extract the hash from the URL
        Pattern pattern = Pattern.compile("/commit/([a-fA-F0-9]+)");
        Matcher matcher = pattern.matcher(url);
        String commitID = "";
        if (matcher.find()) {
            commitID = matcher.group(1);
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = currentDateTime.format(formatter);
    
        return new String[]{commitID, timestamp};
    }
    /**
     * Get the build info for a specific commit
     * @param commitId
     * @return buildInfo, an array {commit_id, build_date, build_logs}
     */
    private String[] getBuildInfoByCommitId(String commitId) {
    String sql = "SELECT * FROM build_history WHERE commit_id = '" + commitId + "'";
    try (Statement stmt = mydb.getConnection().createStatement()){
        System.out.println("Trying to get build info for " + commitId);
        // Execute the query and obtain the result set
        ResultSet rs = stmt.executeQuery(sql);
        
        // Process the result set
        if (rs.next()) {
            String[] buildInfo = new String[3];
            buildInfo[0] = rs.getString("commit_id"); // Commit ID
            buildInfo[1] = rs.getString("build_date"); // Build Date
            buildInfo[2] = rs.getString("build_logs"); // Build Logs
            return buildInfo;
        }
        System.out.println();
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return null; // Return null if no build information was found
}

}
