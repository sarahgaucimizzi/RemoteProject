package com.sarahmizzi.fyp.kodi.jsonrpc.api;

/**
 * Created by Sarah on 29-Feb-16.
 */

import com.fasterxml.jackson.databind.JsonNode;

public class ItemType {
    /**
     * Item.Details.Base
     */
    public static class DetailsBase {
        public static final String LABEL = "label";

        public final String label;

        public DetailsBase(JsonNode node) {
            JsonNode labelNode = node.get(LABEL);
            if (labelNode != null)
                label = labelNode.asText();
            else
                label = null;
        }
    }

    /**
     * Item.Details.Source
     */
    public static class Source extends DetailsBase {
        public static final String FILE = "file";

        public final String file;

        public Source(JsonNode node) {
            super(node);
            JsonNode fileNode = node.get(FILE);
            if (fileNode != null)
                file = fileNode.asText();
            else
                file = null;
        }
    }
}

