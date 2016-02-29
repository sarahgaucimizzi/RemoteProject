package com.sarahmizzi.fyp.kodi.jsonrpc.api;

/**
 * Created by Sarah on 29-Feb-16.
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.sarahmizzi.fyp.utils.JsonUtils;

public class MediaType {


    /**
     * Media.Artwork
     */
    public static class Artwork {
        public static final String BANNER = "banner";
        public static final String TV_SHOW_BANNER = "tvshow.banner";
        public static final String FANART = "fanart";
        public static final String TV_SHOW_FANART = "tvshow.fanart";
        public static final String POSTER = "poster";
        public static final String TV_SHOW_POSTER = "tvshow.poster";
        public static final String THUMB = "thumb";
        public static final String ALBUM_THUMB = "album.thumb";

        public String banner;
        public String fanart;
        public String poster;
        public String thumb;

        public Artwork(JsonNode node) {
            if (node == null) {
                return;
            }

            banner = JsonUtils.stringFromJsonNode(node, BANNER, null);
            if (banner == null)
                banner = JsonUtils.stringFromJsonNode(node, TV_SHOW_BANNER, null);
            fanart = JsonUtils.stringFromJsonNode(node, FANART, null);
            if (fanart == null)
                poster = JsonUtils.stringFromJsonNode(node, TV_SHOW_FANART, null);
            poster = JsonUtils.stringFromJsonNode(node, POSTER, null);
            if (poster == null)
                poster = JsonUtils.stringFromJsonNode(node, TV_SHOW_POSTER, null);
            thumb = JsonUtils.stringFromJsonNode(node, THUMB, null);
            if (thumb == null)
                thumb = JsonUtils.stringFromJsonNode(node, ALBUM_THUMB, null);
        }
    }

    /**
     * Media.Details.Base
     */
    public static class DetailsBase extends ItemType.DetailsBase {
        public static final String FANART = "fanart";
        public static final String THUMBNAIL = "thumbnail";

        public final String fanart;
        public final String thumbnail;

        /**
         * Constructor from Json node
         * @param node Json node
         */
        public DetailsBase(JsonNode node) {
            super(node);
            fanart = JsonUtils.stringFromJsonNode(node, FANART, null);
            thumbnail = JsonUtils.stringFromJsonNode(node, THUMBNAIL, null);
        }

    }

}
