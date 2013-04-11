package com.qian.weenoo.provider;

import static com.google.android.apps.iosched.util.LogUtils.LOGV;
import static com.google.android.apps.iosched.util.LogUtils.LOGI;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.android.apps.iosched.util.SelectionBuilder;
import com.qian.weenoo.provider.NoteContract.Images;
import com.qian.weenoo.provider.NoteContract.Keys;
import com.qian.weenoo.provider.NoteContract.Webs;
import com.qian.weenoo.provider.NoteDatabase.Tables;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;

/**
 * Provider that stores {@link NoteContract} data. Data is usually inserted
 * by {@link }, and queried by various
 * {@link Activity} instances.
 */
public class NoteProvider extends ContentProvider {
    
    private static final String TAG = makeLogTag(NoteProvider.class);

    private NoteDatabase mOpenHelper;
    
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int KEYS = 100;
    private static final int KEYS_ID = 101;
    private static final int KEYS_NAME = 102;
    private static final int KEYS_ID_IMAGES = 103;
    private static final int KEYS_ID_WEBS = 104;

    private static final int IMAGES = 200;
    private static final int IMAGES_ID = 201;

    private static final int WEBS = 300;
    private static final int WEBS_ID = 301;
    
    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = NoteContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "keys", KEYS);
        matcher.addURI(authority, "keys/*", KEYS_ID);
        matcher.addURI(authority, "keys/name/*", KEYS_NAME);
        matcher.addURI(authority, "keys/*/images", KEYS_ID_IMAGES);
        matcher.addURI(authority, "keys/*/webs", KEYS_ID_WEBS);

        matcher.addURI(authority, "images", IMAGES);
        matcher.addURI(authority, "images/*", IMAGES_ID);

        matcher.addURI(authority, "webs", WEBS);
        matcher.addURI(authority, "webs/*", WEBS_ID);

        return matcher;
    }
    
    @Override
    public boolean onCreate() {
        mOpenHelper = new NoteDatabase(getContext());
        LOGI(TAG, "create database.");
        return true;
    }
    
    private void deleteDatabase() {
        // TODO: wait for content provider operations to finish, then tear down
        mOpenHelper.close();
        Context context = getContext();
        NoteDatabase.deleteDatabase(context);
        mOpenHelper = new NoteDatabase(getContext());
    }
    
    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KEYS:
                return Keys.CONTENT_TYPE;
            case KEYS_ID:
                return Keys.CONTENT_ITEM_TYPE;
            case KEYS_NAME:
                return Keys.CONTENT_TYPE;
            case KEYS_ID_IMAGES:
                return Keys.CONTENT_TYPE;
            case KEYS_ID_WEBS:
                return Keys.CONTENT_TYPE;
            case IMAGES:
                return Images.CONTENT_TYPE;
            case IMAGES_ID:
                return Images.CONTENT_ITEM_TYPE;
            case WEBS:
                return Webs.CONTENT_TYPE;
            case WEBS_ID:
                return Webs.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        LOGV(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
            }
