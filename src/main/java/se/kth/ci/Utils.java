package se.kth.ci;

// I/O
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {
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
}
