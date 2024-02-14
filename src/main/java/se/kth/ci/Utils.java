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

/**
 * Class featuring static utility methods for file I/0 operations.
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 */
public class Utils {

    /**
     * Private constructor to prevent instantiation.
     */
    private Utils() {}

    /**
     * Writes the gradle build log to the specified file.
     * @param inputStream InputStream: the input stream to read from
     * @param filePath String: the path to the file to write to
     */
    public static void writeToFile(InputStream inputStream, String filePath){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath)); 
            String line;
             while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            writer.close();
        }
        catch(Exception e){
            System.err.println("Error when writing to file. " + e.getMessage());
        }
    }
    /**
     * Reads the specified file and return it as a String.
     * @param filePath String: the path to the file to read
     * @return String: the content of the file
     */
    public static String readFromFile(String filePath) {
        String content = "";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            content = new String(bytes);
        } catch (IOException e) {
            System.err.println("Error while reading from file. " + e.getMessage());
        }
        return content;
    }
}
