package org.odinware.odinrunes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import java.io.*;

public class TextHelperTest {
    private TextHelper textHelper;
    private final String testFilePath = "testFile.txt";

    @BeforeEach
    public void setup() throws IOException {
        // Check if the file exists, and if not, create it
        File file = new File(testFilePath);
        if (!file.exists()) {
            file.createNewFile();
        }

        // Create a new instance of TextHelper
        textHelper = new TextHelper(testFilePath);

        // Clear the contents of the test file before each test
        BufferedWriter writer = new BufferedWriter(new FileWriter(testFilePath));
        writer.write("");
        writer.close();
    }

    @AfterEach
    public void cleanup() {
        // Delete the test file after each test
        File file = new File(testFilePath);
        file.delete();
    }

    @Test
    public void testCheckAndPopulateFileIfEmpty() throws IOException {
        // Call the method to populate the file
        textHelper.checkAndPopulateFileIfEmpty();

        // Read the contents of the file and check if it matches the expected string
        BufferedReader reader = new BufferedReader(new FileReader(testFilePath));
        String line = reader.readLine();
        reader.close();

        // Use a regular expression to match the date format dd/MM/yy
        assertEquals(true, line.matches("<!-- =====\\[ Odin Runes \\]======\\[ \\{\"model\" : \"as-specified\", \"date\": \"\\d{2}/\\d{2}/\\d{2}\"\\} \\]======\\[ \\+ \\] -->"));
    }


    @Test
    public void testWriteStringToFile() throws IOException {
        // Write a string to the file
        String content = "This is a test";
        textHelper.writeStringToFile(content);

        // Read the contents of the file and check if it matches the expected string
        BufferedReader reader = new BufferedReader(new FileReader(testFilePath));
        String line = reader.readLine();
        reader.close();

        Assertions.assertEquals(content, line);
    }

    @Test
    public void testAppendStringToFile() throws IOException {
        // Append a string to the file
        String content = "This is a test";
        textHelper.appendStringToFile(content);

        // Read the contents of the file and check if it matches the expected string
        BufferedReader reader = new BufferedReader(new FileReader(testFilePath));
        String line = reader.readLine();
        reader.close();

        Assertions.assertEquals(content, line);
    }

    @Test
    public void testAppendOdinFirstInfo() throws IOException {
        // Append Odin first info to the file
        String model = "test-model";
        String date = "01/01/22";
        textHelper.appendOdinFirstInfo(model, date);

        // Read the contents of the file and check if it matches the expected string
        BufferedReader reader = new BufferedReader(new FileReader(testFilePath));
        String line = reader.readLine();
        reader.close();

        Assertions.assertEquals("<!-- =====[ Odin Runes ]======[ {\"model\" : \"" + model + "\", \"date\": \"" + date + "\"} ]======[ + ] -->", line);
    }

}