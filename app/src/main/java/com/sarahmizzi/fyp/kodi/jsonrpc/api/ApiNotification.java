package com.sarahmizzi.fyp.kodi.jsonrpc.api;

/**
 * Created by Sarah on 29-Feb-16.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.notifications.Player;

/**
 * Abstract class, based of all the JSON RPC notifications
 *
 * Each specific notification should be a subclass of this.
 */
public abstract class ApiNotification {
    protected static final String METHOD_NODE = "method";
    protected static final String PARAMS_NODE = "params";

    public final String sender;

    /**
     * Constructor from a notification node (starting on "params" node)
     * @param node node
     */
    public ApiNotification(ObjectNode node) {
        sender = node.get("sender").textValue();
    }

    /**
     * Returns this notification name
     */
    public abstract String getNotificationName();

    /**
     * Returns a specific notification present in the Json Node
     *
     * @param node Json node with notification
     * @return Specific notification object
     */
    public static ApiNotification notificationFromJsonNode(JsonNode node) {
        String method = node.get(METHOD_NODE).asText();
        ObjectNode params = (ObjectNode)node.get(PARAMS_NODE);

        ApiNotification result = null;
        if (method.equals(Player.OnPause.NOTIFICATION_NAME)) {
            result = new Player.OnPause(params);
        } else if (method.equals(Player.OnPlay.NOTIFICATION_NAME)) {
            result = new Player.OnPlay(params);
        } else if (method.equals(Player.OnSeek.NOTIFICATION_NAME)) {
            result = new Player.OnSeek(params);
        } else if (method.equals(Player.OnSpeedChanged.NOTIFICATION_NAME)) {
            result = new Player.OnSpeedChanged(params);
        } else if (method.equals(Player.OnStop.NOTIFICATION_NAME)) {
            result = new Player.OnStop(params);
        }

        return result;
    }
}
