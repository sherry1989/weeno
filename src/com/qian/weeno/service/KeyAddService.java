package com.qian.weeno.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import com.qian.weeno.io.ImagesHandler;
import com.qian.weeno.io.WebsHandler;
import com.qian.weeno.io.model.ErrorResponse;
import com.qian.weeno.io.model.SearchKeyResponse;
import com.qian.weeno.provider.NoteContract;
import com.qian.weeno.provider.NoteContract.Keys;
import com.qian.weeno.util.Hash;

import com.google.android.apps.iosched.io.HandlerException;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.widget.Toast;

import static com.qian.weeno.util.LogUtils.LOGD;
import static com.qian.weeno.util.LogUtils.LOGE;
import static com.qian.weeno.util.LogUtils.LOGI;
import static com.qian.weeno.util.LogUtils.LOGV;
import static com.qian.weeno.util.LogUtils.makeLogTag;

/**
 * Background service to add key to the database and send request to server.
 */
public class KeyAddService extends IntentService {

    private static final String TAG                    = makeLogTag(KeyAddService.class);

    public static final String  ACTION_ADD_KEY         = "com.qian.weenoo.action.ADD_KEY";
    public static final String  ACTION_SEARCH_KEY      = "com.qian.weenoo.action.SEARCH_KEY";
    public static final String  EXTRA_KEY_NAME         = "com.qian.weenoo.extra.KEY_NAME";
    public static final String  EXTRA_KEY_TIME         = "com.qian.weenoo.extra.KEY_TIME";
    public static final String  EXTRA_STATUS_RECEIVER  = "com.qian.weenoo.extra.STATUS_RECEIVER";
    public static final String  EXTRA_KEY_ID         = "com.qian.weenoo.extra.KEY_ID";

    public static final int     STATUS_ERROR           = 0x1;
    public static final int     STATUS_ADD_FINISHED    = 0x2;
    public static final int     STATUS_SEARCH_FINISHED = 0x3;
    public static final int     STATUS_NOTE_FINISHED   = 0x4;

    static {
        // Per
        // http://android-developers.blogspot.com/2011/09/androids-http-clients.html
        if (!UIUtils.hasFroyo()) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private Context             mContext;
    private String              mUserAgent;

    public KeyAddService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        LOGI(TAG, "onCreate()");
        super.onCreate();
        mContext = this;
        mUserAgent = buildUserAgent(mContext);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO Auto-generated method stub
        final String action = intent.getAction();
        LOGI(TAG, "get intent, action is " + action);

        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);

        /**
         * get the key user entered to search, first, add the key to the
         * database, and show the key list in the {@link KeyFragment}, second,
         * try to connect the server and search for the key.
         */
        if (ACTION_ADD_KEY.equals(action)) {
            final String keyName = intent.getStringExtra(KeyAddService.EXTRA_KEY_NAME);
            final long keyTime = intent.getLongExtra(KeyAddService.EXTRA_KEY_TIME, -1);

            // --step 1. add the key to the database
            String keyId = generateKeyId(keyName, keyTime);
            Uri keyUri = addToDB(keyName, keyTime, keyId);
            LOGI(TAG, "key name " + keyName + ", new keyuri is " + keyUri);
            if (receiver != null) {
                receiver.send(STATUS_ADD_FINISHED, Bundle.EMPTY);
            }

            // --step 2. try to connect the server and search for the key
            tryToSearchForKey(keyName, keyId, receiver);
        }

