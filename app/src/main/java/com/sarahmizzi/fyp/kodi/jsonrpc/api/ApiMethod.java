package com.sarahmizzi.fyp.kodi.jsonrpc.api;

import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.connection.HostConnection;

import java.io.IOException;

/**
 * Created by Sarah on 05-Feb-16.
 */
public abstract class ApiMethod<T> {
    private static final String TAG = ApiMethod.class.getSimpleName();
    public static final String RESULT_NODE = "result";
    public static final String ERROR_NODE = "error";
    public static final String ID_NODE = "id";
    public static final String METHOD_NODE = "method";
    public static final String PARAMS_NODE = "params";

    // Method Call ID
    private static int lastId = 0;
    protected final int id;

    protected static final ObjectMapper objectMapper = new ObjectMapper();
    protected final ObjectNode jsonRequest;

    public ApiMethod() {
        synchronized (this) {
            this.id = (++lastId % 10000);
        }

        // Create the rpc request object with the common fields according to JSON RPC spec
        jsonRequest = objectMapper.createObjectNode();
        jsonRequest.put("jsonrpc", "2.0");
        jsonRequest.put(METHOD_NODE, getMethodName());
        jsonRequest.put(ID_NODE, id);
    }

    /**
     * Returns the parameters node of the json request object
     * Creates one if necessary
     * @return Parameters node
     */
    protected ObjectNode getParametersNode() {
        ObjectNode params;
        if (jsonRequest.has(PARAMS_NODE)) {
            params = (ObjectNode)jsonRequest.get(PARAMS_NODE);
        } else {
            params = objectMapper.createObjectNode();
            jsonRequest.put(PARAMS_NODE, params);
        }

        return params;
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, int value) {
        getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, String value) {
        if (value != null)
            getParametersNode().put(parameter, value);
    }


    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, Integer value) {
        if (value != null)
            getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, Double value) {
        if (value != null)
            getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, boolean value) {
        getParametersNode().put(parameter, value);
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param values Values to add
     */
    protected void addParameterToRequest(String parameter, String[] values) {
        if (values != null) {
            final ArrayNode arrayNode = objectMapper.createArrayNode();
            for (int i = 0; i < values.length; i++) {
                arrayNode.add(values[i]);
            }
            getParametersNode().put(parameter, arrayNode);
        }
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, ApiParameter value) {
        if (value != null)
            getParametersNode().put(parameter, value.toJsonNode());
    }

    /**
     * Adds a parameter to the request
     * @param parameter Parameter name
     * @param value Value to add
     */
    protected void addParameterToRequest(String parameter, JsonNode value) {
        if (value != null)
            getParametersNode().put(parameter, value);
    }

    /**
     * Returns the id to identify the current method call.
     * An id is generated for each object that is created.
     * @return Method call id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the string json representation of the current method.
     * @return Json string representation of the current method
     */
    public String toJsonString() { return jsonRequest.toString(); }

    /**
     * Returns the json object representation of the current method.
     * @return JsonObject representation of the current method
     */
    public ObjectNode toJsonObject() { return jsonRequest; }

    /**
     * Calls the method represented by this object on the server.
     * This call is always asynchronous. The results will be posted, through the callback parameter,
     * on the specified handler.
     *
     * @param hostConnection Host connection on which to call the method
     * @param callback Callbacks to post the response to
     * @param handler Handler to invoke callbacks on
     */
    public void execute(HostConnection hostConnection, ApiCallback<T> callback, Handler handler) {
        if (hostConnection != null) {
            hostConnection.execute(this, callback, handler);
        } else {
            callback.onError(ApiException.API_NO_CONNECTION, "No connection specified.");
        }
    }

    /**
     * Returns the current method name
     * @return Current method name
     */
    public abstract String getMethodName();

    /**
     * Constructs an object of this method's return type from a json response.
     * This method must be implemented by each subcall to parse the json reponse and create
     * an return object of the appropriate type for this api method.
     *
     * @param jsonResult Json response obtained from a call
     * @return Result object of the appropriate type for this api method
     */
    public T resultFromJson(String jsonResult) throws ApiException{
        try {
            return resultFromJson((ObjectNode)objectMapper.readTree(jsonResult));
        } catch (JsonProcessingException e) {
            throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
        } catch (IOException e) {
            throw new ApiException(ApiException.INVALID_JSON_RESPONSE_FROM_HOST, e);
        }
    }

    /**
     * Constructs an object of this method's return type from a json response.
     * This method must be implemented by each subcall to parse the json reponse and create
     * an return object of the appropriate type for this api method.
     *
     * @param jsonObject Json response obtained from a call
     * @return Result object of the appropriate type for this api method
     */
    public abstract T resultFromJson(ObjectNode jsonObject) throws ApiException;

    /**
     * Default callback for methods which the result doesnt matter
     */
    public static <T> ApiCallback<T> getDefaultActionCallback() {

        return new ApiCallback<T>() {
            @Override
            public void onSuccess(T result) {
            }

            @Override
            public void onError(int errorCode, String description) {
                Log.d(TAG, "Got an error calling a method. Error code: " + errorCode + ", description: " + description);
            }
        };
    }
}

