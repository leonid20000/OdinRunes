package org.odinware;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * The {@code OpenAIRequestBuilder} class is responsible for constructing a valid HTTP request
 * to the OpenAI API for chat completions.
 * It implements the {@code RequestBuilder} interface.
 *
 * <p>Before building the request, it takes a provided context and OpenAI messages, and constructs
 * a JSON payload with the appropriate format required by the OpenAI API.
 *
 * <p>Once the request is built, it includes the appropriate headers and API key.
 * It returns the constructed request object.
 */
public class OpenAIRequestBuilder implements RequestBuilder{
    private static final Logger logger = Logger.getLogger(OpenAIRequestBuilder.class.getName());

    /**
     * Constructs an HTTP request to the OpenAI API using the provided context and OpenAI messages.
     *
     * @param context The user context for the conversation.
     * @param odinMessages The message history for the conversation.
     * @return The constructed request object.
     */
    @Override
    public Request buildRequest(Context context, JSONArray odinMessages) {
        // setting defaults
        String openaiApiKey = System.getenv("OPENAI_API_KEY_ODIN_FIRST");
        String apiUrl = "https://api.openai.com/v1/chat/completions";
        /* TO DO: Read from a config file (if any) and update defaults. */


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
            } else if (captureMethod.equals("Regionshot (OCR)")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is as follows: " + capturedText));
            } else if (captureMethod.equals("Scrollshot (OCR)")) {
                messages.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is included below. It might have some redundant lines. \n" + capturedText));
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

        if(GptOpsHelper.countWordsInJSONArray(messages) > 2000){
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
}
