package se.kth.ci;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class CIServerTest {

    private static CIServer server;
    private static final String buildDirectory = "test_build";

    /**
     * Start the server properly before the tests
     */
    @BeforeAll
    static void startServer() {
        server = new CIServer(8080, "/", buildDirectory);
    }

    /**
     * Stop the server after all tests are done
     */
    @AfterAll
    static void stopServer() {
        spark.Spark.stop();
    }

    @AfterAll
    static void deleteBuildDirectory() {
        try {
            FileUtils.deleteDirectory(new File(buildDirectory));
        } catch (IOException e) {
            System.err.println("Error while deleting build directory.");
        }
    }

    /**
     * Test for method `parseResponse`.
     * Checks that it extracts good information from JSON String.
     */
    @Test
    public void parseResponsePositiveTest(){
        String json = "{\"ref\":\"refs/heads/testing_value_ref\", \"repository\": {\"url\": \"https://testing_value_url\",\"head_commit\": {\"url\": \"https://testing_value_url/123\",\"head_commit\": {\"timestamp\": \"2024-02-12T10:17:49+01:00\"}}";
        String[] expected = new String[]{"testing_value_ref", "https://testing_value_url","123", "024-02-12T10:17:49+01:00"};
        try {
            assertArrayEquals(expected, server.parseResponse(json), "JSON string was not parsed correctly.");
        } catch (org.json.JSONException e){
            System.err.println("Error while parsing JSON");
        }
    }

    /**
     * Test for method `parseResponse`.
     * Checks that it cannot parse a non-JSON string.
     */
    @Test
    public void parseResponseThrows(){
        String notJson = "This is not a JSON string.";
        assertThrows(org.json.JSONException.class, () -> server.parseResponse(notJson),
                "Method parsed a non-JSON string.");
    }

  

    /**
     * Test for method `handleRequest`
     * Checks that valid repo is cloned correctly on good branch, method should return SUCCESS.
     */
    @Test
    public void cloneValidURLandBranch(){
        String branchName = "turtles2";
        String repoURL = "https://github.com/rickardo-cornelli/testRepo.git";
        ErrorCode exitCode = server.cloneRepository(repoURL, branchName, buildDirectory);
        assertEquals(ErrorCode.SUCCESS, exitCode, "A valid repository was not cloned successfully.");
    }

    /**
     * Test for method `handleRequest`
     * Checks that valid repo is not cloned on invalid branch, method should return ERROR_CLONE.
     */
    @Test
    public void cloneValidURLInvalidBranch(){
        String branchName = "invalidBranch";
        String repoURL = "https://github.com/rickardo-cornelli/testRepo.git";
        ErrorCode exitCode = server.cloneRepository(repoURL, branchName, buildDirectory);
        assertEquals(ErrorCode.ERROR_CLONE, exitCode, "An invalid repository was cloned.");
    }

    /**
     * Test for method `handleRequest`
     * Checks that invalid repo is not cloned, method should return ERROR_CLONE.
     */
    @Test
    public void cloneInvalidURL(){
        String branchName = "invalidBranch";
        String repoURL = "https://github.com/rickardo-cornelli/invalidRepo.git";
        ErrorCode exitCode = server.cloneRepository(repoURL, branchName, buildDirectory);
        assertEquals(ErrorCode.ERROR_CLONE, exitCode, "An invalid repository was cloned.");
    }

    /**
     * Test for method `triggerBuild`
     * Checks that when an invalid build is triggered the method returns ERROR_BUILD.
    
    @Test
    public void triggerInvalidBuild(){
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String filePath = Objects.requireNonNull(classLoader.getResource("invalid_build_test")).getFile();
            ErrorCode exitCodeBuild = server.triggerBuild(filePath);
            assertEquals(ErrorCode.ERROR_BUILD, exitCodeBuild, "An invalid build was successful.");
        } catch (NullPointerException e){
            System.err.println("Error while getting file path.");
        }

    } */

    /**
     * Test for method `triggerBuild`
     * Checks that when a valid build is triggered the method returns SUCCESS.
    
    @Test
    public void triggerValidBuild(){
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String filePath = Objects.requireNonNull(classLoader.getResource("valid_build_test")).getFile();
            ErrorCode exitCodeBuild = server.triggerBuild(filePath);
            assertEquals(ErrorCode.SUCCESS, exitCodeBuild, "A valid build was not successful.");
        } catch (NullPointerException e){
            System.err.println("Error while getting file path.");
        }

    }
    */
    /**
     * Checks that query to database returns expected results. 
     * @throws IOException
     */

    @Test
    public void getDataFromDatabase() throws IOException{
        Database mydb = new Database(); 
        mydb.getConnection();
        String commitId = "3525b1231d11f9712740e28f3e4f5df6b79425bb";
        String timestamp = "2024-02-12 23:01:36";
        String buildLog; 
        byte[] bytes = Files.readAllBytes(Paths.get("build.log"));
        buildLog = new String(bytes);
        String[] buildInfo = server.getBuildInfoByCommitId(commitId);
        assertEquals(commitId, buildInfo[0], "Commit_id doesn't match");
        assertEquals(timestamp, buildInfo[1], "timestamp doesn't match");
        assertEquals(buildLog, buildInfo[2], "build log doesn't match");
    }



}
