package com.sarahmizzi.fyp.kodi.jsonrpc.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by Sarah on 07-Feb-16.
 * Refer to Kore Remote on Android.
 */
public interface ApiParameter {
    public JsonNode toJsonNode();
}
