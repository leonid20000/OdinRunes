package org.odinware.odinrunes;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * The Context class represents the context in which the GPT request is made.
 * It contains captured data and user options that will be used to construct the request.
 */
public class Context implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<CapturedData> capturedDataList;
    private String userProfile;

    public Context() {
        this.capturedDataList = new ArrayList<>();
    }

    /**
     * Adds captured data to the context using the specified captured text and capture method.
     *
     * @param capturedText The text captured from the user's input.
     * @param captureMethod The method used to capture the text (e.g., clipboard, OCR).
     */
    public void addCapturedData(String capturedText, String captureMethod) {
        CapturedData capturedData = new CapturedData(capturedText, captureMethod);
        capturedDataList.add(capturedData);
    }

    /**
     * Deletes the specified captured data from the context.
     *
     * @param capturedDataToDelete The captured data to be deleted.
     */
    public void deleteCapturedData(CapturedData capturedDataToDelete) {
        capturedDataList.remove(capturedDataToDelete);
    }

    /**
     * Returns the list of captured data in the context.
     *
     * @return The list of captured data.
     */
    public List<CapturedData> getCapturedDataList() {
        return capturedDataList;
    }

    /**
     * Returns the user profile of the context.
     *
     * @return The user profile of the context.
     */
    public String getUserProfile() {
        return userProfile;
    }

    /**
     * Sets the user profile for the context.
     *
     * @param userProfile The user profile to be set.
     */
    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }

    /**
     * The CapturedData class represents a piece of data captured from the user's input.
     * It contains the captured text and the capture method used to obtain the data.
     */
    public static class CapturedData implements Serializable{
        private static final long serialVersionUID = 1L;

        private String capturedText;
        private String captureMethod;

        public CapturedData(String capturedText, String captureMethod) {
            this.capturedText = capturedText;
            this.captureMethod = captureMethod;
        }

        public String getCapturedText() {
            if(getCaptureMethod().equals("File (Live)")){
               String fileContent=TextHelper.readIntoString(capturedText);
               if(fileContent == null){
                   return "ERROR READING FROM FILE";
               } else return fileContent;
            } else return capturedText;

        }

        public void setCapturedText(String capturedText) {
            this.capturedText = capturedText;
        }

        public String getCaptureMethod() {
            return captureMethod;
        }

        public void setCaptureMethod(String captureMethod) {
            this.captureMethod = captureMethod;
        }
    }
}
