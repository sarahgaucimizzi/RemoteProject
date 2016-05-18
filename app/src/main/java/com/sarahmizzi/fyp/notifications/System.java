package com.sarahmizzi.fyp.notifications;

/**
 * Created by Sarah on 02-Feb-16.
 * Refer to Kore Remote on Android.
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sarahmizzi.fyp.kodi.jsonrpc.api.ApiNotification;

public class System {

    /**
     * System.OnQuit notification
     * XBMC will be closed
     */
    public static class OnQuit extends ApiNotification {
        public static final String NOTIFICATION_NAME = "System.OnQuit";

        public OnQuit(ObjectNode node) {
            super(node);
        }

        public String getNotificationName() {
            return NOTIFICATION_NAME;
        }
    }

    /**
     * System.OnRestart notification
     * The system will be restarted.
     */
    public static class OnRestart extends ApiNotification {
        public static final String NOTIFICATION_NAME = "System.OnRestart";

        public OnRestart(ObjectNode node) {
            super(node);
        }

        public String getNotificationName() {
            return NOTIFICATION_NAME;
        }
    }

    /**
     * System.OnSleep notification
     * The system will be suspended.
     */
    public static class OnSleep extends ApiNotification {
        public static final String NOTIFICATION_NAME = "System.OnSleep";

        public OnSleep(ObjectNode node) {
            super(node);
        }

        public String getNotificationName() {
            return NOTIFICATION_NAME;
        }
    }
}
