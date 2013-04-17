package com.qian.weeno.ui;

import com.google.android.apps.iosched.calendar.SessionAlarmService;
import com.google.android.apps.iosched.calendar.SessionCalendarService;
import com.google.android.apps.iosched.provider.ScheduleContract;
//import com.google.android.apps.iosched.ui.SessionLivestreamActivity;
import com.google.android.apps.iosched.util.FractionalTouchDelegate;
import com.google.android.apps.iosched.util.HelpUtils;
import com.google.android.apps.iosched.util.ImageFetcher;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.api.android.plus.GooglePlus;
import com.google.api.android.plus.PlusOneButton;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.qian.weeno.R;
import com.qian.weeno.provider.NoteContract;
import com.qian.weeno.service.KeyAddService;
import com.qian.weeno.tracking.EasyTracker;

import static com.qian.weeno.util.LogUtils.LOGW;
import static com.qian.weeno.util.LogUtils.makeLogTag;
import static com.qian.weeno.util.LogUtils.LOGD;
import static com.qian.weeno.util.LogUtils.LOGE;
import static com.qian.weeno.util.LogUtils.LOGI;
import static com.qian.weeno.util.LogUtils.LOGV;

/**
 * A fragment that shows detail information for a key, including images and web pages, etc.
 *
 * <p>This fragment is used in a number of activities, including
 * {@link com.google.android.apps.iosched.ui.phone.NoteDetailActivity},
 * {@link com.google.android.apps.iosched.ui.tablet.SessionsVendorsMultiPaneActivity},
 * {@link com.google.android.apps.iosched.ui.tablet.MapMultiPaneActivity}, etc.
 */
public class NoteDetailFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(NoteDetailFragment.class);

    // Set this boolean extra to true to show a variable height header
    public static final String EXTRA_VARIABLE_HEIGHT_HEADER =
            "com.google.android.iosched.extra.VARIABLE_HEIGHT_HEADER";

    private String mKeyId;
    private Uri mKeyUri;
    
    private String mKeyName;
    private long mkeyTime;

    private String mTitleString;

    private ViewGroup mRootView;
    private TextView mNameView;
    private TextView mTimeView;

    private boolean mImagesCursor = false;
    private boolean mWebsCursor = false;
    private boolean mHasSummaryContent = false;
    private boolean mVariableHeightHeader = false;

    private ImageFetcher mImageFetcher;
//    private List<Runnable> mDeferredUiOperations = new ArrayList<Runnable>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGI(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

//        GooglePlus.initialize(getActivity(), Config.API_KEY, Config.CLIENT_ID);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mKeyUri = intent.getData();
        
        mKeyName = intent.getStringExtra(NoteContract.Keys.KEY_NAME);
        mkeyTime = intent.getLongExtra(NoteContract.Keys.KEY_SEARCH_TIME, -1);

        LOGI(TAG, "keyUri is " + mKeyUri);
        
        if (mKeyUri == null) {
            return;
        }

        mKeyId = NoteContract.Keys.getKeyId(mKeyUri);
        LOGI(TAG, "mKeyId is " + mKeyId);

        mVariableHeightHeader = intent.getBooleanExtra(EXTRA_VARIABLE_HEIGHT_HEADER, false);

        LoaderManager manager = getLoaderManager();
        manager.restartLoader(ImagesQuery._TOKEN, null, this);
        manager.restartLoader(WebsQuery._TOKEN, null, this);

        mImageFetcher = UIUtils.getImageFetcher(getActivity());
        mImageFetcher.setImageFadeIn(false);

