package org.odinware.odinrunes;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import okhttp3.MediaType;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class OpenAIWellsOfWisdomTest {

    private OpenAIWellsOfWisdom requestBuilder;

    @Mock
    private Context context;


    private static JSONObject gptSettingsJsonObject;


    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        requestBuilder = new OpenAIWellsOfWisdom();
        gptSettingsJsonObject = new JSONObject("{\"temperature\":0.8,\"gptProvider\":\"OpenAI (gpt-3.5-turbo)\"}");
    }

    @Test
    public void testBuildRequestWithNewPrompt() throws Exception {
        // Arrange
        JSONArray odinMessages = new JSONArray();
        odinMessages.put(new JSONObject().put("role", "assistant")
                .put("content", "Some message."));
        odinMessages.put(new JSONObject().put("role", "user")
                .put("content", "Hello!"));
        odinMessages.put(new JSONObject().put("role", "assistant")
                .put("content", "some other message."));
        //The message history contains a new user prompt (unanswered user message)
        odinMessages.put(new JSONObject().put("role", "prompt")
                .put("content", "some new prompt"));

        // mocking context
        List<Context.CapturedData> capturedDataList = new ArrayList<>();
        capturedDataList.add(new Context.CapturedData("Clipboard", "Test content"));
        when(context.getCapturedDataList()).thenReturn(capturedDataList);

        // Act
        Request request = requestBuilder.buildRequest(context, odinMessages, gptSettingsJsonObject);

        // Assert
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertEquals("https://api.openai.com/v1/chat/completions", request.url().toString());
        assertEquals(MediaType.parse("application/json; charset=utf-8"), request.body().contentType());



    }

    @Test
    public void testBuildRequestWithoutNewPrompt() throws Exception {
        // Arrange
        JSONArray odinMessages = new JSONArray();
        odinMessages.put(new JSONObject().put("role", "assistant")
                .put("content", "Some message."));
        odinMessages.put(new JSONObject().put("role", "user")
                .put("content", "Hello!"));
        odinMessages.put(new JSONObject().put("role", "assistant")
                .put("content", "some other message."));

        // mocking context
        List<Context.CapturedData> capturedDataList = new ArrayList<>();
        capturedDataList.add(new Context.CapturedData("Clipboard", "Test content"));
        when(context.getCapturedDataList()).thenReturn(capturedDataList);

        // Act
        Request request = requestBuilder.buildRequest(context, odinMessages, gptSettingsJsonObject);

        // Assert
        assertEquals(null, request);


    }
}