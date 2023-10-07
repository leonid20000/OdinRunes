package org.odinware;
import okhttp3.*;
import org.json.JSONArray;
/**
 * The RequestBuilder interface represents a builder for creating a HTTP request object
 * used for making API requests.
 */
public interface RequestBuilder {

    /**
     * Builds a HTTP request based on the provided context and Odin messages.
     *
     * @param context The context containing captured data and user options.
     * @param odinMessages The messages exchanged between the user and the assistant.
     * @return A built HTTP request object, or null if no new prompt is present.
     */
    Request buildRequest(Context context, JSONArray odinMessages);
}
