package com.qian.weeno.ui;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.google.android.apps.iosched.calendar.SessionAlarmService;
import com.google.android.apps.iosched.util.HelpUtils;
import com.google.android.apps.iosched.util.UIUtils;

import com.qian.weeno.Config;
import com.qian.weeno.service.KeyAddService;
import com.qian.weeno.tracking.EasyTracker;
import com.qian.weeno.BuildConfig;
import com.qian.weeno.R;

import static com.qian.weeno.util.LogUtils.LOGD;
import static com.qian.weeno.util.LogUtils.LOGI;
import static com.qian.weeno.util.LogUtils.LOGW;
import static com.qian.weeno.util.LogUtils.makeLogTag;

/**
 * The landing screen for the app, once the user has logged in.
 * 
 * <p>
 * This activity uses different layouts to present its various fragments,
 * depending on the device configuration. {@link KeyFragment},
 * {@link NoteDetailFragment}, and {@link WebPageFragment} are always available to the
 * user. {@link WhatsOnFragment} is always available on tablets and phones in
 * portrait, but is hidden on phones held in landscape.
 * 
 * <p>
 * On phone-size screens, the three fragments are represented by
 * {@link ActionBar} tabs, and can are held inside a {@link ViewPager} to allow
 * horizontal swiping.
 * 
 * <p>
 * On tablets, the three fragments are always visible and are presented as
 * either three panes (landscape) or a grid (portrait).
 */
public class HomeActivity extends BaseActivity implements ActionBar.TabListener,
        ViewPager.OnPageChangeListener {

    private static final String TAG = makeLogTag(HomeActivity.class);

    private Object              mSyncObserverHandle;

    private KeyFragment         mKeyFragment;
//    private NoteDetailFragment        mNoteFragment;
    private WebPageFragment     mWebPageFragment;

    private ViewPager           mViewPager;
    private Menu                mOptionsMenu;

    // private AsyncTask<Void, Void, Void> mGCMRegisterTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        UIUtils.enableDisableActivities(this);
        EasyTracker.getTracker().setContext(this);
        setContentView(R.layout.activity_home);
        FragmentManager fm = getSupportFragmentManager();

        mViewPager = (ViewPager) findViewById(R.id.pager);
        String homeScreenLabel;
        if (mViewPager != null) {
            // Phone setup
            LOGI(TAG, "Phone setup");
            mViewPager.setAdapter(new HomePagerAdapter(getSupportFragmentManager()));
            mViewPager.setOnPageChangeListener(this);
            mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
            mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

            final ActionBar actionBar = getSupportActionBar();
//            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);        //disable tabs in ActionBar
            actionBar.addTab(actionBar.newTab().setText(R.string.title_key).setTabListener(this));
//            actionBar.addTab(actionBar.newTab().setText(R.string.title_note).setTabListener(this));
//            actionBar.addTab(actionBar.newTab()
//                                      .setText(R.string.title_webpage)
//                                      .setTabListener(this));

            homeScreenLabel = getString(R.string.title_key);

        } else {
//            mNoteFragment = (NoteDetailFragment) fm.findFragmentById(R.id.fragment_note);
            mKeyFragment = (KeyFragment) fm.findFragmentById(R.id.fragment_key);
            mWebPageFragment = (WebPageFragment) fm.findFragmentById(R.id.fragment_webpage);

            homeScreenLabel = "Home";
        }
        getSupportActionBar().setHomeButtonEnabled(false);

        EasyTracker.getTracker().trackView(homeScreenLabel);
        LOGD("Tracker", homeScreenLabel);

        // Sync data on load
        if (savedInstanceState == null) {
            // triggerRefresh();
            // registerGCMClient();
        }
    }

    // private void registerGCMClient() {
    // GCMRegistrar.checkDevice(this);
    // if (BuildConfig.DEBUG) {
    // GCMRegistrar.checkManifest(this);
    // }
    //
    // final String regId = GCMRegistrar.getRegistrationId(this);
    //
    // if (TextUtils.isEmpty(regId)) {
    // // Automatically registers application on startup.
    // GCMRegistrar.register(this, Config.GCM_SENDER_ID);
    //
    // } else {
    // // Device is already registered on GCM, check server.
    // if (GCMRegistrar.isRegisteredOnServer(this)) {
    // // Skips registration
    // LOGI(TAG, "Already registered on the GCM server");
    //
    // } else {
    // // Try to register again, but not on the UI thread.
    // // It's also necessary to cancel the task in onDestroy().
    // mGCMRegisterTask = new AsyncTask<Void, Void, Void>() {
    // @Override
    // protected Void doInBackground(Void... params) {
    // boolean registered = ServerUtilities.register(HomeActivity.this, regId);
    // if (!registered) {
    // // At this point all attempts to register with the app
    // // server failed, so we need to unregister the device
    // // from GCM - the app will try to register again when
    // // it is restarted. Note that GCM will send an
    // // unregistered callback upon completion, but
    // // GCMIntentService.onUnregistered() will ignore it.
    // GCMRegistrar.unregister(HomeActivity.this);
    // }
    // return null;
    // }
    //
    // @Override
    // protected void onPostExecute(Void result) {
    // mGCMRegisterTask = null;
    // }
    // };
    // mGCMRegisterTask.execute(null, null, null);
    // }
    // }
    // }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // if (mGCMRegisterTask != null) {
        // mGCMRegisterTask.cancel(true);
        // }
        //
        // try {
        // GCMRegistrar.onDestroy(this);
        // } catch (Exception e) {
        // LOGW(TAG, "GCM unregistration error", e);
        // }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

    @Override
    public void onPageScrolled(int i, float v, int i1) {}

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);

        int titleId = -1;
        switch (position) {
            case 0:
                titleId = R.string.title_key;
                break;
//            case 1:
//                titleId = R.string.title_note;
//                break;
//            case 2:
//                titleId = R.string.title_webpage;
//                break;
        }

        String title = getString(titleId);
        EasyTracker.getTracker().trackView(title);
        LOGD("Tracker", title);

    }

    @Override
    public void onPageScrollStateChanged(int i) {}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);

        // Since the pager fragments don't have known tags or IDs, the only way
        // to persist the
        // reference is to use putFragment/getFragment. Remember, we're not
        // persisting the exact
        // Fragment instance. This mechanism simply gives us a way to persist
        // access to the
        // 'current' fragment instance for the given fragment (which changes
        // across orientation
        // changes).
        //
        // The outcome of all this is that the "Refresh" menu button refreshes
        // the stream across
        // orientation changes.
