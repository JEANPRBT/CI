package se.kth.ci;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



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
        //insertBuild(conn, 1, 345, "01-01-01", "test");
        //listTables(conn);
        //deleteTable(conn, "build_history");
    }

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
    // The following two functions can be deleted, I don't think we have a need for them
    public void listTables(Connection conn){
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, null, new String[]{"TABLE"});
            
            System.out.println("Tables in the database:");
            while (tables.next()) {
                System.out.println(tables.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void deleteTable(Connection conn,String tableName){
        try (Statement stmt = conn.createStatement()){
            String sql = "DROP TABLE IF EXISTS " + tableName;
            stmt.execute(sql);
            System.out.println("Table '" + tableName + "' deleted successfully.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

}