//            case SEARCH_SUGGEST: {
//                final SelectionBuilder builder = new SelectionBuilder();
//
//                // Adjust incoming query to become SQL text match
//                selectionArgs[0] = selectionArgs[0] + "%";
//                builder.table(Tables.SEARCH_SUGGEST);
//                builder.where(selection, selectionArgs);
//                builder.map(SearchManager.SUGGEST_COLUMN_QUERY,
//                        SearchManager.SUGGEST_COLUMN_TEXT_1);
//
//                projection = new String[] {
//                        BaseColumns._ID,
//                        SearchManager.SUGGEST_COLUMN_TEXT_1,
//                        SearchManager.SUGGEST_COLUMN_QUERY
//                };
//
//                final String limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
//                return builder.query(db, projection, null, null, SearchSuggest.DEFAULT_SORT, limit);
//            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        boolean syncToNetwork = !NoteContract.hasCallerIsSyncAdapterParameter(uri);
        switch (match) {
            case KEYS: {
                db.insertOrThrow(Tables.KEYS, null, values);
                getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
                return Keys.buildKeyUri(values.getAsString(Keys.KEY_ID));
            }
            case IMAGES: {
                db.insertOrThrow(Tables.IMAGES, null, values);
                getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
                return Images.buildImageUri(values.getAsString(Images._ID));
            }
            case WEBS: {
                db.insertOrThrow(Tables.WEBS, null, values);
                getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
                return Webs.buildWebUri(values.getAsString(Webs._ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        LOGV(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        boolean syncToNetwork = !NoteContract.hasCallerIsSyncAdapterParameter(uri);
        getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LOGV(TAG, "delete(uri=" + uri + ")");
        if (uri == NoteContract.BASE_CONTENT_URI) {
            // Handle whole database deletes (e.g. when signing out)
            deleteDatabase();
            getContext().getContentResolver().notifyChange(uri, null, false);
            return 1;
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null,
                !NoteContract.hasCallerIsSyncAdapterParameter(uri));
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KEYS: {
                return builder.table(Tables.KEYS);
            }
            case KEYS_ID: {
                final String keyId = Keys.getKeyId(uri);
                return builder.table(Tables.KEYS)
                        .where(Keys.KEY_ID + "=?", keyId);
            }
            case KEYS_NAME: {
                final String keyName = Keys.getKeyName(uri);
                return builder.table(Tables.KEYS)
                        .where(Keys.KEY_NAME + "=?", keyName);
            }
            case IMAGES: {
                return builder.table(Tables.IMAGES);
            }
            case IMAGES_ID: {
                final String imageId = Images.getImageId(uri);
                return builder.table(Tables.IMAGES)
                        .where(Images.IMAGE_ID + "=?", imageId);
            }
            case WEBS: {
                return builder.table(Tables.WEBS);
            }
            case WEBS_ID: {
                final String webId = Webs.getWebId(uri);
                return builder.table(Tables.WEBS)
                        .where(Webs.WEB_ID + "=?", webId);
            }
            case KEYS_ID_IMAGES: {
                final String keyId = Keys.getKeyId(uri);
                return builder.table(Tables.IMAGES)
                        .where(Keys.KEY_ID + "=?", keyId);
            }
            case KEYS_ID_WEBS: {
                final String keyId = Keys.getKeyId(uri);
                return builder.table(Tables.WEBS)
                        .where(Keys.KEY_ID + "=?", keyId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case KEYS: {
                return builder
                        .table(Tables.KEYS)
                        .map(Keys.IMAGES_COUNT, Subquery.KEY_IMAGES_COUNT)
                        .map(Keys.WEBS_COUNT, Subquery.KEY_WEBS_COUNT);
            }
            case KEYS_ID: {
                final String keyId = Keys.getKeyId(uri);
                return builder.table(Tables.KEYS)
                        .where(Keys.KEY_ID + "=?", keyId);
            }
            case KEYS_NAME: {
                final String keyName = Keys.getKeyName(uri);
                return builder.table(Tables.KEYS)
                        .where(Keys.KEY_NAME + "=?", keyName);
            }
            case KEYS_ID_IMAGES: {
                final String keyId = Keys.getKeyId(uri);
                return builder.table(Tables.IMAGES_JOIN_KEYS)
                        .mapToTable(Images.IMAGE_ID, Tables.IMAGES)
                        .mapToTable(Images.IMAGE_URL, Tables.IMAGES)
                        .mapToTable(Images.IMAGE_STATE, Tables.IMAGES)
                        .where(Qualified.IMAGES_KEY_ID + "=?", keyId);
            }
            case KEYS_ID_WEBS: {
                final String keyId = Keys.getKeyId(uri);
                return builder.table(Tables.WEBS_JOIN_KEYS)
                        .mapToTable(Webs._ID, Tables.WEBS)
                        .mapToTable(Webs.WEB_TITLE, Tables.WEBS)
                        .mapToTable(Webs.WEB_CONTENT, Tables.IMAGES)
                        .mapToTable(Webs.WEB_URL, Tables.IMAGES)
                        .mapToTable(Webs.WEB_STATE, Tables.IMAGES)
                        .where(Qualified.WEBS_KEY_ID + "=?", keyId);
            }
            case IMAGES: {
                return builder.table(Tables.IMAGES_JOIN_KEYS)
                        .mapToTable(Images.IMAGE_ID, Tables.IMAGES)
                        .mapToTable(Images.KEY_ID, Tables.IMAGES);
            }
            case IMAGES_ID: {
                final String imageId = Images.getImageId(uri);
                return builder.table(Tables.IMAGES_JOIN_KEYS)
                        .mapToTable(Images.KEY_ID, Tables.IMAGES)
                        .where(Images.IMAGE_ID + "=?", imageId);
            }
            case WEBS: {
                return builder.table(Tables.WEBS_JOIN_KEYS)
                        .mapToTable(Webs._ID, Tables.WEBS)
                        .mapToTable(Webs.KEY_ID, Tables.WEBS);
            }
            case WEBS_ID: {
                final String webId = Webs.getWebId(uri);
                return builder.table(Tables.WEBS)
                        .mapToTable(Webs.KEY_ID, Tables.WEBS)
                        .where(Webs.WEB_ID + "=?", webId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private interface Subquery {
        String KEY_IMAGES_COUNT = "(SELECT COUNT(" + Qualified.IMAGES_IMAGE_ID + ") FROM "
                + Tables.IMAGES + " WHERE " + Qualified.IMAGES_KEY_ID + "="
                + Qualified.KEYS_KEY_ID + ")";

        String KEY_WEBS_COUNT = "(SELECT COUNT(" + Qualified.WEBS_WEB_ID
                + ") FROM " + Tables.WEBS + " WHERE "
                + Qualified.WEBS_KEY_ID + "=" + Qualified.KEYS_KEY_ID + ")";

    }

    /**
     * {@link NoteContract} fields that are fully qualified with a specific
     * parent {@link Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {
        String KEYS_KEY_ID = Tables.KEYS + "." + Keys.KEY_ID;
        String IMAGES_IMAGE_ID = Tables.IMAGES + "." + Images.IMAGE_ID;
        String IMAGES_KEY_ID = Tables.IMAGES + "." + Images.KEY_ID;
        String WEBS_WEB_ID = Tables.WEBS + "." + Webs.WEB_ID;
        String WEBS_KEY_ID = Tables.WEBS + "." + Webs.KEY_ID;

    }
}
