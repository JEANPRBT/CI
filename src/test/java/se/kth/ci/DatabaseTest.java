package se.kth.ci;

import static org.junit.jupiter.api.Assertions.*;

// io
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// db related imports
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;


class DatabaseTest {
    private static Database extractTestDB;
    private static Database insertTestDB;

    /**
     * Start the server properly before the tests
     */
    @BeforeAll
    static void startDatabase() {

        String extractURL = "jdbc:sqlite:src/test/resources/test_database.db";
        String insertURL = "jdbc:sqlite:src/test/resources/insert_test.db";
        try {
            extractTestDB= new Database(extractURL);
            insertTestDB = new Database(insertURL);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Drops the insertTestDB's table after all tests are done.
     * This is required since we would have to change the primary key 
     * each run otherwise.
     */
    @AfterAll
    /**
     * Deletes the specified table. used primarily for testing
     * @param conn
     * @param tableName
     */
    static void deleteInsertDB(){
        try (Statement stmt = insertTestDB.getConnection().createStatement()){
            String sql = "DROP TABLE IF EXISTS build_history";
            stmt.execute(sql);
            System.out.println("Table '" + "build_history"+ "' deleted successfully.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    /**
     * Verifies that the data is correct when gathering all data from the database
     * @throws IOException
     */
    @Test
    public void getAllDataFromDatabase() throws IOException{
        List<String[]> dataExtractions = extractTestDB.getAllBuilds();
        String expectedDate = "2024-02-13 14:13:38";
        for(int i = 0; i < 3; i++){
            assertEquals("test" + (i + 1), dataExtractions.get(i)[0]);
            assertEquals(expectedDate, dataExtractions.get(i)[1]);
            assertEquals("executed 6 tasks, everything ok (this is test #" + (i+1) + ")", dataExtractions.get(i)[2]);
        }

    }

    /**
     * Tests that you can get data for a specific commit/entry in a database
     * @throws IOException
     */
    @Test
    public void getSpecificDataFromDatabase() throws IOException{
        String commitID = "test2";
        String expectedDate = "2024-02-13 14:13:38";
        String[] fetchedData = extractTestDB.getBuildInfoByCommitId(commitID);
        assertEquals("test2", fetchedData[0]);
        assertEquals(expectedDate, fetchedData[1]);
        assertEquals("executed 6 tasks, everything ok (this is test #2)", fetchedData[2]);
    }
    /**
     * Tests that you can insert data to our implementation of a  database
     * @throws IOException
     */
   @Test
   public void insertData() throws IOException{
        String commitID = "test1000";
        String testDate = "2024-02-13 15:49:36"; 
        String testLog = "executed 1000 tasks, everything is fine";
        assertEquals(ErrorCode.SUCCESS, insertTestDB.insertBuild(commitID, testDate, testLog));
   } 
}
