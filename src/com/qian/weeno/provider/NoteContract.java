package com.qian.weeno.provider;

import com.google.android.apps.iosched.util.ParserUtils;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;


/**
 * Contract class for interacting with {@link NoteProvider}. Unless
 * otherwise noted, all time-based fields are milliseconds since epoch and can
 * be compared against {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri}
 * are generated using stronger {@link String} identifiers, instead of
 * {@code int} {@link BaseColumns#_ID} values, which are prone to shuffle during
 * sync.
 */
public class NoteContract {
    
    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that an entry
     * has never been updated, or doesn't exist yet.
     */
    public static final long UPDATED_NEVER = -2;

    /**
     * Special value for {@link SyncColumns#UPDATED} indicating that the last
     * update time is unknown, usually when inserted from a local file source.
     */
    public static final long UPDATED_UNKNOWN = -1;

    public interface SyncColumns {
        /** Last time this entry was updated or synchronized. */
        String UPDATED = "updated";
    }
    
    interface KeysColumns {
        /** Unique string identifying this key. */
        String KEY_ID = "key_id";
        /** Name of this key. */
        String KEY_NAME = "key_name";
        /** Search time of this key. */
        String KEY_SEARCH_TIME = "key_search_time";
        /** State of this key. */
        String KEY_STATE = "key_state";
    }
    
    interface ImagesColumns {
        /** Unique string identifying this image. */
        String IMAGE_ID = "image_id";
        /** URL of this image. */
        String IMAGE_URL = "image_url";
        /** State of this image. */
        String IMAGE_STATE = "image_state";
    }
    
    interface WebsColumns {
        /** Unique string identifying this web. */
        String WEB_ID = "web_id";
        /** Title of this web page. */
        String WEB_TITLE = "web_title";
        /** Display content of this web page. */
        String WEB_CONTENT = "web_content";
        /** URL of this web page. */
        String WEB_URL = "web_url";
        /** State of this web page. */
        String WEB_STATE = "web_state";
    }
    
    public static final String CONTENT_AUTHORITY = "com.qian.weeno";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    
    private static final String PATH_KEYS = "keys";
    private static final String PATH_IMAGES = "images";
    private static final String PATH_WEBS = "webs";
    private static final String PATH_STATE = "state";
    
    /**
     * Keys are individual key that user enters.
     */
    public static class Keys implements KeysColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_KEYS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.weeno.key";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.weeno.key";
        
        /** Key's possible state. */
        public static final String KEY_STATE_NEED_SEARCH = "need_search";
        public static final String KEY_STATE_COMPLETE_SEARCH = "complete_search";
        public static final String KEY_STATE_COMPLETE_NOTE = "complete_note";
        
        
        /** Count of {@link Images} inside given track. */
        public static final String IMAGES_COUNT = "images_count";
        /** Count of {@link Webs} inside given track. */
        public static final String WEBS_COUNT = "webs_count";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = KeysColumns.KEY_SEARCH_TIME
                + " COLLATE NOCASE ASC";

        /** Build {@link Uri} for requested {@link #KEY_ID}. */
        public static Uri buildKeyUri(String keyId) {
            return CONTENT_URI.buildUpon().appendPath(keyId).build();
        }
        
        /**
         * Build {@link Uri} that references any {@link Images} associated
         * with the requested {@link #KEY_ID}.
         */
        public static Uri buildImagesDirUri(String keyId) {
            return CONTENT_URI.buildUpon().appendPath(keyId).appendPath(PATH_IMAGES).build();
        }
        
        /**
         * Build {@link Uri} that references any {@link Webs} associated
         * with the requested {@link #KEY_ID}.
         */
        public static Uri buildWebsDirUri(String keyId) {
            return CONTENT_URI.buildUpon().appendPath(keyId).appendPath(PATH_WEBS).build();
        }
        
        
        /** Build {@link Uri} for requested {@link #KEY_ID}. */
        public static Uri buildKeyUriForState(String keyId) {
            return buildKeyUri(keyId).buildUpon().appendPath(PATH_STATE).build();
        }

        /** Read {@link #KEY_ID} from {@link Keys} {@link Uri}. */
        public static String getKeyId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
        
        /** Read {@link #KEY_NAME} from {@link Keys} {@link Uri}. */
        public static String getKeyName(Uri uri) {
            return uri.getPathSegments().get(2);
        }
        
        /**
         * Generate a {@link #KEY_ID} that will always match the requested
         * {@link Keys} details.
         */
        public static String generateKeyId(String name) {
            return ParserUtils.sanitizeId(name);
        }
    }
    

    /**
     * Images are searched images that belongs to {@link Keys}.
     */
    public static class Images implements ImagesColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_IMAGES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.weeno.image";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.weeno.image";
        
        /** {@link Keys#KEY_ID} that this image belongs to. */
        public static final String KEY_ID = "key_id";
        
        /** Image's possible state. */
        public static final String IMAGE_STATE_NOT_CHOSEN = "not_chosen";
        public static final String IMAGE_STATE_CHOSEN = "chosen";

        /** Build {@link Uri} for requested {@link #IMAGE_ID}. */
        public static Uri buildImageUri(String imageId) {
            return CONTENT_URI.buildUpon().appendPath(imageId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Keys} associated
         * with the requested {@link #IMAGE_ID}.
         */
        public static Uri buildKeysDirUri(String imageId) {
            return CONTENT_URI.buildUpon().appendPath(imageId).appendPath(PATH_KEYS).build();
        }

        /** Read {@link #IMAGE_ID} from {@link Images} {@link Uri}. */
        public static String getImageId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
        
        /**
         * Generate a {@link #IMAGE_ID} that will always match the requested
         * {@link Images} details.
         */
        public static String generateImageId(String name) {
            return ParserUtils.sanitizeId(name);
        }
    }
    
    /**
     * Webs are searched web pages that belongs to {@link Keys}.
     */
    public static class Webs implements WebsColumns, SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEBS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.weeno.web";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.weeno.web";
        
        /** {@link Keys#KEY_ID} that this web page belongs to. */
        public static final String KEY_ID = "key_id";
        
        /** Web's possible state. */
        public static final String WEB_STATE_NOT_CHOSEN = "not_chosen";
        public static final String WEB_STATE_CHOSEN = "chosen";

        /** Build {@link Uri} for requested {@link #WEB_ID}. */
        public static Uri buildWebUri(String webId) {
            return CONTENT_URI.buildUpon().appendPath(webId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Keys} associated
         * with the requested {@link #WEB_ID}.
         */
        public static Uri buildKeysDirUri(String webId) {
            return CONTENT_URI.buildUpon().appendPath(webId).appendPath(PATH_KEYS).build();
        }

        /** Read {@link #WEB_ID} from {@link Webs} {@link Uri}. */
        public static String getWebId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
        
        /**
         * Generate a {@link #WEB_ID} that will always match the requested
         * {@link Webs} details.
         */
        public static String generateWebId(String name) {
            return ParserUtils.sanitizeId(name);
        }
    }
    
    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
                ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true",
                uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    private NoteContract() {
    }
    
}
