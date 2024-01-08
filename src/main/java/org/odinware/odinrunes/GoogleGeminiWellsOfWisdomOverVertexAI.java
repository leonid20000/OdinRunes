package org.odinware.odinrunes;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;


/**
 * The {@code GoogleGeminiWellsOfWisdomOverVertexAI} class is responsible for constructing a valid HTTP request
 * to the Google Gemini API via GCP's VertexAI for chat completions.
 * It implements the {@code WellsOfWisdom} interface.
 *
 * <p>Before building the request, it takes a provided context, messages, and GPT settings, and constructs
 * a JSON payload with the appropriate format required by the Gemini API.
 *
 * <p>Once the request is built, it includes the appropriate headers and temporary access token.
 * It returns the constructed request object.
 */
public class GoogleGeminiWellsOfWisdomOverVertexAI implements WellsOfWisdom {
    private static final Logger logger = Logger.getLogger(GoogleGeminiWellsOfWisdomOverVertexAI.class.getName());
    private okhttp3.OkHttpClient OkHttpClient = new OkHttpClient();

    /**
     * Constructs an HTTP request to the Google Gemini API via GCP's VertexAI using the provided context and messages.
     *
     * @param context The context containing captured data.
     * @param odinMessages The messages exchanged between the user and the assistant.
     * @param gptSettingsJsonObject The settings for the GPT model.
     * @return A built HTTP request object, or null if no new prompt is present.
     * @throws Exception If there is an error while building the request.
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
            MODEL_ID = "gemini-pro";
        }



        OkHttpClient client = new OkHttpClient();




        boolean hasNewPrompt = false;
        String model = MODEL_ID;
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject jsonBody = new JSONObject();

        JSONArray contents = new JSONArray();

        contents.put(new JSONObject()
                .put("role", "USER")
                .put("parts", new JSONObject().put("text", "You are a helpful assistant!")));
        contents.put(new JSONObject()
                .put("role", "MODEL")
                .put("parts", new JSONObject().put("text", "Sure. How can I help?")));

        // Add context
        List<Context.CapturedData> capturedDataList = context.getCapturedDataList();
        for (Context.CapturedData capturedData : capturedDataList) {
            String captureMethod = capturedData.getCaptureMethod();
            String capturedText = capturedData.getCapturedText();
            if (captureMethod.equals("Clipboard")) {
                contents.put(new JSONObject()
                        .put("role", "USER")
                        .put("parts", new JSONObject().put("text", "The text content from a portion of my clipboard is as follows: " + capturedText)));
                contents.put(new JSONObject()
                        .put("role", "MODEL")
                        .put("parts", new JSONObject().put("text", "Ok. got it.")));
            } else if (captureMethod.equals("Regionshot (OCR)")) {
                contents.put(new JSONObject()
                        .put("role", "USER")
                        .put("parts", new JSONObject().put("text", "The text content captured by OCR from a portion of my screen is as follows: " + capturedText)));
                contents.put(new JSONObject()
                        .put("role", "MODEL")
                        .put("parts", new JSONObject().put("text", "Ok. got it.")));
            } else if (captureMethod.equals("Scrollshot (OCR)")) {
                contents.put(new JSONObject()
                        .put("role", "USER")
                        .put("parts", new JSONObject().put("text", "The text content captured by OCR from a portion of my screen is included below. It might have some redundant lines. \n" + capturedText)));
                contents.put(new JSONObject()
                        .put("role", "MODEL")
                        .put("parts", new JSONObject().put("text", "Ok. got it.")));
            } else if (captureMethod.equals("File (Live)")) {
                contents.put(new JSONObject()
                        .put("role", "USER")
                        .put("parts", new JSONObject().put("text", "The content of a file is included below: \n" + capturedText)));
                contents.put(new JSONObject()
                        .put("role", "MODEL")
                        .put("parts", new JSONObject().put("text", "Ok. got it.")));
            } else {
                contents.put(new JSONObject()
                        .put("role", "USER")
                        .put("parts", new JSONObject().put("text", "Some additional information labeled as "+captureMethod+" is included below: \n" + capturedText)));
                contents.put(new JSONObject()
                        .put("role", "MODEL")
                        .put("parts", new JSONObject().put("text", "Ok. got it.")));
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
                contents.put(new JSONObject()
                        .put("role", "USER")
                        .put("parts", new JSONObject().put("text", content)));
            } else if ("assistant".equals(role)) {
                // Process assistant messages
                logger.info("Assistant: " + content);
                contents.put(new JSONObject()
                        .put("role", "MODEL")
                        .put("parts", new JSONObject().put("text", content)));
            } else if ("prompt".equals(role)) {
                // Process the prompt message
                logger.info("Prompt: " + content);
                contents.put(new JSONObject()
                        .put("role", "USER")
                        .put("parts", new JSONObject().put("text", content)));
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




        JSONObject parameters = new JSONObject();
        double temperatureDouble = gptSettingsJsonObject.getDouble("temperature");
        float temperatureFloat = (float) temperatureDouble;

        jsonBody.put("contents", contents);


        // Add generation config
        JSONObject generationConfig = new JSONObject()
                .put("temperature", temperatureFloat)
                .put("topP", 0.8)
                .put("topK", 40);

        jsonBody.put("generation_config", generationConfig);









        String accessToken = getAccessTokenFromEnv();
        if (accessToken != null) {
            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url("https://" + API_ENDPOINT + "/v1/projects/" + PROJECT_ID + "/locations/us-central1/publishers/google/models/" + MODEL_ID + ":streamGenerateContent")
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
     * @return A string generated by gemini-pro and read from the HTTP response object.
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
                        logger.info("Root: " + rootObject.toString());


                        if (rootObject.length() > 0) {
                            JSONObject jsonObject = rootObject.getJSONObject(0);

                            JSONObject candidates = jsonObject.getJSONArray("candidates").getJSONObject(0);
                            JSONObject content = candidates.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            JSONObject text = parts.getJSONObject(0);

                            String contentStr = text.getString("text");
                            System.out.println(contentStr);
                            System.out.println(escapeSpecialCharacters(contentStr));

                            if(jsonObject.has("usageMetadata")) {
                                JSONObject usageMetadata = jsonObject.getJSONObject("usageMetadata");
                                //int promptTokenCount = usageMetadata.getInt("promptTokenCount");
                                //int candidatesTokenCount = usageMetadata.getInt("candidatesTokenCount");
                                //int totalTokenCount = usageMetadata.getInt("totalTokenCount");
                                // To Do
                            }


                            return WellsOfWisdom.finalStringFormatHelper("gemini-pro",contentStr);
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