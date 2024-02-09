package org.odinware.odinrunes;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
/**
 * The {@code OllamaWellsOfWisdom} class is responsible for constructing a valid HTTP request
 * to the Ollama API for chat completions.
 * It implements the {@code WellsOfWisdom} interface.
 *
 * <p>Before building the request, it takes a provided context, messages, and GPT settings, and constructs
 * a JSON payload with the appropriate format required by the Ollama API.
 *
 * <p>Once the request is built, it includes the appropriate headers and authentication information.
 * It returns the constructed request object.
 */
public class OllamaWellsOfWisdom implements WellsOfWisdom {
    private static final Logger logger = Logger.getLogger(OllamaWellsOfWisdom.class.getName());

    /**
     * Constructs an HTTP request to the Ollama API using the provided context and messages.
     *
     * @param context The context containing captured data.
     * @param odinMessages The messages exchanged between the user and the assistant.
     * @param gptSettingsJsonObject The settings for the GPT model.
     * @return A built HTTP request object, or null if no new prompt is present.
     * @throws Exception If there is an error while building the request.
     */
    @Override
    public Request buildRequest(Context context, JSONArray odinMessages, JSONObject gptSettingsJsonObject) throws Exception{

        String apiUrl = gptSettingsJsonObject.getString("backendURI");
        /* TO DO: Read from a config file (if any) and update defaults. */


        boolean hasNewPrompt = false;
        String model = gptSettingsJsonObject.getString("model");
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
            } else if (captureMethod.equals("Regionshot (OCR)")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is as follows: " + capturedText));
            } else if (captureMethod.equals("Scrollshot (OCR)")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is included below. It might have some redundant lines. \n" + capturedText));
            } else if (captureMethod.equals("File (Live)")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The content of a file is included below: \n" + capturedText));
            } else {
                messages.put(new JSONObject().put("role", "system").put("content", "Some additional information labeled as "+captureMethod+" is included below: \n" + capturedText));
            }
        }

        // Add chat history

        for (int i = 0; i < odinMessages.length(); i++) {
            JSONObject message = odinMessages.getJSONObject(i);
            String role = message.getString("role");
            String content = message.getString("content");

            if ("user".equals(role)) {
                // Process user messages
                logger.info("User: " + content);
                messages.put(new JSONObject().put("role", "user").put("content", content));
            } else if ("assistant".equals(role)) {
                // Process assistant messages
                logger.info("Assistant: " + content);
                messages.put(new JSONObject().put("role", "assistant").put("content", content));
            } else if ("prompt".equals(role)) {
                // Process the prompt message
                logger.info("Prompt: " + content);
                messages.put(new JSONObject().put("role", "user").put("content", content));
                hasNewPrompt = true;
            } else {
                // Handle other roles if needed
                logger.info("Unknown role: " + role);
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


        jsonBody.put("model", model);

        jsonBody.put("messages", messages);

        /* TO DO: Add Temperature */
        //double temperatureDouble = gptSettingsJsonObject.getDouble("temperature");
        //float temperatureFloat = (float) temperatureDouble;
        //jsonBody.put("temperature", temperatureFloat);

        jsonBody.put("stream", true);

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build();
        return request;
    }


    @Override
    public Response executeRequest(Request request){
        OkHttpClient httpClient = new OkHttpClient();
        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {

        }

    }

    @Override
    public String readFromResponseStream(Response response){
        try{
            int responseCode= response.code();
            if (responseCode != 200) {
                throw new RuntimeException("Attempts to connect to the API backend using the specified URI resulted in this response code: " + responseCode);
            }

            ResponseBody responseBody = response.body();
            // Parse the JSON string
            JSONObject jsonObject = new JSONObject(responseBody.source().readUtf8Line());
            // Extract the "done" field
            boolean done = jsonObject.has("done") && jsonObject.getBoolean("done");
            if(!done) {
                if (jsonObject.has("message")) {
                    // Extract the "content" field from the "message" object
                    JSONObject messageObject = jsonObject.getJSONObject("message");
                    String content = messageObject.getString("content");
                    return WellsOfWisdom.finalStringFormatHelper("Ollama-"+jsonObject.getString("model"), content);
                } else {
                    // If "message" doesn't exist, return the original JSONObject as a string
                    return jsonObject.toString();
                }

            } else return "data: [DONE]";

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
