package se.kth.ci;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;



public final class Database{
    private Connection conn = null;
    /**
     * Establish connection to database. Then create a table if it does not exist. 
     * 
     */
    public Database(){
        
        try {
            // db parameters
            String url = "jdbc:sqlite:build_history.db";

            // create a connection to the database
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } 

        createTable(conn);
    }
    /**
     * Gets connection, can be used in CIServer
     * @return
     */
    public Connection getConnection(){
        return conn;
    }

    /**
     * Method for creating the main table which will include the build info 
     * @param conn
     */
    public void createTable(Connection conn){
        String sql = "CREATE TABLE IF NOT EXISTS build_history (" +
                     "commit_id TEXT PRIMARY KEY ," +
                     "build_date TEXT NOT NULL," +
                     "build_logs TEXT NOT NULL)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method for inserting values into table. 
     * @param conn
     * @param id
     * @param commit_id
     * @param build_date
     * @param build_logs
     */
    public void insertBuild(Connection conn, String commit_id, String build_date,String build_logs){
        String sql = "INSERT INTO build_history (commit_id, build_date, build_logs) VALUES ( ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, commit_id);
            pstmt.setString(2, build_date );
            pstmt.setString(3, build_logs);
            pstmt.executeUpdate();
            System.out.println("Values inserted into the build_history table successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Fetches data from all entries in the database's table
     * @return builds: a list of arrays {commit_id, build_date, build_logs}
     */
    public List<String[]> getAllBuilds() {
    List<String[]> builds = new ArrayList<>();
    String sql = "SELECT commit_id, build_date, build_logs FROM build_history";
    try (Statement stmt = this.conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            String[] buildInfo = new String[3];
            buildInfo[0] = rs.getString("commit_id");
            buildInfo[1] = rs.getString("build_date");
            buildInfo[2] = rs.getString("build_logs");
            builds.add(buildInfo);
        }
    } catch (SQLException e) {
        System.err.println("Failed to fetch builds.");
        e.printStackTrace();
    }
    return builds;
}


}

