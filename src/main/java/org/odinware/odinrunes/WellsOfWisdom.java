package org.odinware.odinrunes;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The WellsOfWisdom interface represents a builder for creating a HTTP request object
 * used for making API requests.
 */
public interface WellsOfWisdom {

    /**
     * Builds a HTTP request based on the provided context and Odin messages.
     *
     * @param context The context containing captured data and user options.
     * @param odinMessages The messages exchanged between the user and the assistant.
     * @return A built HTTP request object, or null if no new prompt is present.
     */
    Request buildRequest(Context context, JSONArray odinMessages, JSONObject gptSettingsJsonObject) throws Exception;

    /**
     * Executes a HTTP request and returns the Response object.
     *
     * @param request The context containing captured data and user options.
     * @return A built HTTP response object, or null if no response.
     */
    Response executeRequest(Request request);

    /**
     * Reads from the Response object.
     *
     * @param response The response object.
     * @return A string generated by the gpt provider and read from the HTTP response object.
     */
    String readFromResponseStream(Response response);

    /**
     * A utility function designed to facilitate compliance with the expected return format in the readFromResponseStream method.
     *
     * @param model The GPT model name.
     * @param contentStr A partial response string from the GPT provider.
     * @return A string formatted appropriately for use as a return value in implementations of the readFromResponseStream method.
     */
    static String finalStringFormatHelper(String model, String contentStr) {

        // Escape special characters in contentStr
        contentStr = contentStr.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
        //String temp = "data: { \"model\":\"gemini-pro\", \"choices\": [{\"delta\":{\"content\":\""+escapeSpecialCharacters(contentStr)+"\"} }]}";

        // Build the final string in this format
        String result = "data: { \"model\":\"" + model + "\", \"choices\": [{\"delta\":{\"content\":\"" + contentStr + "\"} }]}";
        System.out.println(result);
        return result;
    }

}