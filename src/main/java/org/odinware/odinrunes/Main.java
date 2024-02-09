package org.odinware.odinrunes;
import org.json.JSONObject;

import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Main class is the entry point of the application.
 * It handles the GUI and user interactions.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(GraphicalInteractionsHelper.class.getName());

    private static Context context = new Context();
    private static JSONObject gptSettingsJsonObject = new JSONObject("{\"temperature\":0.8,\"gptProvider\":\"OpenAI (gpt-3.5-turbo)\"}");
    private static JFrame frame;
    private static JPanel mainPanel;
    private static JPanel settingsPanel;
    private static boolean settingsVisible = false;
    private static File selectedFile;

    static {
        selectedFile = new File("OdinSays.txt");
        if (!selectedFile.exists()) {
            try {
                selectedFile.createNewFile(); // This line creates the file.
            } catch (IOException e) {
                // Handle the exception if file creation fails.
                logger.log(Level.SEVERE, "An error occurred: ", e);
            }
        }
    }

    /**
     * The main method of the application.
     * It is responsible for starting the application.
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }


    /**
     * Serializes a Context object to a file.
     *
     * @param context  The Context object to be serialized.
     * @param filePath The path of the file where the Context object will be serialized.
     */
    public static void serializeContext(Context context, String filePath) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);

            objectOut.writeObject(context);

            objectOut.close();
            fileOut.close();

            logger.info("Context object has been serialized and saved to " + filePath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }
    }

    /**
     * Deserializes a Context object from a file.
     *
     * @param filePath The path of the file from which the Context object will be deserialized.
     * @return The deserialized Context object.
     */
    public static Context deserializeContext(String filePath) {
        Context context = null;

        try {
            FileInputStream fileIn = new FileInputStream(filePath);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            context = (Context) objectIn.readObject();

            objectIn.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }

        return context;
    }

    /**
     * Creates and shows the main graphical user interface (GUI) of the application.
     */
    private static void createAndShowGUI() {
        frame = new JFrame("Odin Runes -- A loosely coupled environment to chat with your GPTs.");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the main panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Create the panel for the main components (excluding settings)
        JPanel mainComponentsPanel = new JPanel();
        mainComponentsPanel.setLayout(new FlowLayout());

        // Create the first dropdown menu
        String[] options = {"Clipboard", "Regionshot (OCR)", "Scrollshot (OCR)", "File (Live)","Photo (Coming Soon)"};
        final JComboBox<String> firstDropdown = new JComboBox<>(options);
        firstDropdown.setBackground(new Color(189, 219, 225)); // RGB values for a blue-grey shade

        // Create the second dropdown menu
        String[] arguments = {"Clipboard", "Context"};
        final JComboBox<String> secondDropdown = new JComboBox<>(arguments);

        // Create buttons
        JButton chatButton = new JButton("Chat");

        JButton submitButton = new JButton("Add context");
        JButton settingsButton = new JButton("Settings");

        // Create a listener for the submit button
        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final String selectedFunction = (String) firstDropdown.getSelectedItem();
                final String selectedArgument = (String) secondDropdown.getSelectedItem();

                if (selectedFunction != null && selectedArgument != null) {
                    new Thread(new Runnable() {
                        public void run() {

                            // Perform the action based on the selected function
                            if (selectedFunction.equals("Scrollshot (OCR)")) {
                                GraphicalInteractionsHelper giHelper = new GraphicalInteractionsHelper();
                                context.addCapturedData(giHelper.captureContextFromScrollshot(),"Scrollshot (OCR)");
                                logger.info(selectedArgument);
                                toggleSettingsPanelVisibility();
                                if(!settingsVisible) toggleSettingsPanelVisibility();
                            } else if (selectedFunction.equals("Regionshot (OCR)")) {
                                GraphicalInteractionsHelper giHelper = new GraphicalInteractionsHelper();
                                context.addCapturedData(giHelper.captureContextFromRegionshot(),"Regionshot (OCR)");
                                logger.info(selectedArgument);
                                toggleSettingsPanelVisibility();
                                if(!settingsVisible) toggleSettingsPanelVisibility();
                            } else if (selectedFunction.equals("Clipboard")) {
                                context.addCapturedData(GraphicalInteractionsHelper.captureContextFromClipboard(),"Clipboard");
                                logger.info(selectedArgument);
                                toggleSettingsPanelVisibility();
                                if(!settingsVisible) toggleSettingsPanelVisibility();
                            } else if (selectedFunction.equals("File (Live)")) {
                                JFileChooser fileChooser = new JFileChooser();
                                int returnValue = fileChooser.showOpenDialog(null);
                                if (returnValue == JFileChooser.APPROVE_OPTION) {
                                    File tempFile = fileChooser.getSelectedFile();
                                    if(TextHelper.readIntoString(tempFile.getAbsolutePath())==null) {
                                        // Do something with the selected file, e.g., display its path
                                        JOptionPane.showMessageDialog(frame, "ERROR: Something is wrong with the selected file: " + tempFile.getAbsolutePath());
                                    }else{
                                        JOptionPane.showMessageDialog(frame, "OK: I will pass the latest content of the selected file as part of the context to your specified GPT provider. \nThis means that any changes to the file will also be automatically reflected in the context. \nThe selected file: " + tempFile.getAbsolutePath());
                                        context.addCapturedData(tempFile.getAbsolutePath(), "File (Live)");
                                    }
                                }
                            }

                            // Add logic to handle the result or display a message.
                        }
                    }).start();
                }
            }
        });

        // Create a listener for the chat button
        chatButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TextHelper odinSays = new TextHelper(selectedFile.getAbsolutePath());
                GptOpsHelper.streamResponse(odinSays,context,gptSettingsJsonObject);
            }
        });

        // Create a listener for the settings button
        settingsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleSettingsPanelVisibility();
            }
        });

        // Add labels and components to the main components panel
        mainComponentsPanel.add(chatButton);

        // Add some separators
        int some=8;
        for (int i = 0; i < some; i++) mainComponentsPanel.add(new JSeparator(SwingConstants.VERTICAL));


        mainComponentsPanel.add(submitButton);
        mainComponentsPanel.add(new JLabel(" from "));
        mainComponentsPanel.add(firstDropdown);

        // Add some separators
        for (int i = 0; i < some; i++) mainComponentsPanel.add(new JSeparator(SwingConstants.VERTICAL));


        // Create a button to create a file
        JButton createFileButton = new JButton("New");
        createFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showSaveDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    try {
                        boolean fileCreated = selectedFile.createNewFile();
                        if (fileCreated) {
                            JOptionPane.showMessageDialog(frame, "File created: " + selectedFile.getAbsolutePath()+"\nOdinRunes will use this file every time you click the chat button from now on.");
                        } else {
                            JOptionPane.showMessageDialog(frame, "File already exists.");
                        }
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Error message: ", e);
                    }
                }
            }
        });
        mainComponentsPanel.add(createFileButton);

        // Create a button to open a file
        JButton openFileButton = new JButton("Open");
        openFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File tempFile = fileChooser.getSelectedFile();
                    if(!TextHelper.isFirstLineValid(tempFile)) {
                        // Do something with the selected file, e.g., display its path
                        JOptionPane.showMessageDialog(frame, "Something is wrong with the selected output file: " + selectedFile.getAbsolutePath());
                    }else{
                        selectedFile=tempFile;
                        JOptionPane.showMessageDialog(frame, "Output file set successfully to: " + selectedFile.getAbsolutePath()+"\nOdinRunes will use this file every time you click the chat button from now on.");
                    }
                }
            }
        });
        mainComponentsPanel.add(openFileButton);



        mainComponentsPanel.add(settingsButton);


        // Add the main components panel to the main panel
        mainPanel.add(mainComponentsPanel);

        // Initially, hide the settings panel
        settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setVisible(false);
        // Add the settingsPanel to the mainPanel
        mainPanel.add(settingsPanel);

        frame.add(mainPanel);
        frame.pack(); // Initially set the frame size
        frame.setVisible(true);
    }






    /**
     * Toggles the visibility of the settings panel.
     */
    private static void toggleSettingsPanelVisibility() {
        if (!settingsVisible) {
            // Flush the settingsPanel by removing all existing components
            settingsPanel.removeAll();

            JPanel rowPanelG0 = new JPanel(); // Create a separate panel for each row
            rowPanelG0.setLayout(new FlowLayout(FlowLayout.LEFT));
            JEditorPane gptEditorPane = new JEditorPane();
            gptEditorPane.setContentType("text/html");
            gptEditorPane.setText("<html>Bellow you can see and change various settings. Click the 'Settings' button again to hide this panel. <h2>GPT:</h2> You can choose your desired GPT provider and configure it's parameters here:</html>");
            gptEditorPane.setEditable(false);
            gptEditorPane.setBackground(null);
            rowPanelG0.add(gptEditorPane);
            settingsPanel.add(rowPanelG0);

            JPanel rowPanelG1 = new JPanel(); // Create a separate panel for each row
            rowPanelG1.setLayout(new FlowLayout(FlowLayout.LEFT));


            // Set initial values based on gptSettingJsonObject
            String selectedGptProvider = gptSettingsJsonObject.getString("gptProvider");
            double initialTemperature = gptSettingsJsonObject.getDouble("temperature");


            // Create an array of options for the dropdown
            String[] options = {"OpenAI (gpt-3.5-turbo)", "Google's VertexAI (chat-bison)", "Google's VertexAI (gemini-pro)", "Ollama"};

            // Create a JComboBox with the options array
            final JComboBox<String> dropdown = new JComboBox<>(options);
            // Set the background color of the JComboBox to a blue-grey shade
            dropdown.setBackground(new Color(189, 219, 225)); // RGB values for a blue-grey shade
            // Set the initial selected item in the dropdown
            dropdown.setSelectedItem(selectedGptProvider);
            // Add the dropdown to the panel
            rowPanelG1.add(dropdown);

            JLabel sliderLabel = new JLabel("Temperature:");
            final JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (initialTemperature * 100));

            rowPanelG1.add(sliderLabel);
            rowPanelG1.add(slider);
            // Create a JButton
            JButton applyGPTSettingsButton = new JButton("Apply");

            // Add an ActionListener to the button
            applyGPTSettingsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Print the selected value from the JComboBox
                    String selectedOption = (String) dropdown.getSelectedItem();
                    logger.info("Selected gpt provider Value: " + selectedOption);
                    // Check if the selected option is "Ollama"
                    if (selectedOption.equals("Ollama")) {
                        // Create a dialog box
                        JDialog dialog = new JDialog();
                        dialog.setTitle("Ollama Settings");
                        dialog.setLayout(new BorderLayout());

                        // Create a panel for the description label
                        JPanel descriptionPanel = new JPanel();
                        descriptionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                        // Create the description label
                        JLabel descriptionLabel = new JLabel("<html>Ollama is an open-source project that helps you run opensource LLMs locally. <br>It supports a wide range of models such as llama2, mistral, etc. "
                                + "<br><br>"
                                + "The chat completion backend is the API backend from where Ollama is serving the specified LLM's chat completion API.<br>"
                                + "Example: http://localhost:11434/api/chat <br><br>"
                                + "</html>");
                        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);
                        descriptionPanel.add(descriptionLabel);

                        // Create a panel for the input fields
                        JPanel inputPanel = new JPanel();
                        inputPanel.setLayout(new GridBagLayout());
                        GridBagConstraints gbc = new GridBagConstraints();
                        gbc.gridx = 0;
                        gbc.gridy = GridBagConstraints.RELATIVE;
                        gbc.anchor = GridBagConstraints.WEST;
                        gbc.fill = GridBagConstraints.HORIZONTAL; // Set fill to horizontal to make the fields span on multiple columns
                        gbc.insets = new Insets(5, 5, 5, 5);

                        // Create the first input field
                        JLabel label1 = new JLabel("Model: ");
                        JTextField textField1 = new JTextField(45);
                        String attributeValue1 = gptSettingsJsonObject.optString("model", "");
                        textField1.setText(attributeValue1);
                        inputPanel.add(label1, gbc);
                        gbc.gridx = 1;
                        inputPanel.add(textField1, gbc);

                        // Create the second input field
                        JLabel label2 = new JLabel("Backend URI (for chat completion API): ");
                        JTextField textField2 = new JTextField(45);
                        String attributeValue2 = gptSettingsJsonObject.optString("backendURI", "");
                        textField2.setText(attributeValue2);
                        gbc.gridx = 0;
                        inputPanel.add(label2, gbc);
                        gbc.gridx = 1;
                        inputPanel.add(textField2, gbc);

                        // Create a panel for the submit button
                        JPanel buttonPanel = new JPanel();
                        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
                        // Create the submit button
                        JButton submitButton = new JButton("OK");
                        buttonPanel.add(submitButton);

                        // Add an action listener for the submit button
                        submitButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                // Get the values from the input fields
                                String model = textField1.getText();
                                String backendURI = textField2.getText();

                                // Print the selected value from the JSlider
                                int sliderValue = slider.getValue();
                                logger.info("Selected Temperature Value: " + sliderValue / 100.0);

                                // Create a JSON object with the selected values
                                gptSettingsJsonObject = new JSONObject();
                                gptSettingsJsonObject.put("gptProvider", selectedOption);
                                gptSettingsJsonObject.put("temperature", sliderValue / 100.0);
                                gptSettingsJsonObject.put("model", model);
                                gptSettingsJsonObject.put("backendURI", backendURI);

                                // Log the JSON representation
                                logger.info("Selected Values (JSON): " + gptSettingsJsonObject.toString());

                                // Close the dialog box
                                dialog.dispose();
                            }
                        });

                        // Add the panels to the dialog box
                        dialog.add(descriptionPanel, BorderLayout.NORTH);
                        dialog.add(inputPanel, BorderLayout.CENTER);
                        dialog.add(buttonPanel, BorderLayout.SOUTH);

                        // Display the dialog box
                        dialog.pack();
                        dialog.setVisible(true);
                    } else {
                        // Print the selected value from the JSlider
                        int sliderValue = slider.getValue();
                        logger.info("Selected Temperature Value: " + sliderValue / 100.0);

                        // Create a JSON object with the selected values
                        gptSettingsJsonObject = new JSONObject();
                        gptSettingsJsonObject.put("gptProvider", selectedOption);
                        gptSettingsJsonObject.put("temperature", sliderValue / 100.0);

                        // Log the JSON representation
                        logger.info("Selected Values (JSON): " + gptSettingsJsonObject.toString());
                    }
                }
            });

            // Add the button to the panel
            rowPanelG1.add(applyGPTSettingsButton);
            settingsPanel.add(rowPanelG1);



            JPanel rowPanelC0 = new JPanel(); // Create a separate panel for each row
            rowPanelC0.setLayout(new FlowLayout(FlowLayout.LEFT));
            JEditorPane contextEditorPane = new JEditorPane();
            contextEditorPane.setContentType("text/html");
            contextEditorPane.setText("<html><h2>Context:</h2> When you add some context using the 'Add Context' button, it will appear here:</html>");
            contextEditorPane.setEditable(false);
            contextEditorPane.setBackground(null);
            rowPanelC0.add(contextEditorPane);
            settingsPanel.add(rowPanelC0);



            // Create the rest of settings panel and populate it with components
            List<Context.CapturedData> capturedDataList = context.getCapturedDataList();
            for (final Context.CapturedData capturedData : capturedDataList) {
                JPanel rowPanel = new JPanel(); // Create a separate panel for each row
                rowPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                rowPanel.add(new JLabel(capturedData.getCaptureMethod()));
                rowPanel.add(new JLabel(" was used to load "));
                String capturedText = capturedData.getCapturedText();
                logger.info("captured text is: " + capturedText);
                int tempLength=0;
                if (capturedText != null) {
                    tempLength=capturedText.length();
                }

                // Truncate the text to the first 15 characters followed by "..."
                if (capturedText.length() > 25) {
                    capturedText = capturedText.substring(0, 25) + "... ";
                }
                rowPanel.add(new JLabel("<html><b><font color='blue'>'" + capturedText + "'</font></b> | Size: " + tempLength + " characters.</html>"));

                // Add a button to remove the captured data
                JButton removeButton = new JButton("X");
                removeButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // When the button is pressed, remove the corresponding captured data
                        context.deleteCapturedData(capturedData);
                        // Refresh the settings panel
                        toggleSettingsPanelVisibility();
                        toggleSettingsPanelVisibility();
                    }
                });
                rowPanel.add(removeButton);

                settingsPanel.add(rowPanel); // Add the row panel to the main settingsPanel
            }


            JPanel rowPanelC1 = new JPanel(); // Create a separate panel for each row
            rowPanelC1.setLayout(new FlowLayout(FlowLayout.LEFT));
            JEditorPane contextButtonsPane = new JEditorPane();
            contextButtonsPane.setContentType("text/html");
            contextButtonsPane.setText("<html><b>Save/Load:</b> You can save the current context or load a previously saved context using the buttons bellow:</html>");
            contextButtonsPane.setEditable(false);
            contextButtonsPane.setBackground(null);
            rowPanelC1.add(contextButtonsPane);
            settingsPanel.add(rowPanelC1);

            JPanel rowPanelC2 = new JPanel(); // Create a separate panel for each row
            rowPanelC2.setLayout(new FlowLayout(FlowLayout.LEFT));

            // Create a button to create a file
            JButton createFileButton = new JButton("Save Context");
            createFileButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    int returnValue = fileChooser.showSaveDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedContextFile = fileChooser.getSelectedFile();
                        try {
                            boolean fileCreated = selectedContextFile.createNewFile();
                            if (fileCreated) {
                                serializeContext(context,selectedContextFile.getAbsolutePath());
                                JOptionPane.showMessageDialog(frame, "Successfully serialized the context to: " + selectedContextFile.getAbsolutePath());
                            } else {
                                JOptionPane.showMessageDialog(frame, "Context file with similar name already exists!");
                            }
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, "Error message: ", e);
                        }
                    }
                }
            });

            rowPanelC2.add(createFileButton);

            // Create a button to open a file
            JButton openFileButton = new JButton("Load Context");
            openFileButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    int returnValue = fileChooser.showOpenDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File tempFile = fileChooser.getSelectedFile();
                        Context tempContext=deserializeContext(tempFile.getAbsolutePath());
                        if(tempContext == null) {
                            // Do something with the selected file, e.g., display its path
                            JOptionPane.showMessageDialog(frame, "Something is wrong with the selected context file: " + selectedFile.getAbsolutePath());
                        }else{
                            context=tempContext;
                            toggleSettingsPanelVisibility();
                            toggleSettingsPanelVisibility();
                        }
                    }
                }
            });
            rowPanelC2.add(openFileButton);
            settingsPanel.add(rowPanelC2);
        }

        settingsVisible = !settingsVisible;
        settingsPanel.setVisible(settingsVisible);

        // Refit the frame size
        frame.pack();
    }

}