//        setHasOptionsMenu(true);

        HelpUtils.maybeShowAddToScheduleTutorial(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        LOGI(TAG, "onCreateView()");
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_key_detail, null);
        
        mNameView = (TextView) mRootView.findViewById(R.id.key_name);
        mTimeView = (TextView) mRootView.findViewById(R.id.key_search_time);
        
        mNameView.setText(mKeyName);
        mTimeView.setText(DateUtils.formatDateTime(getActivity(),
                                                   mkeyTime,
                                                   DateUtils.FORMAT_SHOW_TIME
                                                           | DateUtils.FORMAT_12HOUR));

        if (mVariableHeightHeader) {
            View headerView = mRootView.findViewById(R.id.header_session);
            ViewGroup.LayoutParams layoutParams = headerView.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            headerView.setLayoutParams(layoutParams);
        }

        return mRootView;
    }

    @Override
    public void onStop() {
        super.onStop();

//        if (mInitStarred != mStarred) {
//            // Update Calendar event through the Calendar API on Android 4.0 or new versions.
//            if (UIUtils.hasICS()) {
//                Intent intent;
//                if (mStarred) {
//                    // Set up intent to add session to Calendar, if it doesn't exist already.
//                    intent = new Intent(SessionCalendarService.ACTION_ADD_SESSION_CALENDAR,
//                            mKeyUri);
//                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_START,
//                            mSessionBlockStart);
//                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_END,
//                            mSessionBlockEnd);
//                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_ROOM, mRoomName);
//                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitleString);
//
//                } else {
//                    // Set up intent to remove session from Calendar, if exists.
//                    intent = new Intent(SessionCalendarService.ACTION_REMOVE_SESSION_CALENDAR,
//                            mKeyUri);
//                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_START,
//                            mSessionBlockStart);
//                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_BLOCK_END,
//                            mSessionBlockEnd);
//                    intent.putExtra(SessionCalendarService.EXTRA_SESSION_TITLE, mTitleString);
//                }
//                intent.setClass(getActivity(), SessionCalendarService.class);
//                getActivity().startService(intent);
//            }
//
//            if (mStarred && System.currentTimeMillis() < mSessionBlockStart) {
//                setupNotification();
//            }
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }
    

//    private void setupNotification() {
//        // Schedule an alarm that fires a system notification when expires.
//        final Context ctx = getActivity();
//        Intent scheduleIntent = new Intent(
//                SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
//                null, ctx, SessionAlarmService.class);
//        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, mSessionBlockStart);
//        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, mSessionBlockEnd);
//        ctx.startService(scheduleIntent);
//    }
    
