package se.kth.ci;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;


class CIServerTest {

    private static CIServer server;

    /**
     * Start the server properly before the tests
     */
    @BeforeAll
    static void startServer() {
        server = new CIServer(8080, "/");
    }

    /**
     * Stop the server after all tests are done
     */
    @AfterAll
    static void stopServer() {
        spark.Spark.stop();
    }
    /**
     * Test for method `parseResponse`.
     * Checks that it extracts good information from JSON String.
     */
    @Test
    public void parseResponsePositiveTest(){
        String json = "{\"ref\":\"refs/heads/testing_value_ref\", \"repository\": {\"url\": \"https://testing_value_url\"}}";
        String[] expected = new String[]{"testing_value_ref", "https://testing_value_url"};
        try {
            assertArrayEquals(expected, server.parseResponse(json), "JSON string was not parsed correctly.");
        } catch (org.json.JSONException e){
            System.err.println("Error while parsing JSON");
        }
    }

    /**
     * Test for method `parseResponse`.
     * Checks that it cannot parse a non-JSON string;
     */
    @Test
    public void parseResponseThrows(){
        String notJson = "This is not a JSON string.";
        assertThrows(org.json.JSONException.class, () -> {
            server.parseResponse(notJson);
        }, "Method parsed a non-JSON string.");
    }

    /**
     * Test handling requests, verify that repo is cloned.
     * Note: verify that everything is deleted in to_build
     */
    @Test
    public void testValidURLandBranch(){
        String branchName = "main";
        String repoURL = "https://github.com/Zaina-ram/testRepo.git";
        try{
            int exitCode = server.handleRequest(branchName, repoURL);
            assertEquals(exitCode, 1, "Expected handleRequest to return 1, got " + exitCode);
        }catch(Exception e){
            fail("Test failed with exception " + e);
        }
    }
    /**
     * Tests that an invalid branch is not cloned for 
     * a valid URL. Exit code 0 means it fails
     */

    @Test
    public void testValidURLInvalidBranch(){
        String branchName = "invalidBranch";
        String repoURL = "https://github.com/Zaina-ram/testRepo.git";
        try{
            int exitCode = server.handleRequest(branchName, repoURL);
            assertEquals(exitCode, 0, "Expected handleRequest to return 0, got " + exitCode);
        }catch(Exception e){
            fail("Test failed with exception " + e);
        }

    }

    /**
     * Tests that an invalid repo URL doesn't lead to the cloning of a repo
     * 
     */
    @Test
    public void testInvalidURL(){
        String branchName = "invalidBranch";
        String repoURL = "https://github.com/rickardo-cornelli/invalidRepo.git";
        try{
            int exitCode = server.handleRequest(branchName, repoURL);
            assertEquals(exitCode, 0, "Expected handleRequest to return 0, got " + exitCode);
        }catch(Exception e){
            fail("Test failed with exception " + e);
        }
    }

    @Test
    public void runTestsForValidURLandBranch(){
        String branchName = "main";
        String repoURL = "https://github.com/Zaina-ram/testRepo.git";
        try{
            server.handleRequest(branchName, repoURL);
            int exitCode = server.triggerTesting(branchName);
            server.deleteDir();
            assertEquals(exitCode, 1, "Expected handleRequest to return 1, got " + exitCode);
        }catch(Exception e){
            fail("Test failed with exception " + e);
        }
    }
}