package org.odinware.odinrunes;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * The TextHelper class provides utility methods for working with text files.
 */
public class TextHelper {
    private static final Logger logger = Logger.getLogger(TextHelper.class.getName());

    private String filePath;

    /**
     * Constructs a TextHelper object with the specified file path.
     *
     * @param filePath the path to the text file
     */
    public TextHelper(String filePath) {
        this.filePath = filePath;
        checkAndPopulateFileIfEmpty();
        openTextFile();
    }

    /**
     * Checks if the file is empty, and populates it with default content if necessary.
     */
    public void checkAndPopulateFileIfEmpty() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            if (reader.readLine() == null) {
                // The file is empty, so populate it with default content.
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
                String strDate = formatter.format(date);
                appendOdinFirstInfo("as-specified", strDate);
                appendStringToFile("Hello, append your prompt to the end of this file. I will pass it on together with the relevant context to your GPT. \n");
                appendOverInfo();
                appendUserInfo();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }
    }

    /**
     * Writes the specified string content to the file, overwriting its previous content.
     *
     * @param content the string content to write
     */
    public void writeStringToFile(String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }
    }

    /**
     * Appends the specified string content to the file.
     *
     * @param content the string content to append
     */
    public void appendStringToFile(String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(content);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }
    }

    /**
     * Appends the Odin first info to the file.
     *
     * @param model the Odin model
     * @param date  the date
     */
    public void appendOdinFirstInfo(String model, String date) {
        String odinFirstInfo = String.format(
                "<!-- =====[ Odin Runes ]======[ {\"model\" : \"%s\", \"date\": \"%s\"} ]======[ + ] -->\n",
                model, date
        );
        appendStringToFile(odinFirstInfo);
    }

    /**
     * Appends the user info to the file.
     */
    public void appendUserInfo() {
        String userInfo = "<!-- =====[ User ]=====[ : ] -->\n";
        appendStringToFile(userInfo);
    }

    /**
     * Appends the over info with an error to the file.
     */
    public void appendOverInfoWithError() {
        String overInfo = "<!-- =====[ OVER ]=====[ ! ] -->\n";
        appendStringToFile(overInfo);
    }

    /**
     * Appends the assistant info to the file.
     */
    public void appendAssistantInfo() {
        String assistantInfo = "<!-- =====[ Assistant ]=====[ : ] -->\n";
        appendStringToFile(assistantInfo);
    }

    /**
     * Appends the over info to the file.
     */
    public void appendOverInfo() {
        String overInfo = "<!-- =====[ OVER ]=====[ # ] -->\n";
        appendStringToFile(overInfo);
    }

    /**
     * Opens the text file using the default system program.
     */
    public void openTextFile() {
        try {
            File fileToOpen = new File(filePath);

            if (Desktop.isDesktopSupported() && fileToOpen.exists()) {
                Desktop.getDesktop().open(fileToOpen);
            } else {
                System.out.println("Desktop not supported, or the file does not exist.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }
    }

    /**
     * Retrieves the messages from the text file.
     *
     * @return a JSONArray containing the messages
     */
    public JSONArray getMessages() {
        JSONArray messages = new JSONArray();
        boolean isUserSection = false;
        boolean isAssistantSection = false;
        StringBuilder messageBuilder = new StringBuilder();
        String jsonTemp = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("<!-- =====[ User ]=====[ : ] -->")) {
                    isUserSection = true;
                    isAssistantSection = false;
                    messageBuilder.setLength(0); // Clear the message builder.
                } else if (line.contains("<!-- =====[ Odin Runes ]======[") && line.contains("]======[ + ] -->")) {
                    isUserSection = false;
                    isAssistantSection = false;
                    messageBuilder.setLength(0); // Clear the message builder.
                    // Extract JSON content from the second pair of square brackets.
                    int startIndex = line.indexOf('[') + 1;
                    int endIndex = line.lastIndexOf(']');
                    if (startIndex >= 0 && endIndex > startIndex) {
                        String jsonContent = line.substring(startIndex, endIndex);
                        try {
                            // Check if the JSON content is valid.
                            new JSONObject(new JSONTokener(jsonContent));
                            jsonTemp = jsonContent;
                        } catch (Exception e) {
                            jsonTemp = null; // JSON is invalid.
                        }
                    }
                } else if (line.contains("<!-- =====[ Assistant ]=====[ : ] -->")) {
                    isUserSection = false;
                    isAssistantSection = true;
                    messageBuilder.setLength(0); // Clear the message builder.
                } else if (line.contains("<!-- =====[ OVER ]=====[ # ] -->")) {
                    if (isUserSection || isAssistantSection) {
                        String content = messageBuilder.toString().trim();
                        if (!content.isEmpty()) {
                            String role = isUserSection ? "user" : "assistant";
                            JSONObject message = new JSONObject()
                                    .put("role", role)
                                    .put("content", content);
                            if (jsonTemp != null) {
                                message.put("jsonInfo", jsonTemp); // Add the JSON content to the message.
                            }
                            messages.put(message);
                        }
                    }
                    isUserSection = false;
                    isAssistantSection = false;
                    jsonTemp = null; // Reset the JSON temp variable.
                } else if (line.contains("<!-- =====[ OVER ]=====[ ! ] -->")) {
                    if (isUserSection || isAssistantSection) {
                        messageBuilder.setLength(0); // Clear the message builder.
                    }
                    isUserSection = false;
                    isAssistantSection = false;
                    jsonTemp = null; // Reset the JSON temp variable.
                } else if (isUserSection || isAssistantSection) {
                    messageBuilder.append(line).append("\n");
                }
            }
            // Last prompt if any
            if (isUserSection) {
                String content = messageBuilder.toString().trim();
                if (!content.isEmpty()) {
                    JSONObject message = new JSONObject()
                            .put("role", "prompt")
                            .put("content", content);
                    messages.put(message);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }

        return messages;
    }

    /**
     * Checks if the first line of the file contains valid Odin information.
     *
     * @param file the file to check
     * @return true if the first line is valid, false otherwise
     */
    public static boolean isFirstLineValid(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            return firstLine != null && firstLine.contains("<!-- =====[ Odin");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
            return false; // Handle the exception as needed
        }
    }

    public static String readIntoString(String filePath) {
        try {
                File file = new File(filePath);
                StringBuilder stringBuilder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
            }

            reader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred in readIntoString: ", e);
            return null;
        }
    }
}