//  private void onKeyQueryComplete(Cursor cursor) {
//  mImagesCursor = true;
//  if (!cursor.moveToFirst()) {
//      return;
//  }
//
//  mTitleString = cursor.getString(ImagesQuery.TITLE);
//
//  // Format time block this session occupies
//  mSessionBlockStart = cursor.getLong(ImagesQuery.BLOCK_START);
//  mSessionBlockEnd = cursor.getLong(ImagesQuery.BLOCK_END);
//  mRoomName = cursor.getString(ImagesQuery.ROOM_NAME);
//  final String subtitle = UIUtils.formatSessionSubtitle(
//          mTitleString, mSessionBlockStart, mSessionBlockEnd, mRoomName, getActivity());
//
//  mTitle.setText(mTitleString);
//
//  mUrl = cursor.getString(ImagesQuery.URL);
//  if (TextUtils.isEmpty(mUrl)) {
//      mUrl = "";
//  }
//
//  mHashtags = cursor.getString(ImagesQuery.HASHTAGS);
//  if (!TextUtils.isEmpty(mHashtags)) {
//      enableSocialStreamMenuItemDeferred();
//  }
//
//  mRoomId = cursor.getString(ImagesQuery.ROOM_ID);
//
//  setupShareMenuItemDeferred();
//  showStarredDeferred(mInitStarred = (cursor.getInt(ImagesQuery.STARRED) != 0));
//
//  final String sessionAbstract = cursor.getString(ImagesQuery.ABSTRACT);
//  if (!TextUtils.isEmpty(sessionAbstract)) {
//      UIUtils.setTextMaybeHtml(mAbstract, sessionAbstract);
//      mAbstract.setVisibility(View.VISIBLE);
//      mHasSummaryContent = true;
//  } else {
//      mAbstract.setVisibility(View.GONE);
//  }
//
//  mPlusOneButton.setSize(PlusOneButton.Size.TALL);
//  String url = cursor.getString(ImagesQuery.URL);
//  if (TextUtils.isEmpty(url)) {
//      mPlusOneButton.setVisibility(View.GONE);
//  } else {
//      mPlusOneButton.setUrl(url);
//  }
//
//  final View requirementsBlock = mRootView.findViewById(R.id.session_requirements_block);
//  final String sessionRequirements = cursor.getString(ImagesQuery.REQUIREMENTS);
//  if (!TextUtils.isEmpty(sessionRequirements)) {
//      UIUtils.setTextMaybeHtml(mRequirements, sessionRequirements);
//      requirementsBlock.setVisibility(View.VISIBLE);
//      mHasSummaryContent = true;
//  } else {
//      requirementsBlock.setVisibility(View.GONE);
//  }
//
//  // Show empty message when all data is loaded, and nothing to show
//  if (mWebsCursor && !mHasSummaryContent) {
//      mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
//  }
//
//  ViewGroup linksContainer = (ViewGroup) mRootView.findViewById(R.id.links_container);
//  linksContainer.removeAllViews();
//
//  LayoutInflater inflater = getLayoutInflater(null);
//
//  boolean hasLinks = false;
//
//  final Context context = mRootView.getContext();
//
//  // Render I/O live link
//  final boolean hasLivestream = !TextUtils.isEmpty(
//          cursor.getString(ImagesQuery.LIVESTREAM_URL));
//  long currentTimeMillis = UIUtils.getCurrentTime(context);
//  if (UIUtils.hasHoneycomb() // Needs Honeycomb+ for the live stream
//          && hasLivestream
//          && currentTimeMillis > mSessionBlockStart
//          && currentTimeMillis <= mSessionBlockEnd) {
//      hasLinks = true;
//
//      // Create the link item
//      ViewGroup linkContainer = (ViewGroup)
//              inflater.inflate(R.layout.list_item_session_link, linksContainer, false);
//      ((TextView) linkContainer.findViewById(R.id.link_text)).setText(
//              R.string.session_link_livestream);
//      linkContainer.setOnClickListener(new View.OnClickListener() {
//          @Override
//          public void onClick(View view) {
//              fireLinkEvent(R.string.session_link_livestream);
////              Intent livestreamIntent = new Intent(Intent.ACTION_VIEW, mSessionUri);
////              livestreamIntent.setClass(context, SessionLivestreamActivity.class);
////              startActivity(livestreamIntent);
//          }
//      });
//
//      linksContainer.addView(linkContainer);
//  }
//
//  // Render normal links
//  for (int i = 0; i < ImagesQuery.LINKS_INDICES.length; i++) {
//      final String linkUrl = cursor.getString(ImagesQuery.LINKS_INDICES[i]);
//      if (!TextUtils.isEmpty(linkUrl)) {
//          hasLinks = true;
//
//          // Create the link item
//          ViewGroup linkContainer = (ViewGroup)
//                  inflater.inflate(R.layout.list_item_session_link, linksContainer, false);
//          ((TextView) linkContainer.findViewById(R.id.link_text)).setText(
//                  ImagesQuery.LINKS_TITLES[i]);
//          final int linkTitleIndex = i;
//          linkContainer.setOnClickListener(new View.OnClickListener() {
//              @Override
//              public void onClick(View view) {
//                  fireLinkEvent(ImagesQuery.LINKS_TITLES[linkTitleIndex]);
//                  Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
//                  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//                  UIUtils.safeOpenLink(context, intent);
//              }
//          });
//
//          linksContainer.addView(linkContainer);
//      }
//  }
//
//  // Show past/present/future and livestream status for this block.
//  UIUtils.updateTimeAndLivestreamBlockUI(context,
//          mSessionBlockStart, mSessionBlockEnd, hasLivestream,
//          null, null, mSubtitle, subtitle);
//  mRootView.findViewById(R.id.session_links_block)
//          .setVisibility(hasLinks ? View.VISIBLE : View.GONE);
//  
//  EasyTracker.getTracker().trackView("Session: " + mTitleString);
//  LOGD("Tracker", "Session: " + mTitleString);
//}

    /**
     * Handle {@link ImagesQuery} {@link Cursor}.
     */
    private void onImageQueryComplete(Cursor cursor) {
        LOGI(TAG, "onImageQueryComplete()");
        
        mImagesCursor = true;
        // TODO: remove existing speakers from layout, since this cursor might be from a data change
        final ViewGroup imagesGroup = (ViewGroup)
                mRootView.findViewById(R.id.key_images_block);
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        boolean hasImages = false;

        while (cursor.moveToNext()) {

            final String imageUrl = cursor.getString(ImagesQuery.IMAGE_URL);
            final String imageState = cursor.getString(ImagesQuery.IMAGE_STATE);
            
            LOGI(TAG, "imageUrl is " + imageUrl);

            final View imageView = inflater
                    .inflate(R.layout.image_detail, imagesGroup, false);
            final ImageView keyImageView = (ImageView) imageView
                    .findViewById(R.id.key_image);

            if (!TextUtils.isEmpty(imageUrl)) {
                mImageFetcher.loadThumbnailImage(imageUrl, keyImageView,
                        R.drawable.person_image_empty);
            }

            
            imageView.setLongClickable(true);
            imageView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // TODO change image state
                    return false;
                }                    
            });

            imagesGroup.addView(imageView);
            hasImages = true;
            mHasSummaryContent = true;
        }

        imagesGroup.setVisibility(hasImages ? View.VISIBLE : View.GONE);

        // Show empty message when all data is loaded, and nothing to show
        if (mImagesCursor && !mHasSummaryContent) {
            mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Handle {@link WebsQuery} {@link Cursor}.
     */
    private void onWebsQueryComplete(Cursor cursor) {
        LOGI(TAG, "onWebsQueryComplete()");
        
        mWebsCursor = true;
        // TODO: remove existing speakers from layout, since this cursor might be from a data change
        final ViewGroup websGroup = (ViewGroup)
                mRootView.findViewById(R.id.key_webs_block);
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        boolean hasWebs = false;

        while (cursor.moveToNext()) {
            final String webTitle = cursor.getString(WebsQuery.WEB_TITLE);
            if (TextUtils.isEmpty(webTitle)) {
                continue;
            }

            final String webAbstract = cursor.getString(WebsQuery.WEB_CONTENT);
            final String webUrl = cursor.getString(WebsQuery.WEB_URL);
            final String webState = cursor.getString(WebsQuery.WEB_STATE);

            final View webView = inflater
                    .inflate(R.layout.web_detail, websGroup, false);
            final TextView webTitleView = (TextView) webView
                    .findViewById(R.id.web_title);
            final TextView webAbstractView = (TextView) webView
                    .findViewById(R.id.web_abstract);
            final TextView webHostView = (TextView) webView
                    .findViewById(R.id.web_host);

            webTitleView.setText(webTitle);
            webAbstractView.setText(webAbstract);
            
            webHostView.setText(getDomainName(webUrl));

            if (!TextUtils.isEmpty(webUrl)) {
                webView.setEnabled(true);
                webView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //TODO show the web page related to the web page
//                        Intent speakerProfileIntent = new Intent(Intent.ACTION_VIEW,
//                                Uri.parse(speakerUrl));
//                        speakerProfileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//                        UIUtils.preferPackageForIntent(getActivity(), speakerProfileIntent,
//                                UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
//                        UIUtils.safeOpenLink(getActivity(), speakerProfileIntent);
                    }
                });
            } else {
                webView.setEnabled(false);
                webView.setOnClickListener(null);
            }
            
            webView.setLongClickable(true);
            webView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // TODO change web state
                    return false;
                }                    
            });

            websGroup.addView(webView);
            hasWebs = true;
            mHasSummaryContent = true;
        }

        websGroup.setVisibility(hasWebs ? View.VISIBLE : View.GONE);

        // Show empty message when all data is loaded, and nothing to show
        if (mWebsCursor && !mHasSummaryContent) {
            mRootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }
    }

