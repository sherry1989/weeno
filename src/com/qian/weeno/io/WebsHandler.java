package com.qian.weeno.io;

import static com.qian.weeno.util.LogUtils.LOGI;
import static com.qian.weeno.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.android.apps.iosched.util.Lists;
import com.google.gson.Gson;

import com.qian.weeno.io.model.WebInfo;
import com.qian.weeno.provider.NoteContract;
import com.qian.weeno.provider.NoteContract.Webs;
import com.qian.weeno.util.Hash;

/**
 * Handler that parses a list of image data into a list of content provider operations.
 */
public class WebsHandler {
    private static final String TAG = makeLogTag(WebsHandler.class);

    public WebsHandler() {
    }

    public ArrayList<ContentProviderOperation> process(WebInfo[] webInfos, String keyId)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        int numEvents = 0;
        if (webInfos != null) {
            numEvents = webInfos.length;
        }

        if (numEvents > 0) {
            LOGI(TAG, "Insert web info for " + keyId);

            for (WebInfo webInfo : webInfos) {                
                // Insert image info
                batch.add(ContentProviderOperation
                        .newInsert(NoteContract
                                .addCallerIsSyncAdapterParameter(Webs.CONTENT_URI))
//                        .withValue(SyncColumns.UPDATED, System.currentTimeMillis())
                        .withValue(Webs.KEY_ID, keyId)
                        .withValue(Webs.WEB_ID, generateImageId(webInfo.site, keyId))
                        .withValue(Webs.WEB_TITLE, webInfo.title)
                        .withValue(Webs.WEB_CONTENT, webInfo.content)
                        .withValue(Webs.WEB_URL, webInfo.site)
                        .withValue(Webs.WEB_STATE, Webs.WEB_STATE_NOT_CHOSEN)
                        .build());
            }
        }

        return batch;
    }
    
    private String generateImageId(String webUrl, String keyId) {
        // TODO Auto-generated method stub
        String hashKey = Hash.md5sum(webUrl + keyId);        
        return Webs.generateWebId(hashKey);
    }
}
