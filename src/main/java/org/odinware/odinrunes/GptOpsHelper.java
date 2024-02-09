package org.odinware.odinrunes;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    /**
     * Builds and returns a custom Request object based on the provided RequestBuilder implementation.
     * This method allows for flexibility in constructing requests for different GPT providers, models, or configurations.
     *
     * @param wellsOfWisdom The RequestBuilder implementation to use for building the custom Request object.
     * @param context The Context object containing the captured data and chat history.
     * @param odinMessages The JSONArray containing the history of Odin's messages.
     * @return A custom Request object based on the provided RequestBuilder implementation.
     */
    public static Request buildCustomRequest(WellsOfWisdom wellsOfWisdom, Context context, JSONArray odinMessages, JSONObject gptSettingsJsonObject) throws Exception {
        return wellsOfWisdom.buildRequest(context, odinMessages, gptSettingsJsonObject);
    }

    /**
     * Streams the response from the GPT provider and processes the partial responses in real-time.
     * The response data is written to the specified TextHelper object to capture the conversation history.
     * This method handles the formatting of the response JSON and extracts the necessary information.
     *
     * @param odinSays The TextHelper object to write the response data to.
     * @param context The Context object containing the captured data and chat history.
     */
    public static void streamResponse(TextHelper odinSays, Context context, JSONObject gptSettingsJsonObject) {
        WellsOfWisdom customWellsOfWisdom = null;
        // Accessing the gptProvider value from the object
        String gptProvider = gptSettingsJsonObject.getString("gptProvider");

        // Switch case based on gptProvider value
        switch (gptProvider) {
            case "OpenAI (gpt-3.5-turbo)":
                // Code to handle OpenAI (gpt-3.5-turbo) provider
                customWellsOfWisdom = new OpenAIWellsOfWisdom();
                logger.info("Using OpenAI (gpt-3.5-turbo)");
                break;
            case "Google's VertexAI (chat-bison)":
                // Code to handle OpenAI (gpt-3.5-turbo) provider
                customWellsOfWisdom = new GooglePalm2WellsOfWisdomOverVertexAI();
                logger.info("Using Google's VertexAI (chat-bison)");
                break;

             case "Google's VertexAI (gemini-pro)":
                 // Code to handle Google's VertexAI (gemini-pro)
                 customWellsOfWisdom = new GoogleGeminiWellsOfWisdomOverVertexAI();
                 logger.info("Using Google's VertexAI (gemini-pro)");
                 break;

            case "Ollama":
                // Code to handle Ollama
                customWellsOfWisdom = new OllamaWellsOfWisdom();
                logger.info("Using Ollama");
                break;

            default:
                // Code to handle the default case (if gptProvider doesn't match any case)
                customWellsOfWisdom = new OllamaWellsOfWisdom();
                logger.info("Unknown provider! Defaulting to OpenAI (gpt-3.5-turbo) instead.");
                break;
        }

        //RequestBuilder customRequestBuilder = new GoogleVertexAIRequestBuilder();

        JSONArray odinMessages = odinSays.getMessages();
        //Request request = buildCustomRequest(openAIRequestBuilder, context, odinMessages);
        Request request = null;
        try {
                request = buildCustomRequest(customWellsOfWisdom, context, odinMessages, gptSettingsJsonObject);

                String model = "";
                boolean hasError = false;
                boolean hasIntro = false;
                if(request != null) {
                    //OkHttpClient httpClient = new OkHttpClient();
                    odinSays.appendStringToFile("\n");
                    odinSays.appendOverInfo();
                    String tempContent = "";

                    try (Response response = customWellsOfWisdom.executeRequest(request)) {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {

                            // Process the response as a stream
                            String responseChunk;
                            boolean isDone = false;

                            while ((responseChunk = customWellsOfWisdom.readFromResponseStream(response)) != null) {
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
                                                tempContent = "";
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

                                    }catch (JSONException e){
                                        String tempResponseChunk;
                                        String problematicResponse = responseChunk;
                                        hasError = true;

                                        Date date = new Date();
                                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
                                        String strDate = formatter.format(date);
                                        odinSays.appendOdinFirstInfo("ERROR", strDate);

                                            while ((tempResponseChunk = responseBody.source().readUtf8Line()) != null) {
                                                logger.info(tempResponseChunk);
                                                problematicResponse += tempResponseChunk;
                                            }
                                        odinSays.appendStringToFile("There was a problem processing the response from your specified GPT provider: \n"+problematicResponse);


                                }

                                }

                            if(!tempContent.isEmpty()){
                                odinSays.appendStringToFile(tempContent);
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
                    } finally {
                    }

                }

        } catch (Exception e) {
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
            String strDate = formatter.format(date);
            odinSays.appendStringToFile("\n");
            odinSays.appendOverInfo();
            odinSays.appendOdinFirstInfo("ERROR", strDate);
            odinSays.appendStringToFile("An error occurred: "+e+"\n");
            odinSays.appendOverInfoWithError();
            odinSays.appendUserInfo();
            logger.log(Level.SEVERE, "An error occurred: ", e);        }
    }

    /**
     * Extracts a valid JSON object from a response chunk string.
     * The input string is checked for the presence of valid JSON content and parsed to a JSONObject if valid.
     *
     * @param input The response chunk string to extract the JSON object from.
     * @return The extracted valid JSON object, or null if no valid JSON is found.
     */
    private static JSONObject extractValidJson (String input) throws JSONException {
        //System.out.println("this is input:" + input);
        String jsonLine=null;
        try {
            // Remove the "data: " prefix and parse the JSON object
            jsonLine = input.trim().substring("data: ".length());
        } catch (Exception e) {
            JSONException customException = new JSONException("Failed to retrieve a valid response from your specified GPT provider. Check your environment variables, particularly the API key and make sure they are set to valid values.\n");
            // Set the original exception as the cause of the custom exception
            customException.initCause(e);
            // Throw the custom exception
            throw customException;
        }

        try {
            if (jsonLine.trim().equals("[DONE]") ) {
                return new JSONObject("{\"done\" : true}");
            }
            return new JSONObject(jsonLine);
        } catch (JSONException e) {
            // Handle JSON parsing error, if needed
            logger.log(Level.SEVERE, "An error occurred: ", e);
            throw new JSONException("Invalid JSON: " + jsonLine, e);
        }

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
