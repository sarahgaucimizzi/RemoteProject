package com.sarahmizzi.fyp.kodi.jsonrpc.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.utils.JsonUtils;

/**
 * Created by Sarah on 08-Mar-16.
 */
public class Application {

    public static final class SetVolume extends ApiMethod<Integer> {
        public final static String METHOD_NAME = "Application.SetVolume";

        public SetVolume(String volume) {
            super();
            addParameterToRequest("volume", volume);
        }

        @Override
        public String getMethodName() { return METHOD_NAME; }

        @Override
        public Integer resultFromJson(ObjectNode jsonObject) throws ApiException {
            return JsonUtils.intFromJsonNode(jsonObject, RESULT_NODE);
        }
    }
}