//        if (mWebPageFragment != null) {
//            getSupportFragmentManager().putFragment(outState, "stream_fragment", mWebPageFragment);
//        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mWebPageFragment == null) {
            mWebPageFragment = (WebPageFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                                                                                         "stream_fragment");
        }
    }

    private class HomePagerAdapter extends FragmentPagerAdapter {
        public HomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return (mKeyFragment = new KeyFragment());

//                case 1:
//                    return (mNoteFragment = new NoteDetailFragment());
//
//                case 2:
//                    return (mWebPageFragment = new WebPageFragment());
            }
            return null;
        }

        @Override
        public int getCount() {
            return 1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getSupportMenuInflater().inflate(R.menu.home, menu);
        setupSearchMenuItem(menu);
        setupAddKeyItem(menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupSearchMenuItem(Menu menu) {
//        MenuItem searchItem = menu.findItem(R.id.menu_search);
//        if (searchItem != null && UIUtils.hasHoneycomb()) {
//            SearchView searchView = (SearchView) searchItem.getActionView();
//            if (searchView != null) {
//                SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
//                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//            }
//        }
    }

    private void setupAddKeyItem(Menu menu) {
        final MenuItem addKeyEditItem = menu.findItem(R.id.menu_add_key);
        if (addKeyEditItem != null) {
            final EditText addKeyEditView = (EditText) addKeyEditItem.getActionView();
            if (addKeyEditView != null) {
                addKeyEditView.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // TODO Auto-generated method stub

                    }

                });

                final HomeActivity tempHomeActivity = this;

                addKeyEditView.setOnEditorActionListener(new OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        // TODO Auto-generated method stub
                        if (actionId == EditorInfo.IME_NULL
                            /*|| actionId == EditorInfo.IME_ACTION_DONE*/
                            && event.getAction() == KeyEvent.ACTION_DOWN) {
                            LOGI(TAG, "The key to add is: " + v.getText());

                            // when get the key user entered, start
                            // KeyAddService to add the key
                            Intent keyAddIntent = new Intent(KeyAddService.ACTION_ADD_KEY,
                                                             null,
                                                             tempHomeActivity,
                                                             KeyAddService.class);
                            keyAddIntent.putExtra(KeyAddService.EXTRA_KEY_NAME, v.getText().toString());
                            keyAddIntent.putExtra(KeyAddService.EXTRA_KEY_TIME,
                                                  System.currentTimeMillis());
                            keyAddIntent.putExtra(KeyAddService.EXTRA_STATUS_RECEIVER, mKeyFragment.mReceiver);
                            tempHomeActivity.startService(keyAddIntent);

//                            addKeyEditView.clearFocus();
                            addKeyEditItem.collapseActionView();
                        }
                        Toast.makeText(tempHomeActivity,
                                       v.getText() + "--" + actionId,
                                       Toast.LENGTH_LONG).show();
                        
                       InputMethodManager imm = (InputMethodManager)getSystemService(tempHomeActivity.INPUT_METHOD_SERVICE);   
                       imm.hideSoftInputFromWindow(v.getWindowToken(), 0);    

                        return true;
                    }

                });
            }

            addKeyEditItem.setOnActionExpandListener(new OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    // Do something when collapsed
                    LOGI(TAG, "onMenuItemActionCollapse");
                    return true; // Return true to collapse action view
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // addKeyEditView.clearFocus();
                    LOGI(TAG, "onMenuItemActionExpand");
                    return true; // Return true to expand action view
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // case R.id.menu_refresh:
        // triggerRefresh();
        // return true;

            case R.id.menu_search:
                if (!UIUtils.hasHoneycomb()) {
                    startSearch(null, false, Bundle.EMPTY, false);
                    return true;
                }
                break;

            case R.id.menu_add_key:
                break;

            case R.id.menu_about:
                HelpUtils.showAbout(this);
                return true;

                // case R.id.menu_sign_out:
                // AccountUtils.signOut(this);
                // finish();
                // return true;
                //
                // case R.id.menu_beam:
                // Intent beamIntent = new Intent(this, BeamActivity.class);
                // startActivity(beamIntent);
                // return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // private void triggerRefresh() {
    // Bundle extras = new Bundle();
    // extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    // if (!UIUtils.isGoogleTV(this)) {
    // ContentResolver.requestSync(
    // new Account(AccountUtils.getChosenAccountName(this),
    // GoogleAccountManager.ACCOUNT_TYPE),
    // ScheduleContract.CONTENT_AUTHORITY, extras);
    // }
    //
    // if (mWebPageFragment != null) {
    // mWebPageFragment.refresh();
    // }
    // }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // mSyncStatusObserver.onStatusChanged(0);
        //
        // // Watch for sync state changes
        // final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
        // ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        // mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask,
        // mSyncStatusObserver);
    }

    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    // private final SyncStatusObserver mSyncStatusObserver = new
    // SyncStatusObserver() {
    // @Override
    // public void onStatusChanged(int which) {
    // runOnUiThread(new Runnable() {
    // @Override
    // public void run() {
    // String accountName =
    // AccountUtils.getChosenAccountName(HomeActivity.this);
    // if (TextUtils.isEmpty(accountName)) {
    // setRefreshActionButtonState(false);
    // return;
    // }
    //
    // Account account = new Account(accountName,
    // GoogleAccountManager.ACCOUNT_TYPE);
    // boolean syncActive = ContentResolver.isSyncActive(
    // account, ScheduleContract.CONTENT_AUTHORITY);
    // boolean syncPending = ContentResolver.isSyncPending(
    // account, ScheduleContract.CONTENT_AUTHORITY);
    // setRefreshActionButtonState(syncActive || syncPending);
    // }
    // });
    // }
    // };

}
