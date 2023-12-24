package org.odinware.odinrunes;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import okhttp3.Request;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GptOpsHelperTest {

    @Mock
    private WellsOfWisdom mockWellsOfWisdom;

    @Mock
    private Context mockContext;

    @Mock
    private JSONArray mockOdinMessages;

    @Mock
    private JSONObject mockGptSettingsJsonObject;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testBuildCustomRequest() throws Exception {
        // Define your test data or behavior for the request builder
        String validApiUrl = "https://example.com/api"; // Replace with a valid API URL
        when(mockWellsOfWisdom.buildRequest(any(Context.class), any(JSONArray.class), any(JSONObject.class)))
                .thenReturn(new Request.Builder().url(validApiUrl).build());

        // Call the method under test
        Request request = GptOpsHelper.buildCustomRequest(mockWellsOfWisdom, mockContext, mockOdinMessages, mockGptSettingsJsonObject);

        // Assert that the request is not null
        assertNotNull(request);

    }

    @Test
    public void testExtractValidJson_ValidInput() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String validJsonInput = "data: {\"model\":\"gpt-3.5-turbo\"}\n";

        // Use reflection to access the private method
        Method extractValidJsonMethod = GptOpsHelper.class.getDeclaredMethod("extractValidJson", String.class);
        extractValidJsonMethod.setAccessible(true);
        JSONObject result = (JSONObject) extractValidJsonMethod.invoke(null, validJsonInput);

        // Assert that the result is not null and contains the expected data
        assertNotNull(result);
        assertEquals(true, result.has("model"));
        assertEquals("gpt-3.5-turbo", result.getString("model"));
    }

    @Test
    public void testExtractValidJson_InvalidInput() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Invalid JSON input
        String invalidJsonInput = "data: invalid-json\n";

        // Use reflection to access the private method
        Method extractValidJsonMethod = GptOpsHelper.class.getDeclaredMethod("extractValidJson", String.class);
        extractValidJsonMethod.setAccessible(true);
        JSONObject result = (JSONObject) extractValidJsonMethod.invoke(null, invalidJsonInput);

        // Assert that the result is null for invalid input
        assertNull(result);
    }

    @Test
    public void testCountWordsInJSONArray() {
        // Create a sample JSONArray
        JSONArray messages = new JSONArray();
        messages.put("{\n" +
                "      \"role\": \"assistant\",\n" +
                "      \"content\": \"\\n\\nHello there, how may I assist you today?\",\n" +
                "    }");
        messages.put("{\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \"\\n\\nWrite me ...\",\n" +
                "    }");

        int wordCount = GptOpsHelper.countWordsInJSONArray(messages);

        // Assert that the word count is as expected
        assertEquals(25, wordCount);
    }
}
