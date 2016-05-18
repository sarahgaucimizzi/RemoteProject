package com.sarahmizzi.fyp.kodi.jsonrpc.api;

/**
 * Created by Sarah on 29-Feb-16.
 * Refer to Kore Remote on Android.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.sarahmizzi.fyp.utils.JsonUtils;

/**
 * Types from Global.*
 */
public class GlobalType {

    /**
     * Global.Time
     */
    public static class Time {
        public static final String HOURS = "hours";
        public static final String MILLISECONDS = "milliseconds";
        public static final String MINUTES = "minutes";
        public static final String SECONDS = "seconds";

        public final int hours;
        public final int milliseconds;
        public final int minutes;
        public final int seconds;

        public Time(JsonNode node) {
            hours = JsonUtils.intFromJsonNode(node, HOURS, 0);
            milliseconds = JsonUtils.intFromJsonNode(node, MILLISECONDS, 0);
            minutes = JsonUtils.intFromJsonNode(node, MINUTES, 0);
            seconds = JsonUtils.intFromJsonNode(node, SECONDS, 0);
        }

        /**
         * Returns the seconds from midnight that this time object represents
         *
         * @return Seconds from midnight
         */
        public int ToSeconds() {
            return hours * 3600 + minutes * 60 + seconds;
        }
    }

    /**
     * Global.IncrementDecrement
     */
    public interface IncrementDecrement {
        public final String INCREMENT = "increment";
        public final String DECREMENT = "decrement";
    }

}


