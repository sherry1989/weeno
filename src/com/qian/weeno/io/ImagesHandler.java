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

import com.qian.weeno.io.model.ImageInfo;
import com.qian.weeno.provider.NoteContract;
import com.qian.weeno.provider.NoteContract.Images;
import com.qian.weeno.util.Hash;

/**
 * Handler that parses a list of image data into a list of content provider operations.
 */
public class ImagesHandler {
    private static final String TAG = makeLogTag(ImagesHandler.class);

    public ImagesHandler() {
    }

    public ArrayList<ContentProviderOperation> process(ImageInfo[] imageInfos, String keyId)
            throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        int numEvents = 0;
        if (imageInfos != null) {
            numEvents = imageInfos.length;
        }

        if (numEvents > 0) {
            LOGI(TAG, "Insert image info for " + keyId);

            for (ImageInfo imageInfo : imageInfos) {                
                // Insert image info
                batch.add(ContentProviderOperation
                        .newInsert(NoteContract
                                .addCallerIsSyncAdapterParameter(Images.CONTENT_URI))
//                        .withValue(SyncColumns.UPDATED, System.currentTimeMillis())
                        .withValue(Images.KEY_ID, keyId)
                        .withValue(Images.IMAGE_ID, generateImageId(imageInfo.imgUrl, keyId))
                        .withValue(Images.IMAGE_URL, imageInfo.imgUrl)
                        .withValue(Images.IMAGE_STATE, Images.IMAGE_STATE_NOT_CHOSEN)
                        .build());
            }
        }

        return batch;
    }
    
    private String generateImageId(String imgUrl, String keyId) {
        // TODO Auto-generated method stub
        String hashKey = Hash.md5sum(imgUrl + keyId);        
        return Images.generateImageId(hashKey);
    }
}
