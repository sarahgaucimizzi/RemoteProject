package com.sarahmizzi.fyp.kodi.jsonrpc.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiException;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiMethod;

/**
 * Created by Sarah on 08-Feb-16.
 * Refer to Kore Remote on Android.
 */
public class JSONRPC {
    public static final class Ping extends ApiMethod<String> {
        public final static String METHOD_NAME = "JSONRPC.Ping";

        /**
         * Ping responder
         */
        public Ping() {
            super();
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public String resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).textValue();
        }
    }
}