        else if (ACTION_SEARCH_KEY.equals(action)) {
            final String keyName = intent.getStringExtra(KeyAddService.EXTRA_KEY_NAME);
            final String keyId = intent.getStringExtra(KeyAddService.EXTRA_KEY_ID);
            tryToSearchForKey(keyName, keyId, receiver);
        }

    }

    /**
     * try to connect the server and search for the key, first, check the
     * network connectivity, if is connected to the network, first, send request
     * to the server to search the key and insert key related information to
     * database, and then, update the key's state, if is not connected to the
     * network, just show a toast to the user
     */
    private void tryToSearchForKey(String keyName, String keyId, ResultReceiver receiver) {
        // TODO Auto-generated method stub
        if (getNetworkConnectivity()) {

            // --step 1. send request to the server to search the key and
            // insert key related information to database
            try {
                final ContentResolver resolver = mContext.getContentResolver();
                ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

                SearchKeyResponse response = getResForKey(keyName);
                batch.addAll(new ImagesHandler().process(response.imageInfos, keyId));
                batch.addAll(new WebsHandler().process(response.webInfos, keyId));

                try {
                    // Apply all queued up batch operations for local data.
                    resolver.applyBatch(NoteContract.CONTENT_AUTHORITY, batch);
                }
                catch (RemoteException e) {
                    throw new RuntimeException("Problem applying batch operation", e);
                }
                catch (OperationApplicationException e) {
                    throw new RuntimeException("Problem applying batch operation", e);
                }
            }
            catch (IOException e) {
                // TODO Auto-generated catch block

                LOGE(TAG, "Get Error when sending request to server for key " + keyName);

                if (receiver != null) {
                    // Pass back error to surface listener
                    final Bundle bundle = new Bundle();
                    bundle.putString(Intent.EXTRA_TEXT, e.toString());
                    receiver.send(STATUS_ERROR, bundle);
                }

                e.printStackTrace();
            }

            // --step 2. update the key's state
            updateKey(keyId);
            LOGI(TAG, "Finish update key, keyId is " + keyId);

            if (receiver != null) {
                receiver.send(STATUS_SEARCH_FINISHED, Bundle.EMPTY);
            }

            return;
        }

        else {
            LOGI(TAG, "Cannot search key because of disconnect to internet, keyId is " + keyId);

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, "Cannot connect to internet", Toast.LENGTH_LONG)
                         .show();
                }
            });
        }
    }

    private boolean getNetworkConnectivity() {
        // TODO Auto-generated method stub

        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo == null) {
            return false;
        } else {
            return true;
        }

    }

    private int updateKey(String keyId) {
        // TODO Auto-generated method stub

        Uri stateURI = stateUriforKey(keyId);

        ContentValues values = new ContentValues();
        values.put(KeysTable.STATE, KeysTable.COMPLETE_SEARCH);

        return getContentResolver().update(stateURI, values, null, null);
    }

    private Uri addToDB(String keyName, long keyTime, String keyId) {
        // TODO Auto-generated method stub
        ContentValues values = new ContentValues();
        LOGI(TAG, "key name is " + keyName + ", keyTime is " + keyTime);
        values.put(KeysTable.ID, keyId);
        values.put(KeysTable.NAME, keyName);
        values.put(KeysTable.SEARCH_TIME, keyTime);
        values.put(KeysTable.STATE, KeysTable.NEED_SEARCH);

        return getContentResolver().insert(KeysTable.CONTENT_URI, values);

    }

    private String generateKeyId(String keyName, long keyTime) {
        // TODO Auto-generated method stub
        String hashKey = Hash.md5sum(keyName + keyTime);
        return NoteContract.Keys.generateKeyId(hashKey);
    }

    private Uri stateUriforKey(String keyId) {
        return NoteContract.addCallerIsSyncAdapterParameter(NoteContract.Keys.buildKeyUriForState(keyId));
    }

    private SearchKeyResponse getResForKey(String keyName) throws IOException {
        // TODO Auto-generated method stub

        URL url = new URL(com.qian.weeno.Config.SEARCH_KEY_URL
                          + URLEncoder.encode(keyName, "utf-8"));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("User-Agent", mUserAgent);
        urlConnection.setDoInput(true);

        LOGI(TAG, "Geting from URL: " + url);

        urlConnection.connect();
        throwErrors(urlConnection);
        String json = readInputStream(urlConnection.getInputStream());
        return new Gson().fromJson(json, SearchKeyResponse.class);
    }

    /**
     * Build and return a user-agent string that can identify this application
     * to remote servers. Contains the package name and version code.
     */
    private static String buildUserAgent(Context context) {
        String versionName = "unknown";
        int versionCode = 0;

        try {
            final PackageInfo info = context.getPackageManager()
                                            .getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName;
            versionCode = info.versionCode;
        }
        catch (PackageManager.NameNotFoundException ignored) {}

        return context.getPackageName() + "/" + versionName + " (" + versionCode + ") (gzip)";
    }

    private void throwErrors(HttpURLConnection urlConnection) throws IOException {
        final int status = urlConnection.getResponseCode();
        if (status < 200 || status >= 300) {
            String errorMessage = null;
            try {
                String errorContent = readInputStream(urlConnection.getErrorStream());
                LOGV(TAG, "Error content: " + errorContent);
                ErrorResponse errorResponse = new Gson().fromJson(errorContent, ErrorResponse.class);
                errorMessage = errorResponse.error.message;
            }
            catch (IOException ignored) {}
            catch (JsonSyntaxException ignored) {}

            String exceptionMessage = "Error response "
                                      + status
                                      + " "
                                      + urlConnection.getResponseMessage()
                                      + (errorMessage == null ? "" : (": " + errorMessage))
                                      + " for "
                                      + urlConnection.getURL();

            // TODO: the API should return 401, and we shouldn't have to parse
            // the message
            throw (errorMessage != null && errorMessage.toLowerCase().contains("auth")) ? new HandlerException.UnauthorizedException(exceptionMessage)
                                                                                       : new HandlerException(exceptionMessage);
        }
    }

    private static String readInputStream(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String responseLine;
        StringBuilder responseBuilder = new StringBuilder();
        while ((responseLine = bufferedReader.readLine()) != null) {
            responseBuilder.append(responseLine);
        }
        return responseBuilder.toString();
    }

    private interface KeysTable {
        String ID              = NoteContract.Keys.KEY_ID;
        String NAME            = NoteContract.Keys.KEY_NAME;
        String SEARCH_TIME     = NoteContract.Keys.KEY_SEARCH_TIME;
        String STATE           = NoteContract.Keys.KEY_STATE;

        String NEED_SEARCH     = NoteContract.Keys.KEY_STATE_NEED_SEARCH;
        String COMPLETE_SEARCH = NoteContract.Keys.KEY_STATE_COMPLETE_SEARCH;

        Uri    CONTENT_URI     = NoteContract.addCallerIsSyncAdapterParameter(NoteContract.Keys.CONTENT_URI);

    }

}
