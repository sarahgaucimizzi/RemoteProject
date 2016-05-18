package com.sarahmizzi.fyp.kodi.jsonrpc.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.utils.JsonUtils;

/**
 * Created by Sarah on 05-Feb-16.
 * Refer to Kore Remote on Android.
 */
public class ApiException extends Exception {
    public static final int INVALID_JSON_RESPONSE_FROM_HOST = 0;
    public static final int IO_EXCEPTION_WHILE_CONNECTING = 1;
    public static final int IO_EXCEPTION_WHILE_SENDING_REQUEST = 2;
    public static final int IO_EXCEPTION_WHILE_READING_RESPONSE = 3;
    public static final int HTTP_RESPONSE_CODE_UNKNOWN = 4;
    public static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = 5;
    public static final int HTTP_RESPONSE_CODE_NOT_FOUND = 6;
    public static int API_ERROR = 100;
    public static int API_NO_CONNECTION = 101;
    public static int API_METHOD_WITH_SAME_ID_ALREADY_EXECUTING = 102;

    private int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Construct exception from other exception
     *
     * @param code              Exception code
     * @param originalException Original exception
     */
    public ApiException(int code, Exception originalException) {
        super(originalException);
        this.code = code;
    }

    /**
     * Construct exception from JSON response
     *
     * @param code         Exception code
     * @param jsonResponse Json response, with an Error node
     */
    public ApiException(int code, ObjectNode jsonResponse) {
        super((jsonResponse.get(ApiMethod.ERROR_NODE) != null) ?
                JsonUtils.stringFromJsonNode(jsonResponse.get(ApiMethod.ERROR_NODE), "message") :
                "No message returned");
        this.code = code;
    }

    /**
     * Internal code of the exception
     *
     * @return Code of the exception
     */
    public int getCode() {
        return code;
    }
}

