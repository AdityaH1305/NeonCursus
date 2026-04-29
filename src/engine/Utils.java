package engine;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {

    // Reads a file and returns its contents as a single String
    public static String loadResource(String filePath) throws Exception {
        String result;
        try {
            result = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (Exception ex) {
            throw new Exception("Error reading file: " + filePath, ex);
        }
        return result;
    }
}