package org.odinware;
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
        frame = new JFrame("Graphical Interactions");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the main panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Create the panel for the main components (excluding settings)
        JPanel mainComponentsPanel = new JPanel();
        mainComponentsPanel.setLayout(new FlowLayout());

        // Create the first dropdown menu
        String[] options = {"Clipboard", "Regionshot (OCR)", "Scrollshot (OCR)", "Photo (Coming Soon)"};
        final JComboBox<String> firstDropdown = new JComboBox<>(options);

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
                GptOpsHelper.streamResponse(odinSays,context);
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
                            JOptionPane.showMessageDialog(frame, "File created: " + selectedFile.getAbsolutePath());
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

            JPanel rowPanelC0 = new JPanel(); // Create a separate panel for each row
            rowPanelC0.setLayout(new FlowLayout(FlowLayout.LEFT));
            JEditorPane contextEditorPane = new JEditorPane();
            contextEditorPane.setContentType("text/html");
            contextEditorPane.setText("<html>Bellow you can see and change various settings. Click the 'Settings' button again to hide this panel. <h2>Context:</h2> When you add some context using the 'Add Context' button, it will appear here:</html>");
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

                // Truncate the text to the first 15 characters followed by "..."
                if (capturedText.length() > 25) {
                    capturedText = capturedText.substring(0, 25) + "... ";
                }
                rowPanel.add(new JLabel("'"+capturedText+"'"));

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
