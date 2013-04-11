package com.qian.weenoo.service;


import com.qian.weenoo.provider.NoteContract;
import com.qian.weenoo.provider.NoteContract.Keys;
import com.qian.weenoo.util.Hash;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;

import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;
import static com.google.android.apps.iosched.util.LogUtils.LOGD;

/**
 * Background service to add key to the database and send request to server.
 */
public class KeyAddService extends IntentService {
    
    private static final String TAG = makeLogTag(KeyAddService.class);
    
    public static final String ACTION_ADD_KEY = "com.qian.weenoo.action.ADD_KEY";
    public static final String EXTRA_KEY_NAME = "com.qian.weenoo.extra.KEY_NAME";
    public static final String EXTRA_KEY_TIME = "com.qian.weenoo.extra.KEY_TIME";
    
    public KeyAddService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO Auto-generated method stub
        final String action = intent.getAction();
        LOGD(TAG, "get intent, action is " + action);
        
        /**
         * get the key user entered to search, 
         * first, add the key to the database, 
         * second, show the key in the {@link KeyFragment},
         * third, send request to the server to search the key.
         */
        if (ACTION_ADD_KEY.equals(action)) {
            final String keyName = intent.getStringExtra(KeyAddService.EXTRA_KEY_NAME);
            final long keyTime = intent.getLongExtra(KeyAddService.EXTRA_KEY_TIME, -1);
            Uri keyUri = addToDB(keyName, keyTime);
            LOGD(TAG, "key name " + keyName + ", new keyuri is " + keyUri);
            notifyToShow(keyUri);
            return;
        }
    }

    private Uri addToDB(String keyName, long keyTime) {
        // TODO Auto-generated method stub
        ContentValues values = new ContentValues();
        LOGD(TAG, "key name is " + keyName + ", keyTime is " + keyTime);
        values.put(KeysTable.ID, generateKeyId(keyName,keyTime));
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
    
    private void notifyToShow(Uri keyUri) {
        // TODO Auto-generated method stub
        // when get the new key URI, start KeyAddService to add the key
    }
    
    private interface KeysTable {
        String ID = NoteContract.Keys.KEY_ID;       
        String NAME = NoteContract.Keys.KEY_NAME;
        String SEARCH_TIME = NoteContract.Keys.KEY_SEARCH_TIME;
        String STATE = NoteContract.Keys.KEY_STATE;
        
        String NEED_SEARCH = NoteContract.Keys.KEY_STATE_NEED_SEARCH;
        
        Uri CONTENT_URI = NoteContract.addCallerIsSyncAdapterParameter(Keys.CONTENT_URI);
           
    }

}
