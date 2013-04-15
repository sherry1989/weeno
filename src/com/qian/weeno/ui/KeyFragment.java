package com.qian.weeno.ui;

import com.qian.weeno.provider.NoteContract;
import com.qian.weeno.service.KeyAddService;
import com.qian.weeno.ui.widget.SimpleSectionedListAdapter;
import com.qian.weeno.util.UIUtils;
import com.qian.weeno.util.ParserUtils;
import com.qian.weeno.R;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.actionmodecompat.ActionMode;
import com.google.android.apps.iosched.provider.ScheduleContract;
// import com.google.android.apps.iosched.ui.SessionLivestreamActivity;
// import
// com.google.android.apps.iosched.ui.tablet.SessionsVendorsMultiPaneActivity;
import com.google.android.apps.iosched.util.DetachableResultReceiver;

import static com.qian.weeno.util.LogUtils.LOGW;
import static com.qian.weeno.util.LogUtils.makeLogTag;
import static com.qian.weeno.util.LogUtils.LOGD;
import static com.qian.weeno.util.LogUtils.LOGE;
import static com.qian.weeno.util.LogUtils.LOGI;
import static com.qian.weeno.util.LogUtils.LOGV;

/**
 * A fragment that provides interface to enter a search key and shows the
 * historical keys, etc.
 */
