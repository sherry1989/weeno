package com.qian.weenoo.provider;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGW;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

import com.qian.weenoo.provider.NoteContract.Keys;
import com.qian.weenoo.provider.NoteContract.KeysColumns;
import com.qian.weenoo.provider.NoteContract.ImagesColumns;
import com.qian.weenoo.provider.NoteContract.WebsColumns;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link NoteProvider}.
 */
public class NoteDatabase extends SQLiteOpenHelper {
    
    private static final String TAG = makeLogTag(NoteDatabase.class);

    private static final String DATABASE_NAME = "note.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_SESSION_TYPE = 1;

    private static final int DATABASE_VERSION = VER_SESSION_TYPE;
    
    interface Tables {
        String KEYS = "keys";
        String IMAGES = "images";
        String WEBS = "webs";
        
        String IMAGES_JOIN_KEYS = "images "
                + "LEFT OUTER JOIN keys ON images.key_id=keys.key_id";
        
        String WEBS_JOIN_KEYS = "webs "
                + "LEFT OUTER JOIN webs ON webs.key_id=keys.key_id";
    }
    
    /** {@code REFERENCES} clauses. */
    private interface References {
        String KEY_ID = "REFERENCES " + Tables.KEYS + "(" + Keys.KEY_ID + ")";
    }

    public NoteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + Tables.KEYS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KeysColumns.KEY_ID + " TEXT NOT NULL,"
                + KeysColumns.KEY_NAME + " TEXT NOT NULL,"
                + KeysColumns.KEY_SEARCH_TIME + " INTEGER NOT NULL,"
                + KeysColumns.KEY_STATE + " TEXT NOT NULL,"
                + "UNIQUE (" + KeysColumns.KEY_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.IMAGES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Keys.KEY_ID + " TEXT " + References.KEY_ID + ","
                + ImagesColumns.IMAGE_ID + " TEXT NOT NULL,"
                + ImagesColumns.IMAGE_URL + " TEXT NOT NULL,"
                + ImagesColumns.IMAGE_STATE + " TEXT NOT NULL,"
                + "UNIQUE (" + ImagesColumns.IMAGE_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.WEBS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Keys.KEY_ID + " TEXT " + References.KEY_ID + ","
                + WebsColumns.WEB_ID + " TEXT NOT NULL,"
                + WebsColumns.WEB_TITLE + " TEXT NOT NULL,"
                + WebsColumns.WEB_CONTENT + " TEXT NOT NULL,"
                + WebsColumns.WEB_URL + " TEXT NOT NULL,"
                + WebsColumns.WEB_STATE + " TEXT NOT NULL,"
                + "UNIQUE (" + WebsColumns.WEB_ID + ") ON CONFLICT REPLACE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        LOGD(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // NOTE: This switch statement is designed to handle cascading database
        // updates, starting at the current version and falling through to all
        // future upgrade cases. Only use "break;" when you want to drop and
        // recreate the entire database.

        int version = oldVersion;

        LOGD(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            LOGW(TAG, "Destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.KEYS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.IMAGES);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.WEBS);

            onCreate(db);
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}
