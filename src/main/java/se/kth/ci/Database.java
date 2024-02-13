package se.kth.ci;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;



public final class Database{
    private Connection conn;

    /**
     * Constructor which establishes connection to the database and creates a table if it does not exist.
     * @param url String: link to the database, for example "jdbc:sqlite:build_history.db";
     */
    public Database(String url){
        try {
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        createTable();
    }

    /**
     * Method for creating the main table which will includes the build information
     */
    public void createTable(){
        String sql = "CREATE TABLE IF NOT EXISTS build_history (" +
                     "commit_id TEXT PRIMARY KEY ," +
                     "build_date TEXT NOT NULL," +
                     "build_logs TEXT NOT NULL)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error while creating the table. " + e.getMessage());
        }
    }
    
    /**
     * Method for inserting new builds into the table.
     * @param commitId: the commit ID of the build
     * @param buildDate String: the date of the build
     * @param buildLogs String: the build logs
     */
    public ErrorCode insertBuild(String commitId, String buildDate, String buildLogs){
        String sql = "INSERT INTO build_history (commit_id, build_date, build_logs) VALUES ( ?, ?, ?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, commitId);
            preparedStatement.setString(2, buildDate );
            preparedStatement.setString(3, buildLogs);
            preparedStatement.executeUpdate();
            System.out.println("Values inserted into the build_history table successfully.");
            return ErrorCode.SUCCESS;
        } catch (SQLException e) {
            System.out.println("Error while inserting values into the build_history table. " + e.getMessage());
            return ErrorCode.ERROR_INSERT_DB;
        }
    }

      /**
     * Get the build info for a specific commit.
     * @param commitId the commit ID of the build
     * @return String[] : an array containing commitId, buildDate and buildLogs
     */
    public String[] getBuild(String commitId) {
        String sql = "SELECT * FROM build_history WHERE commit_id = '" + commitId + "'";
        try (Statement stmt = conn.createStatement()){
            System.out.println("Trying to get build info for " + commitId);
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return new String[]{
                        rs.getString("commit_id"),
                        rs.getString("build_date"),
                        rs.getString("build_logs")
                };
            }
        } catch (SQLException e) {
            System.out.println("Error while retrieving build info " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetch data from all entries in the table.
     * @return List<String[]> : a list of arrays containing commitId, buildDate and buildLogs
     */
    public List<String[]> getAllBuilds() {
        List<String[]> builds = new ArrayList<>();
        String sql = "SELECT commit_id, build_date, build_logs FROM build_history";
        try (Statement stmt = this.conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                builds.add(new String[]{
                    rs.getString("commit_id"),
                    rs.getString("build_date"),
                    rs.getString("build_logs")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching builds. " + e.getMessage());
        }
        return builds;
    }
}

