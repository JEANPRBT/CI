package se.kth.ci;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

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

    /**
     * Delete the build directory after all tests are done
     */
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
        String json = "{\"ref\":\"refs/heads/testing_value_ref\", \"repository\": {\"url\": \"https://testing_value_url\"}}";
        String[] expected = new String[]{"https://testing_value_url", "testing_value_ref"};
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
     */
    @Test
    public void triggerInvalidBuild(){
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String filePath = Objects.requireNonNull(classLoader.getResource("invalid_build")).getFile();
            ErrorCode exitCodeBuild = server.triggerBuild(filePath);
            assertEquals(ErrorCode.ERROR_BUILD, exitCodeBuild, "An invalid build was successful.");
        } catch (NullPointerException e){
            System.err.println("Error while getting file path.");
        }

    }

    /**
     * Test for method `triggerBuild`
     * Checks that when a valid build is triggered the method returns SUCCESS.
     */
    @Test
    public void triggerValidBuild(){
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String filePath = Objects.requireNonNull(classLoader.getResource("valid_build")).getFile();
            ErrorCode exitCodeBuild = server.triggerBuild(filePath);
            assertEquals(ErrorCode.SUCCESS, exitCodeBuild, "A valid build was not successful.");
        } catch (NullPointerException e){
            System.err.println("Error while getting file path.");
        }

    }

    /**
     * Test for method `triggerTesting`
     * Checks that when a project does not contain tests, the method returns NO_TESTS 
     */
    @Test
    public void repoWithoutTests(){
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String filePath = Objects.requireNonNull(classLoader.getResource("valid_build")).getFile();
            ErrorCode exitCodeTest = server.triggerTesting(filePath);
            assertEquals(ErrorCode.NO_TESTS, exitCodeTest, "Testing for a project without tests was triggered.");
        } catch (NullPointerException e){
            System.err.println("Error while getting file path.");
        }

    }

    /**
     * Test for method `triggerTesting`
     * Checks that when a project has valid tests, the method returns SUCCESS
     */
    @Test
    public void triggerValidTests(){
        ClassLoader classLoader = getClass().getClassLoader();
        String filePath = Objects.requireNonNull(classLoader.getResource("valid_tests")).getFile();
        ErrorCode exitCodeTest = server.triggerTesting(filePath);
        assertEquals(ErrorCode.SUCCESS, exitCodeTest, "Testing for valid tests failed.");

    }

    /**
     * Test for method `triggerTesting`
     * Checks that when a project has invalid tests, the method returns ERROR_TEST
     */
    @Test
    public void triggerInvalidTests(){
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String filePath = Objects.requireNonNull(classLoader.getResource("invalid_tests")).getFile();
            ErrorCode exitCodeTest = server.triggerTesting(filePath);
            assertEquals(ErrorCode.ERROR_TEST, exitCodeTest, "Testing for invalid tests was successful.");
        } catch (NullPointerException e){
            System.err.println("Error while getting file path.");
        }
    }


    /**
     * Test for method 'setCommitStatus'
     * Checks that when setting the status of an existing commit, the method returns SUCCESS
     */
    @Test
    public void statusValidCommitId(){
        ErrorCode expectedCode = server.setCommitStatus(ErrorCode.SUCCESS, "rickardo-cornelli/testRepo","653cc3fc1350e2f3419850dc6e253950172eeb2f", "test", "valid commit test");
        assertEquals(ErrorCode.SUCCESS, expectedCode, "Setting commit status failed");
    }

    /**
     * Test for method 'setCommitStatus'
     * Checks that when setting the status of an non-existent commit, the method returns ERROR_STATUS
     */
    @Test
    public void statusInvalidCommitId(){
        ErrorCode expectedCode = server.setCommitStatus(ErrorCode.SUCCESS, "rickardo-cornelli/testRepo","abc", "test", "invalid commit test");
        assertEquals(ErrorCode.ERROR_STATUS, expectedCode, expectedCode == ErrorCode.SUCCESS? "Setting commit status of non-existent commit succeeded" : "Error while trying to set commit of invalid commit");
    }
}
