/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qian.weeno.ui.phone;

import com.qian.weeno.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.qian.weeno.ui.HomeActivity;
import com.qian.weeno.ui.NoteDetailFragment;
import com.google.android.apps.iosched.ui.SimpleSinglePaneActivity;
import com.google.android.apps.iosched.ui.TrackInfoHelperFragment;

import com.qian.weeno.util.UIUtils;

import com.actionbarsherlock.view.MenuItem;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;

/**
 * A single-pane activity that shows a {@link SessionDetailFragment}.
 */
public class NoteDetailActivity extends SimpleSinglePaneActivity /*implements
        TrackInfoHelperFragment.Callbacks*/ {

    private String mKeyId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Uri keyUri = getIntent().getData();

//            getSupportFragmentManager().beginTransaction()
//                    .add(TrackInfoHelperFragment.newFromSessionUri(keyUri),
//                            "track_info")
//                    .commit();
        }
    }

    @Override
    protected Fragment onCreatePane() {
        return new NoteDetailFragment();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            // Up to this session's track details, or Home if no track is available
//            Intent parentIntent;
////            if (mKeyId != null) {
////                parentIntent = new Intent(Intent.ACTION_VIEW,
////                        ScheduleContract.Tracks.buildTrackUri(mKeyId));
////            } else {
//                parentIntent = new Intent(this, HomeActivity.class);
////            }
//
//            NavUtils.navigateUpTo(this, parentIntent);
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public void onTrackInfoAvailable(String trackId, String trackName, int trackColor) {
//        mKeyId = trackId;
//        setTitle(trackName);
//        setActionBarColor(trackColor);
//    }
}
