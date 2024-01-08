package org.odinware.odinrunes;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;



/**
 * The {@code GooglePalm2WellsOfWisdomOverVertexAI} class is responsible for constructing a valid HTTP request
 * to the Google Palm2 API via GCP's VertexAI for chat completions.
 * It implements the {@code WellsOfWisdom} interface.
 *
 * <p>Before building the request, it takes a provided context, messages, and GPT settings, and constructs
 * a JSON payload with the appropriate format required by the Palm2 API.
 *
 * <p>Once the request is built, it includes the appropriate headers and temporary access token.
 * It returns the constructed request object.
 */
public class GooglePalm2WellsOfWisdomOverVertexAI implements WellsOfWisdom {
    private static final Logger logger = Logger.getLogger(GooglePalm2WellsOfWisdomOverVertexAI.class.getName());
    private OkHttpClient OkHttpClient = new OkHttpClient();

    /**
     * Constructs an HTTP request to the Google Palm2 API via GCP's VertexAI using the provided context and messages.
     *
     * @param context The user context for the conversation.
     * @param odinMessages The message history for the conversation.
     * @return The constructed request object.
     */
    @Override
    public Request buildRequest(Context context, JSONArray odinMessages, JSONObject gptSettingsJsonObject) throws Exception {

        String API_ENDPOINT = System.getenv("VERTEXAI_API_ENDPOINT");
        if (API_ENDPOINT == null || API_ENDPOINT.isEmpty()) {
            throw new Exception("VERTEXAI_API_ENDPOINT environment variable is not set. This environment variable should be set to the endpoint for the generative AI service provided by the Google Cloud Platform. See VertexAI tutorials for more details.");
        }

        String PROJECT_ID = System.getenv("VERTEXAI_PROJECT_ID");
        if (PROJECT_ID == null || PROJECT_ID.isEmpty()) {
            throw new Exception("VERTEXAI_PROJECT_ID environment variable is not set. See VertexAI tutorials for more details.");
        }

        String MODEL_ID = System.getenv("VERTEXAI_MODEL_ID");
        if (MODEL_ID == null || MODEL_ID.isEmpty()) {
            MODEL_ID = "chat-bison";
        }



        OkHttpClient client = new OkHttpClient();




        boolean hasNewPrompt = false;
        String model = MODEL_ID;
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonBody = new JSONObject();

        JSONArray messages = new JSONArray();
        JSONArray contextInfo = new JSONArray();

        contextInfo.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant."));

        // Add context
        List<Context.CapturedData> capturedDataList = context.getCapturedDataList();
        for (Context.CapturedData capturedData : capturedDataList) {
            String captureMethod = capturedData.getCaptureMethod();
            String capturedText = capturedData.getCapturedText();
            if (captureMethod.equals("Clipboard")) {
                contextInfo.put(new JSONObject().put("role", "system").put("content", "The text content from a portion of the user's clipboard is as follows: " + capturedText));
            } else if (captureMethod.equals("Regionshot (OCR)")) {
                contextInfo.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is as follows: " + capturedText));
            } else if (captureMethod.equals("Scrollshot (OCR)")) {
                contextInfo.put(new JSONObject().put("role", "system").put("content", "The text content captured by OCR from a portion of the user's screen is included below. It might have some redundant lines. \n" + capturedText));
            } else if (captureMethod.equals("File (Live)")) {
                contextInfo.put(new JSONObject().put("role", "system").put("content", "The content of a file is included below: \n" + capturedText));
            } else {
                contextInfo.put(new JSONObject().put("role", "system").put("content", "Some additional information labeled as "+captureMethod+" is included below: \n" + capturedText));
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
                messages.put( new JSONObject().put("struct_val", new JSONObject().put("author", new JSONObject().put("string_val", new JSONArray().put("user"))).put("content", new JSONObject().put("string_val", new JSONArray().put(content)))) );
            } else if ("assistant".equals(role)) {
                // Process assistant messages
                logger.info("Assistant: " + content);
                messages.put( new JSONObject().put("struct_val", new JSONObject().put("author", new JSONObject().put("string_val", new JSONArray().put("assistant"))).put("content", new JSONObject().put("string_val", new JSONArray().put(content)))) );
            } else if ("prompt".equals(role)) {
                // Process the prompt message
                logger.info("Prompt: " + content);
                messages.put( new JSONObject().put("struct_val", new JSONObject().put("author", new JSONObject().put("string_val", new JSONArray().put("user"))).put("content", new JSONObject().put("string_val", new JSONArray().put(content)))) );
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

        if(GptOpsHelper.countWordsInJSONArray(messages) > 6000 && MODEL_ID.contains("chat-bison")){
            model = "chat-bison-32k";
        }



        JSONObject parameters = new JSONObject();
        double temperatureDouble = gptSettingsJsonObject.getDouble("temperature");
        float temperatureFloat = (float) temperatureDouble;

        JSONObject structVal = new JSONObject();
        structVal.put("temperature", new JSONObject().put("float_val", temperatureFloat));
        structVal.put("topP", new JSONObject().put("float_val", 0.8));
        structVal.put("topK", new JSONObject().put("int_val", 40));
        //structVal.put("maxOutputTokens", new JSONObject().put("int_val", 1024));

        parameters.put("struct_val", structVal);

        jsonBody.put("inputs", new JSONArray().put(new JSONObject().put("struct_val",new JSONObject().put("context",new JSONObject().put("string_val",new JSONArray().put(contextInfo.toString()))).put("messages", new JSONObject().put("list_val",messages)) )  )).put("parameters", parameters);









        String accessToken = getAccessTokenFromEnv();
        if (accessToken != null) {
            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url("https://" + API_ENDPOINT + "/v1/projects/" + PROJECT_ID + "/locations/us-central1/publishers/google/models/" + MODEL_ID + ":serverStreamingPredict")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();



            return request;
        } else {
            logger.log(Level.SEVERE, "Failed to obtain access token.");

        }
        return null;
    }

    /**
     * Executes a HTTP request and returns the Response object.
     *
     * @param request The HTTP request object.
     * @return A built HTTP response object, or null if no response.
     */
    @Override
    public Response executeRequest(Request request) {
        try {
            return OkHttpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads from the Response object.
     *
     * @param response The response object.
     * @return A string generated by Google Palm2 and read from the HTTP response object.
     */
    @Override
    public String readFromResponseStream(Response response) {
        JSONArray rootObject=null;
        try {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String chunk;
                String bigChunk="";
                while ((chunk = responseBody.source().readUtf8Line()) != null) {
                    //System.out.println("BigChunk: " + bigChunk);
                    try {
                        try{
                            rootObject = new JSONArray("["+bigChunk+chunk+"{}]");
                        }catch (Exception e){
                            // Let's do this better next time!
                            rootObject = new JSONArray(bigChunk+chunk+"{}]");

                        }
                        logger.info("Roottt: " + rootObject.toString());


                        if (rootObject.length() > 0) {
                            JSONObject jsonObject = rootObject.getJSONObject(0);
                            JSONObject outputs = jsonObject.getJSONArray("outputs").getJSONObject(0);
                            JSONObject structVal = outputs.getJSONObject("structVal");
                            JSONObject candidates = structVal.getJSONObject("candidates")
                                    .getJSONArray("listVal").getJSONObject(0);
                            JSONObject content = candidates.getJSONObject("structVal")
                                    .getJSONObject("content");

                            String contentStr = content.getJSONArray("stringVal").getString(0);
                            System.out.println(contentStr);
                            System.out.println(escapeSpecialCharacters(contentStr));

                            return WellsOfWisdom.finalStringFormatHelper("chat-bison",contentStr);
                        }



                    }catch(Exception e){
                        //e.printStackTrace();
                        // Let's do this better next time!
                    }finally {
                        bigChunk += chunk;
                    }


                }


            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        if(rootObject != null)
            return "data: "+rootObject.toString();
        else return null;
    }

    private static String getAccessTokenFromEnv() {
        // Read the access token from an environment variable
        String accessToken = System.getenv("GCLOUD_VERTEX_AI_ACCESS_TOKEN");

        if (accessToken != null && !accessToken.isEmpty()) {
            return accessToken;
        }

        return null;
    }
    private static String escapeSpecialCharacters(String input) {
        // Escape special characters manually
        input = input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        return input;
    }



}