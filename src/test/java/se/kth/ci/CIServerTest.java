package se.kth.ci;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

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
        String json = "{\"ref\":\"refs/heads/testing_value_ref\", \"repository\": {\"url\": \"https://testing_value_url\",\"head_commit\": {\"url\": \"https://testing_value_url/123\",\"head_commit\": {\"timestamp\": \"2024-02-12T10:17:49+01:00\"}}";
        String[] expected = new String[]{"https://testing_value_url", "testing_value_ref", "123", "024-02-12T10:17:49+01:00"};
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
        assertNotEquals(ErrorCode.SUCCESS, exitCode, "A valid repository was not cloned successfully.");
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
        File repository = new File("src/test/resources/invalid_build");
        try {
            ErrorCode exitCodeBuild = server.triggerBuild(repository.getAbsolutePath());
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
        File repository = new File("src/test/resources/valid_build");
        try {
            ErrorCode exitCodeBuild = server.triggerBuild(repository.getAbsolutePath());
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
        File repository = new File("src/test/resources/valid_build");
        try {
            ErrorCode exitCodeTest = server.triggerTesting(repository.getAbsolutePath());
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
        File repository = new File("src/test/resources/valid_tests");
        ErrorCode exitCodeTest = server.triggerTesting(repository.getAbsolutePath());
        assertEquals(ErrorCode.SUCCESS, exitCodeTest, "Testing for valid tests failed.");
    }

    /**
     * Test for method `triggerTesting`
     * Checks that when a project has invalid tests, the method returns ERROR_TEST
     */
    @Test
    public void triggerInvalidTests(){
        File repository = new File("src/test/resources/invalid_tests");
        try {
            ErrorCode exitCodeTest = server.triggerTesting(repository.getAbsolutePath());
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
        ErrorCode expectedCode = server.setCommitStatus(
                ErrorCode.SUCCESS,
                "rickardo-cornelli/testRepo",
                "653cc3fc1350e2f3419850dc6e253950172eeb2f",
                "test",
                "valid commit test"
        );
        assertEquals(ErrorCode.SUCCESS, expectedCode, "Setting commit status failed");
    }

    /**
     * Test for method 'setCommitStatus'
     * Checks that when setting the status of a non-existent commit, the method returns ERROR_STATUS
     */
    @Test
    public void statusInvalidCommitId(){
        ErrorCode expectedCode = server.setCommitStatus(
                ErrorCode.SUCCESS,
                "rickardo-cornelli/testRepo",
                "abc",
                "test",
                "invalid commit test"
        );
        assertEquals(ErrorCode.ERROR_STATUS, expectedCode,
                expectedCode == ErrorCode.SUCCESS?
                        "Setting commit status of non-existent commit succeeded." :
                        "Error while trying to set commit status of invalid commit."
        );
    }

    /**
     * Test for method 'setCommitStatus'
     * Checks that the method returns ERROR_STATUS when trying to set commit status for a repo the CI server doesn't have access to set commit statuses for
     */
    @Test
    public void statusRepoWithoutAccess(){
        ErrorCode expectedCode = server.setCommitStatus(ErrorCode.SUCCESS,
                "noPermissions/repo",
                "abc",
                "test",
                "invalid commit test"
        );
        assertEquals(ErrorCode.ERROR_STATUS, expectedCode,
                expectedCode == ErrorCode.SUCCESS?
                        "Setting commit status of repo without access succeeded." :
                        "Error while trying to set commit status of repo without access."
        );
    }
}