public class KeyFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback,
        DetachableResultReceiver.Receiver {

    private static final String        TAG = makeLogTag(KeyFragment.class);

    private SimpleSectionedListAdapter mAdapter;
    private KeyAdapter                 mKeyAdapter;
    private SparseArray<String>        mLongClickedItemData;
    private View                       mLongClickedView;
    private ActionMode                 mActionMode;
    private boolean                    mScrollToNow;

    public DetachableResultReceiver    mReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * The KeyAdapter is wrapped in a SimpleSectionedListAdapter so that we
         * can show list headers separating out the different days of the
         * conference (Wednesday/Thursday/Friday).
         */
        mKeyAdapter = new KeyAdapter(getActivity());
        mAdapter = new SimpleSectionedListAdapter(getActivity(),
                                                  R.layout.list_item_key_header,
                                                  mKeyAdapter);
        setListAdapter(mAdapter);

        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);

        if (savedInstanceState == null) {
            mScrollToNow = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_list_with_empty_container,
                                                      container,
                                                      false);
        inflater.inflate(R.layout.empty_waiting_for_sync,
                         (ViewGroup) root.findViewById(android.R.id.empty),
                         true);
        root.setBackgroundColor(Color.WHITE);
        ListView listView = (ListView) root.findViewById(android.R.id.list);
        listView.setItemsCanFocus(true);
        listView.setCacheColorHint(Color.WHITE);
        listView.setSelector(android.R.color.transparent);
        listView.setEmptyView(root.findViewById(android.R.id.empty));
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        LOGI(TAG, "onActivityCreated()");
        
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a
        // FragmentPagerAdapter in the fragment's onCreate may cause the same
        // LoaderManager to be
        // dealt to multiple fragments because their mIndex is -1 (haven't been
        // added to the
        // activity yet). Thus, we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
                                                @Override
                                                public void onChange(boolean selfChange) {
                                                    if (getActivity() == null) {
                                                        return;
                                                    }

                                                    Loader<Cursor> loader = getLoaderManager().getLoader(0);
                                                    if (loader != null) {
                                                        loader.forceLoad();
                                                    }
                                                }
                                            };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.getContentResolver()
                .registerContentObserver(ScheduleContract.Sessions.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    // LoaderCallbacks
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        LOGI(TAG, "onCreateLoader()");
        return new CursorLoader(getActivity(),
                                NoteContract.Keys.CONTENT_URI,
                                KeysQuery.PROJECTION,
                                null,
                                null,
                                NoteContract.Keys.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        LOGI(TAG, "onLoadFinished()");
        if (getActivity() == null) {
            return;
        }
        
        LOGI(TAG, "before currentTime set ");

        long currentTime = UIUtils.getCurrentTime(getActivity());
        
        LOGI(TAG, "after currentTime set, currentTime is " + currentTime);
        
        int firstNowPosition = ListView.INVALID_POSITION;
        
        LOGI(TAG, "before new sections, currentTime is " + currentTime);

        List<SimpleSectionedListAdapter.Section> sections = new ArrayList<SimpleSectionedListAdapter.Section>();
        
        LOGI(TAG, "after new sections.");
        
        cursor.moveToFirst();
        long previousSearchTime = -1;
        long searchTime;
        while (!cursor.isAfterLast()) {
            searchTime = cursor.getLong(KeysQuery.KEY_SEARCH_TIME);
            LOGI(TAG, "onLoadFinished(), searchTime is " + searchTime);
            if (!UIUtils.isSameDay(previousSearchTime, searchTime)) {
                LOGI(TAG, "not same day, add section, position is " + cursor.getPosition());
                sections.add(new SimpleSectionedListAdapter.Section(cursor.getPosition(),
                                                                    DateUtils.formatDateTime(getActivity(),
                                                                                             searchTime,
                                                                                             DateUtils.FORMAT_ABBREV_MONTH
                                                                                                     | DateUtils.FORMAT_SHOW_DATE
                                                                                                     | DateUtils.FORMAT_SHOW_WEEKDAY)));
            }
            if (mScrollToNow && firstNowPosition == ListView.INVALID_POSITION
            // if we're currently in this block, or we're not in a block
            // and this
            // block is in the future, then this is the scroll position
                && ((searchTime < currentTime/* && currentTime < blockEnd*/) || searchTime > currentTime)) {
                firstNowPosition = cursor.getPosition();
            }
            previousSearchTime = searchTime;
            cursor.moveToNext();
        }

        mKeyAdapter.changeCursor(cursor);

        SimpleSectionedListAdapter.Section[] dummy = new SimpleSectionedListAdapter.Section[sections.size()];
        mAdapter.setSections(sections.toArray(dummy));

        if (mScrollToNow && firstNowPosition != ListView.INVALID_POSITION) {
            firstNowPosition = mAdapter.positionToSectionedPosition(firstNowPosition);
            getListView().setSelectionFromTop(firstNowPosition,
                                              getResources().getDimensionPixelSize(R.dimen.list_scroll_top_offset));
            mScrollToNow = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        LOGI(TAG, "onLoaderReset()");
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
//        String title = mLongClickedItemData.get(KeysQuery.STARRED_SESSION_TITLE);
//        String hashtags = mLongClickedItemData.get(KeysQuery.STARRED_SESSION_HASHTAGS);
//        String url = mLongClickedItemData.get(KeysQuery.STARRED_SESSION_URL);
        boolean handled = false;
//        switch (item.getItemId()) {
//            case R.id.menu_map:
//                String roomId = mLongClickedItemData.get(KeysQuery.STARRED_SESSION_ROOM_ID);
//                helper.startMapActivity(roomId);
//                handled = true;
//                break;
//            case R.id.menu_star:
//                String sessionId = mLongClickedItemData.get(KeysQuery.STARRED_SESSION_ID);
//                Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(sessionId);
//                helper.setSessionStarred(sessionUri, false, title);
//                handled = true;
//                break;
//            case R.id.menu_share:
//                // On ICS+ devices, we normally won't reach this as
//                // ShareActionProvider will handle
//                // sharing.
//                helper.shareSession(getActivity(), R.string.share_template, title, hashtags, url);
//                handled = true;
//                break;
//            case R.id.menu_social_stream:
//                helper.startSocialStream(hashtags);
//                handled = true;
//                break;
//            default:
//                LOGW(TAG, "Unknown action taken");
//        }
//        mActionMode.finish();
        return handled;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.sessions_context, menu);
        MenuItem starMenuItem = menu.findItem(R.id.menu_star);
        starMenuItem.setTitle(R.string.description_remove_schedule);
        starMenuItem.setIcon(R.drawable.ic_action_remove_schedule);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        if (mLongClickedView != null) {
            UIUtils.setActivatedCompat(mLongClickedView, false);
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // TODO Auto-generated method stub
        HomeActivity activity = (HomeActivity) getActivity();
        if (activity == null) {
            return;
        }

        switch (resultCode) {
            case KeyAddService.STATUS_ADD_FINISHED: {
                // TODO update key list
                Toast.makeText(activity, "STATUS_ADD_FINISHED", Toast.LENGTH_LONG).show();
                LOGI(TAG, "STATUS_ADD_FINISHED");
                getLoaderManager().restartLoader(0, null, this);
                break;
            }
            case KeyAddService.STATUS_SEARCH_FINISHED: {
                // TODO update key list
                Toast.makeText(activity, "STATUS_SEARCH_FINISHED", Toast.LENGTH_LONG).show();
                LOGI(TAG, "STATUS_SEARCH_FINISHED");
                getLoaderManager().restartLoader(0, null, this);
                break;
            }
            case KeyAddService.STATUS_NOTE_FINISHED: {
                // TODO update key list
                break;
            }
            case KeyAddService.STATUS_ERROR: {
                // Error happened down in KeyAddService, show as toast.
                final String errorText = getString(R.string.toast_serchkey_error,
                                                   resultData.getString(Intent.EXTRA_TEXT));
                Toast.makeText(activity, errorText, Toast.LENGTH_LONG).show();
                break;
            }
        }

        // activity.updateRefreshStatus(mSyncing);
    }

    /**
     * A list adapter that shows historical keys user entered as list items. It
     * handles a number of different cases, such as empty blocks where the user
     * has not chosen a session, blocks with conflicts (i.e. multiple sessions
     * chosen), non-session blocks, etc.
     */
    private class KeyAdapter extends CursorAdapter {

        public KeyAdapter(Context context) {
            super(context, null, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LOGI(TAG, "newView()");
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_key_block,
                                                             parent,
                                                             false);
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {
            LOGI(TAG, "bindView()");
            
            final String keyId = cursor.getString(KeysQuery.KEY_ID);
            final String keyName = cursor.getString(KeysQuery.KEY_NAME);
            final long keyTime = cursor.getLong(KeysQuery.KEY_SEARCH_TIME);
            final String keyState = cursor.getString(KeysQuery.KEY_STATE);
            final String keyTimeString = UIUtils.formatTimeString(keyTime, context);

            final TextView timeView = (TextView) view.findViewById(R.id.key_search_time);
            final TextView nameView = (TextView) view.findViewById(R.id.key_name);
            final TextView stateView = (TextView) view.findViewById(R.id.key_state);
            final ImageButton extraButton = (ImageButton) view.findViewById(R.id.extra_button);
            final View primaryTouchTargetView = view.findViewById(R.id.list_item_middle_container);

            final Resources res = getResources();

            primaryTouchTargetView.setOnLongClickListener(null);
            UIUtils.setActivatedCompat(primaryTouchTargetView, false);

            timeView.setText(DateUtils.formatDateTime(context,
                                                      keyTime,
                                                      DateUtils.FORMAT_SHOW_TIME
                                                              | DateUtils.FORMAT_12HOUR));
            
            nameView.setText(keyName);
            stateView.setText(keyState);
            extraButton.setVisibility(View.VISIBLE);

//            // Show past/present/future and livestream status for this block.
//            UIUtils.updateTimeAndLivestreamBlockUI(context,
//                                                   blockStart,
//                                                   blockEnd,
//                                                   isLiveStreamed,
//                                                   view,
//                                                   titleView,
//                                                   subtitleView,
//                                                   subtitle);
        }
    }

    private interface KeysQuery {

        String[] PROJECTION      = {     BaseColumns._ID,
                                         NoteContract.Keys.KEY_ID,
                                         NoteContract.Keys.KEY_NAME,
                                         NoteContract.Keys.KEY_SEARCH_TIME,
                                         NoteContract.Keys.KEY_STATE,
                                         NoteContract.Keys.IMAGES_COUNT,
                                         NoteContract.Keys.WEBS_COUNT,};

        int      _ID             = 0;
        int      KEY_ID          = 1;
        int      KEY_NAME        = 2;
        int      KEY_SEARCH_TIME = 3;
        int      KEY_STATE       = 4;
        int      KEY_IMAGES_COUNT = 5;
        int      KEY_WEBS_COUNT       = 6;
    }

}
