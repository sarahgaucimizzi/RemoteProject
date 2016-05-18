package com.sarahmizzi.fyp.notifications;

/**
 * Created by Sarah on 02-Feb-16.
 * Refer to Kore Remote on Android.
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiNotification;
import com.sarahmizzi.fyp.utils.JsonUtils;

public class Input {

    /**
     * Input.OnInputRequested
     * The user is requested to provide some information
     */
    public static class OnInputRequested extends ApiNotification {
        public static final String NOTIFICATION_NAME = "Input.OnInputRequested";

        public static final String DATA_NODE = "data";

        public final String title;
        public final String type;
        public final String value;

        public OnInputRequested(ObjectNode node) {
            super(node);
            ObjectNode dataNode = (ObjectNode) node.get(DATA_NODE);
            title = JsonUtils.stringFromJsonNode(dataNode, "title");
            type = JsonUtils.stringFromJsonNode(dataNode, "type");
            value = JsonUtils.stringFromJsonNode(dataNode, "value");
        }

        public String getNotificationName() {
            return NOTIFICATION_NAME;
        }
    }
}
