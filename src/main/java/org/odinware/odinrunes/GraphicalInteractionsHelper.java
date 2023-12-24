package org.odinware.odinrunes;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import org.sikuli.script.*;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.text.similarity.LevenshteinDistance;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;


/**
 * The GraphicalInteractionsHelper class provides methods for capturing and interacting with graphical elements on the screen.
 * It utilizes libraries such as SikuliX and Tesseract for performing tasks like capturing screenshots and extracting text from images.
 *
 * Usage:
 * - Create an instance of GraphicalInteractionsHelper to access its methods.
 * - Use the provided methods to capture context from various sources, such as the clipboard, regionshots, or scrollshots.
 * - Use the captured context for further processing or interaction with other components.
 *
 * Note:
 * - Make sure to have the necessary dependencies installed and added to the project.
 * - The class assumes the availability of tools like SikuliX and Tesseract for capturing and processing graphical elements.
 *
 */
public class GraphicalInteractionsHelper  {
    private static final Logger logger = Logger.getLogger(GraphicalInteractionsHelper.class.getName());
    private Region selectedRegion;
    private Screen screen;
    private int scrollAmount;
    private int numScreenshots;
    private String previousExtractedText;
    private String finalText;

    /**
     * Creates a new instance of the GraphicalInteractionsHelper class.
     * Initializes the region, screen, and other variables used for graphical interactions.
     */
    public GraphicalInteractionsHelper() {
        // Initialize the region, screen, and other variables here
        screen = new Screen();
        logger.info("Please select a region by drawing a rectangle with your mouse.");
        selectedRegion = screen.selectRegion();
        scrollAmount = 2;
        numScreenshots = 3000;
        previousExtractedText = "";
        finalText = "";
    }

