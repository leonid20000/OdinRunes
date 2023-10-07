package org.odinware;

import okhttp3.*;
import okio.BufferedSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * The GptOpsHelper class provides utility methods for building requests to chat-based language processing models, regardless of the specific GPT provider.
 * It interacts with GPT models to perform chat-based language processing tasks and handles response streaming and JSON processing.
 *
 * This class can be used with different GPT providers by implementing the RequestBuilder interface to build custom requests.
 * It also includes methods for counting words in a JSONArray and extracting valid JSON objects from a response stream.
 *
 * Basic Usage:
 * 1. Use the buildCustomRequest method to build a custom Request object based on a specific RequestBuilder implementation.
 * 2. Use the streamResponse method to stream the response from the GPT provider and process the partial responses in real-time.
 *
 * @version 1.0
 */
public class GptOpsHelper {
    private static final Logger logger = Logger.getLogger(GptOpsHelper.class.getName());
    public static Request buildOpenAIRequest(Context context, TextHelper odinSays) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY_ODIN_FIRST");
        String apiUrl = "https://api.openai.com/v1/chat/completions";
        boolean hasNewPrompt = false;
        String model = "gpt-3.5-turbo";


        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonBody = new JSONObject();

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant."));

        // Add context
        List<Context.CapturedData> capturedDataList = context.getCapturedDataList();
        for (Context.CapturedData capturedData : capturedDataList) {
            String captureMethod = capturedData.getCaptureMethod();
            String capturedText = capturedData.getCapturedText();
            if (captureMethod.equals("Clipboard")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The text content from a portion of the user's clipboard is as follows: " + capturedText));
            } else if (captureMethod.equals("Regionshot")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is as follows: " + capturedText));
            } else if (captureMethod.equals("Scrollshot")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is included below. It might have some redundant lines. \n" + capturedText));
            }
        }

        // Add chat history
        JSONArray odinMessages = odinSays.getMessages();
        for (int i = 0; i < odinMessages.length(); i++) {
            JSONObject message = odinMessages.getJSONObject(i);
            String role = message.getString("role");
            String content = message.getString("content");

            if ("user".equals(role)) {
                // Process user messages
                System.out.println("User: " + content);
                messages.put(new JSONObject().put("role", "user").put("content", content));
            } else if ("assistant".equals(role)) {
                // Process assistant messages
                System.out.println("Assistant: " + content);
                messages.put(new JSONObject().put("role", "assistant").put("content", content));
            } else if ("prompt".equals(role)) {
                // Process the prompt message
                System.out.println("Prompt: " + content);
                messages.put(new JSONObject().put("role", "user").put("content", content));
                hasNewPrompt = true;
            } else {
                // Handle other roles if needed
                System.out.println("Unknown role: " + role);
            }

            // If 'jsonInfo' field is present in the message, you can extract it like this:
            if (message.has("jsonInfo")) {
                String jsonInfo = message.getString("jsonInfo");
                // Process jsonInfo if needed
            }
        }

        if (!hasNewPrompt) {
            return null; // No new prompt to send
        }

        if(countWordsInJSONArray(messages) > 2000){
            model = "gpt-3.5-turbo-16k";
        }

        jsonBody.put("model", model);

        jsonBody.put("messages", messages);