//    private void enableSocialStreamMenuItemDeferred() {
//        mDeferredUiOperations.add(new Runnable() {
//            @Override
//            public void run() {
//                mSocialStreamMenuItem.setVisible(true);
//            }
//        });
//        tryExecuteDeferredUiOperations();
//    }
//
//    private void showStarredDeferred(final boolean starred) {
//        mDeferredUiOperations.add(new Runnable() {
//            @Override
//            public void run() {
//                showStarred(starred);
//            }
//        });
//        tryExecuteDeferredUiOperations();
//    }
//
//    private void showStarred(boolean starred) {
//        mStarMenuItem.setTitle(starred
//                ? R.string.description_remove_schedule
//                : R.string.description_add_schedule);
//        mStarMenuItem.setIcon(starred
//                ? R.drawable.ic_action_remove_schedule
//                : R.drawable.ic_action_add_schedule);
//        mStarred = starred;
//    }
//
//    private void setupShareMenuItemDeferred() {
//        mDeferredUiOperations.add(new Runnable() {
//            @Override
//            public void run() {
//                new SessionsHelper(getActivity())
//                        .tryConfigureShareMenuItem(mShareMenuItem, R.string.share_template,
//                                mTitleString, mHashtags, mUrl);
//            }
//        });
//        tryExecuteDeferredUiOperations();
//    }
//
//    private void tryExecuteDeferredUiOperations() {
//        if (mStarMenuItem != null && mSocialStreamMenuItem != null) {
//            for (Runnable r : mDeferredUiOperations) {
//                r.run();
//            }
//            mDeferredUiOperations.clear();
//        }
//    }
    

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.session_detail, menu);
//        mStarMenuItem = menu.findItem(R.id.menu_star);
//        mSocialStreamMenuItem = menu.findItem(R.id.menu_social_stream);
//        mShareMenuItem = menu.findItem(R.id.menu_share);
//        tryExecuteDeferredUiOperations();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        SessionsHelper helper = new SessionsHelper(getActivity());
//        switch (item.getItemId()) {
//            case R.id.menu_map:                
//                EasyTracker.getTracker().trackEvent(
//                        "Session", "Map", mTitleString, 0L);
//                LOGD("Tracker", "Map: " + mTitleString);
//                
//                helper.startMapActivity(mRoomId);
//                return true;
//
//            case R.id.menu_star:
//                boolean star = !mStarred;
//                showStarred(star);
//                helper.setSessionStarred(mKeyUri, star, mTitleString);
//                Toast.makeText(
//                        getActivity(),
//                        getResources().getQuantityString(star
//                                ? R.plurals.toast_added_to_schedule
//                                : R.plurals.toast_removed_from_schedule, 1, 1),
//                        Toast.LENGTH_SHORT).show();
//                
//                EasyTracker.getTracker().trackEvent(
//                        "Session", star ? "Starred" : "Unstarred", mTitleString, 0L);
//                LOGD("Tracker", (star ? "Starred: " : "Unstarred: ") + mTitleString);
//
//                return true;
//
//            case R.id.menu_share:
//                // On ICS+ devices, we normally won't reach this as ShareActionProvider will handle
//                // sharing.
//                helper.shareSession(getActivity(), R.string.share_template, mTitleString,
//                        mHashtags, mUrl);
//                return true;
//
//            case R.id.menu_social_stream:
//                EasyTracker.getTracker().trackEvent(
//                        "Session", "Stream", mTitleString, 0L);
//                LOGD("Tracker", "Stream: " + mTitleString);
//
//                helper.startSocialStream(UIUtils.getSessionHashtagsString(mHashtags));
//                return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Event structure:
     * Category -> "Session Details"
     * Action -> Link Text
     * Label -> Session's Title
     * Value -> 0.
     */
    public void fireLinkEvent(int actionId) {
        EasyTracker.getTracker().trackEvent(
                "Session", getActivity().getString(actionId), mTitleString, 0L);
        LOGD("Tracker", getActivity().getString(actionId) + ": " + mTitleString);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        LOGI(TAG, "onCreateLoader()");
        CursorLoader loader = null;
        if (id == ImagesQuery._TOKEN  && mKeyUri != null){
            Uri imagesUri = NoteContract.Keys.buildImagesDirUri(mKeyId);
            LOGI(TAG, "imagesUri is " + imagesUri);
            loader = new CursorLoader(getActivity(), imagesUri, ImagesQuery.PROJECTION, null, 
                    null, null);
        } else if (id == WebsQuery._TOKEN  && mKeyUri != null){
            Uri websUri = NoteContract.Keys.buildWebsDirUri(mKeyId);
            LOGI(TAG, "websUri is " + websUri);
            loader = new CursorLoader(getActivity(), websUri, WebsQuery.PROJECTION, null, 
                    null, null);
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        LOGI(TAG, "onLoadFinished()");
        if (getActivity() == null) {
            return;
        }

        if (loader.getId() == ImagesQuery._TOKEN) {
            onImageQueryComplete(cursor);
        } else if (loader.getId() == WebsQuery._TOKEN) {
            onWebsQueryComplete(cursor);
        } else {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    /**
     * {@link com.qian.weeno.ui.NoteDetailFragment.Images} query parameters.
     */
    private interface ImagesQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                NoteContract.Images.IMAGE_ID,
                NoteContract.Images.IMAGE_URL,
                NoteContract.Images.IMAGE_STATE,
        };

        int IMAGE_ID = 0;
        int IMAGE_URL = 1;
        int IMAGE_STATE = 2;
//        int REQUIREMENTS = 5;
//        int STARRED = 6;
//        int HASHTAGS = 7;
//        int URL = 8;
//        int YOUTUBE_URL = 9;
//        int PDF_URL = 10;
//        int NOTES_URL = 11;
//        int LIVESTREAM_URL = 12;
//        int ROOM_ID = 13;
//        int ROOM_NAME = 14;
//
//        int[] LINKS_INDICES = {
//                URL,
//                YOUTUBE_URL,
//                PDF_URL,
//                NOTES_URL,
//        };
//
//        int[] LINKS_TITLES = {
//                R.string.session_link_main,
//                R.string.session_link_youtube,
//                R.string.session_link_pdf,
//                R.string.session_link_notes,
//        };
    }
    
    private static String getDomainName(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        }
        catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    /**
     * {@link com.qian.weeno.ui.NoteDetailFragment.Webs} query parameters.
     */
    private interface WebsQuery {
        int _TOKEN = 0x3;

        String[] PROJECTION = {
               NoteContract.Webs.WEB_ID,
               NoteContract.Webs.WEB_TITLE,
               NoteContract.Webs.WEB_CONTENT,
               NoteContract.Webs.WEB_URL,
               NoteContract.Webs.WEB_STATE,
        };

        int WEB_ID = 0;
        int WEB_TITLE = 1;
        int WEB_CONTENT = 2;
        int WEB_URL = 3;
        int WEB_STATE = 4;
    }
}