    /**
     * Retrieves the text content from the clipboard.
     * @return The text content from the clipboard, or an empty string if it is not available or cannot be retrieved.
     */
    public static String captureContextFromClipboard(){
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);

        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String clipboardContent = (String) contents.getTransferData(DataFlavor.stringFlavor);
                return clipboardContent;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
            }
        }
        return "";
    }

    /**
     * Captures the text content from a scrollable region on the screen.
     * @return The captured text content, or an empty string if it is not available or cannot be captured.
     */
    public String captureContextFromScrollshot() {
        int lowerRightX = selectedRegion.getX() + selectedRegion.getW();
        int lowerRightY = selectedRegion.getY() + selectedRegion.getH();

        for (int i = 0; i < numScreenshots; i++) {
            Mouse.move(new Location(lowerRightX, lowerRightY));
            try {
                selectedRegion.highlight(0.1);
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
            }

            ScreenImage screenImage = screen.capture(selectedRegion);

            String screenshotName = "./screenshot_" + i + ".png";
            screenImage.save("./", screenshotName);
            logger.info("Captured screenshot: " + screenshotName);

            Tesseract tesseract = new Tesseract();
            // Get the language from environment variable if set
            String language = System.getenv("ODIN_RUNES_OCR_LANGUAGE");
            if (language == null || language.isEmpty()) {
                language="eng";
            }
            tesseract.setLanguage(language);

            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

            File imageFile = new File(String.format("./%s", screenshotName));
            try {
                String currentExtractedText = tesseract.doOCR(imageFile);
                int distance = levenshteinDistance.apply(currentExtractedText, previousExtractedText);
                List<String> middle = getMiddle(currentExtractedText, 0.2F, 0.8F);
                double similarity = getSimilarity(middle, previousExtractedText);

                if (similarity > 0.7) scrollAmount++;
                if (similarity < 0.2 && scrollAmount > 1) scrollAmount--;

                logger.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@: " + similarity + " " + scrollAmount);

                if (distance > 5) {
                    logger.info("Got more text");
                    String mergedString = finalText + currentExtractedText;
                    previousExtractedText = currentExtractedText;
                    finalText = mergedString;
                }

                if (distance < 5 && i > 3) {
                    break;
                }

            } catch (TesseractException e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
                // Display an error message dialog
                String errorMessage = "An error occurred:\n" + e.getMessage();
                JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);

            }
            catch (Error e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
                // Display an error message dialog
                String errorMessage = "Error opening data file "+System.getenv("TESSDATA_PREFIX")+"/"+language+".traineddata\n" +
                        "Please make sure the TESSDATA_PREFIX environment variable is set to your \"tessdata\" directory.\n" +
                        "Failed loading language"+" '"+language+"'\n" +
                        "Tesseract couldn't load any languages!";
                JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                throw e;
            }

            try {
                screen.wheel(selectedRegion, Button.WHEEL_DOWN, scrollAmount);
            } catch (FindFailed e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
            }


        }

        logger.info("Screenshots captured and scrolling completed!");
        logger.info("#########################################Merged String: " + finalText);
        return finalText;
    }

    /**
     * Captures the text content from a series of screenshots taken at regular intervals on the screen.
     * @return The captured text content, or an empty string if it is not available or cannot be captured.
     */
    public String captureContextFromTimeshot() {
        int lowerRightX = selectedRegion.getX() + selectedRegion.getW();
        int lowerRightY = selectedRegion.getY() + selectedRegion.getH();
        Mouse.move(new Location(lowerRightX, lowerRightY));

        for (int i = 0; i < numScreenshots; i++) {
            ScreenImage screenImage = screen.capture(selectedRegion);

            String screenshotName = "./screenshot_" + i + ".png";
            screenImage.save("./", screenshotName);
            logger.info("Captured screenshot: " + screenshotName);

            Tesseract tesseract = new Tesseract();
            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

            File imageFile = new File(String.format("./%s", screenshotName));
            try {
                String currentExtractedText = tesseract.doOCR(imageFile);
                int distance = levenshteinDistance.apply(currentExtractedText, previousExtractedText);
                List<String> middle = getMiddle(currentExtractedText, 0.2F, 0.8F);
                double similarity = getSimilarity(middle, previousExtractedText);


                logger.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@: " + similarity);

                if (distance > 5 && similarity < 0.9) {
                    logger.info("Got more text");
                    String mergedString = finalText + "\n" +
                            " At timestamp: " + i + " the OCR of the screen is: " + currentExtractedText;
                    previousExtractedText = currentExtractedText;
                    finalText = mergedString;
                }


            } catch (TesseractException e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
            }


            try {
                selectedRegion.highlight(0.1);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
            }
        }

        logger.info("Screenshots captured.");
        logger.info("#########################################Merged String: " + finalText);

        return finalText;
    }

    /**
     * Captures the text content from a specific region on the screen.
     * @return The captured text content, or an empty string if it is not available or cannot be captured.
     */
    public String captureContextFromRegionshot() {
        int lowerRightX = selectedRegion.getX() + selectedRegion.getW();
        int lowerRightY = selectedRegion.getY() + selectedRegion.getH();

        Mouse.move(new Location(lowerRightX, lowerRightY));
        ScreenImage screenImage = screen.capture(selectedRegion);

        String screenshotName = "./screenshot_" + 0 + ".png";
        screenImage.save("./", screenshotName);
        logger.info("Captured screenshot: " + screenshotName);

        Tesseract tesseract = new Tesseract();
        // Get the language from environment variable if set
        String language = System.getenv("ODIN_RUNES_OCR_LANGUAGE");
        if (language == null || language.isEmpty()) {
            language="eng";
        }
        tesseract.setLanguage(language);

        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

        File imageFile = new File(String.format("./%s", screenshotName));
        try {
                finalText = tesseract.doOCR(imageFile);

        } catch (TesseractException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
            // Display an error message dialog
            String errorMessage = "An error occurred:\n" + e.getMessage();
            JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);

        }
        catch (Error e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
            // Display an error message dialog
            String errorMessage = "Error opening data file "+System.getenv("TESSDATA_PREFIX")+"/"+language+".traineddata\n" +
                    "Please make sure the TESSDATA_PREFIX environment variable is set to your \"tessdata\" directory.\n" +
                    "Failed loading language"+" '"+language+"'\n" +
                    "Tesseract couldn't load any languages!";
            JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        }


        try {
            selectedRegion.highlight(0.1);
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }


        logger.info("Screenshots captured");
        logger.info("#########################################Merged String: " + finalText);

        return finalText;
    }

    /**
     * Extracts a middle portion of a given text.
     * @param input The text to extract the middle portion from.
     * @param startRatio The starting ratio of the middle portion, expressed as a value between 0.0 and 1.0.
     * @param endRatio The ending ratio of the middle portion, expressed as a value between 0.0 and 1.0.
     * @return The middle portion of the text as a substring.
     */
    public List<String> getMiddle(String input, float startRatio, float endRatio) {
        String[] wordArray = input.split(" ");
        int startIndex = (int) (wordArray.length * startRatio);
        int endIndex = (int) (wordArray.length * endRatio);
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(wordArray.length, endIndex);
        List<String> middleWords = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(wordArray, startIndex, endIndex)));
        return middleWords;
    }

    /**
     * Calculates the similarity between a list of strings and a larger text.
     * @param stringsToFind The list of strings to search for in the larger text.
     * @param largerText The larger text to search in for the given strings.
     * @return The similarity score between 0.0 and 1.0 (inclusive).
     */
    public double getSimilarity(List<String> stringsToFind, String largerText) {
        int totalWords = stringsToFind.size();
        int foundWords = 0;

        for (String str : stringsToFind) {
            if (largerText.contains(str)) {
                foundWords++;
            }
        }

        if (totalWords == 0) {
            return 0.0;
        } else {
            return (double) foundWords / totalWords;
        }
    }

}