        jsonBody.put("stream", true);

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + openaiApiKey)
                .post(requestBody)
                .build();
        return request;
    }

    /**
     * Builds and returns a custom Request object based on the provided RequestBuilder implementation.
     * This method allows for flexibility in constructing requests for different GPT providers, models, or configurations.
     *
     * @param requestBuilder The RequestBuilder implementation to use for building the custom Request object.
     * @param context The Context object containing the captured data and chat history.
     * @param odinMessages The JSONArray containing the history of Odin's messages.
     * @return A custom Request object based on the provided RequestBuilder implementation.
     */
    public static Request buildCustomRequest(RequestBuilder requestBuilder, Context context, JSONArray odinMessages) {
        return requestBuilder.buildRequest(context, odinMessages);
    }

    /**
     * Streams the response from the GPT provider and processes the partial responses in real-time.
     * The response data is written to the specified TextHelper object to capture the conversation history.
     * This method handles the formatting of the response JSON and extracts the necessary information.
     *
     * @param odinSays The TextHelper object to write the response data to.
     * @param context The Context object containing the captured data and chat history.
     */
    public static void streamResponse(TextHelper odinSays, Context context) {
        RequestBuilder openAIRequestBuilder = new OpenAIRequestBuilder();
        JSONArray odinMessages = odinSays.getMessages();
        Request request = buildCustomRequest(openAIRequestBuilder, context, odinMessages);
        String model = "";
        boolean hasError = false;
        boolean hasIntro = false;
            if(request != null) {
                OkHttpClient httpClient = new OkHttpClient();
                odinSays.appendStringToFile("\n");
                odinSays.appendOverInfo();

                try (Response response = httpClient.newCall(request).execute()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {

                        // Process the response as a stream
                        String responseChunk;
                        boolean isDone = false;
                        String tempContent = "";
                        while ((responseChunk = responseBody.source().readUtf8Line()) != null) {
                            if (responseChunk.isEmpty()) {
                                continue;
                            }
                            // Process the partial response
                            logger.info(responseChunk);
                            try {

                                    JSONObject jsonResponse = extractValidJson(responseChunk);


                                    if (jsonResponse != null) {
                                        if (jsonResponse.has("done")) {
                                            isDone = true;
                                            odinSays.appendStringToFile(tempContent);
                                            break;
                                        }
                                        model = jsonResponse.getString("model");
                                        if(!hasIntro){
                                            Date date = new Date();
                                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
                                            String strDate = formatter.format(date);
                                            odinSays.appendOdinFirstInfo(model, strDate);
                                            odinSays.appendAssistantInfo();
                                            hasIntro = true;
                                        }
                                        JSONArray choices = jsonResponse.getJSONArray("choices");
                                        JSONObject choice = choices.getJSONObject(0);
                                        JSONObject delta = choice.getJSONObject("delta");
                                        if (delta.has("content")) {
                                            logger.info(delta.get("content").toString());
                                            tempContent += delta.get("content").toString();
                                            if (tempContent.length() > 100) {
                                                odinSays.appendStringToFile(tempContent);
                                                tempContent = "";
                                            }
                                        }
                                    }

                                }catch (Exception e){
                                    String tempResponseChunk;
                                    String problematicResponse = "";
                                    hasError = true;
                                    while ((tempResponseChunk = responseBody.source().readUtf8Line()) != null) {
                                        logger.info(tempResponseChunk);
                                        problematicResponse += tempResponseChunk;
                                    }
                                    Date date = new Date();
                                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
                                    String strDate = formatter.format(date);
                                    odinSays.appendOdinFirstInfo("ERROR", strDate);
                                    odinSays.appendStringToFile("There was a problem processing the response from your specified GPT provider: \n"+problematicResponse);
                                }

                            }
                            odinSays.appendStringToFile("\n");
                            if(!hasError){
                                odinSays.appendOverInfo();
                            }else{
                                odinSays.appendOverInfoWithError();
                            }
                            odinSays.appendUserInfo();
                    }

                } catch (IOException e) {
                    odinSays.appendStringToFile("\n");
                    odinSays.appendOverInfoWithError();
                    odinSays.appendUserInfo();
                    logger.log(Level.SEVERE, "An error occurred: ", e);
                }

            }
    }

    /**
     * Extracts a valid JSON object from a response chunk string.
     * The input string is checked for the presence of valid JSON content and parsed to a JSONObject if valid.
     *
     * @param input The response chunk string to extract the JSON object from.
     * @return The extracted valid JSON object, or null if no valid JSON is found.
     */
    private static JSONObject extractValidJson(String input) {
        //System.out.println("this is input:" + input);

        // Remove the "data: " prefix and parse the JSON object
        String jsonLine = input.trim().substring("data: ".length());

        try {
            if (jsonLine.trim().equals("[DONE]")) {
                return new JSONObject("{\"done\" : true}");
            }
            return new JSONObject(jsonLine);
        } catch (JSONException e) {
            // Handle JSON parsing error, if needed
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }

        return null; // Return null if no valid JSON is found
    }

    /**
     * Counts the number of words in a JSONArray.
     * This method converts the JSONArray to a single JSON-formatted string and splits it into words using non-alphanumeric characters as delimiters.
     *
     * @param messages The JSONArray containing the chat messages.
     * @return The number of words in the JSONArray.
     */
    public static int countWordsInJSONArray(JSONArray messages) {
        // Convert the JSONArray to a single JSON-formatted string
        String jsonString = messages.toString();

        // Split the JSON string into words using non-alphanumeric characters as delimiters
        String[] words = jsonString.split("\\W+");

        // Count all the words
        return words.length;
    }

}
