
package com.wallpaper.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.wallpaper.core.*;
import com.wallpaper.core.Adapter.OnGetViewListener;
import com.wallpaper.core.RestClientHandler.OnRestResponseHandler;

import java.util.ArrayList;

public class HomeActivity extends Activity implements OnRestResponseHandler,
        OnBackStackChangedListener, OnGetViewListener, OnFragmentClickListener {

    private final String TAG = "HomeActivity";
    private final String KEY_LIST_DATA = "list_cache";
    private final String KEY_LIST_POSITION = "list_position";

    private static ImageLoader mImageLoader;
    private ArrayList<NodeCategory> mData;
    private int mPosition = -1;
    private boolean mIgnoreSelection = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.getFragmentManager().addOnBackStackChangedListener(this);
        super.setContentView(R.layout.activity_home);
        this.loadData(savedInstanceState);

        if (savedInstanceState == null) {
            BitmapDisplayer displayer = getResources().getBoolean(
                    R.bool.config_enable_image_fade_in)
                    ? new FadeInBitmapDisplayer(getResources().getInteger(
                            R.integer.config_fade_in_time))
                    : new SimpleBitmapDisplayer();

            final DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .displayer(displayer)
                    .cacheInMemory()
                    .cacheOnDisc()
                    .build();

            final ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                    .threadPriority(Thread.NORM_PRIORITY - 1)
                    .offOutOfMemoryHandling()
                    .tasksProcessingOrder(QueueProcessingType.FIFO)
                    .defaultDisplayImageOptions(options)
                    .build();

            mImageLoader = ImageLoader.getInstance();
            mImageLoader.init(config);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (this.mData != null) {
            outState.putSerializable(KEY_LIST_DATA, this.mData);
            outState.putInt(KEY_LIST_POSITION, this.mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    public void loadData(Bundle savedInstanceState) {

        // Check Network State
        if (!NetworkUtil.getNetworkState(this)) {
            final RetryFragment fragment = RetryFragment.getFragmentWithMessage("No connection");
            this.addFragment(fragment, RetryFragment.TAG, true);
            return;
        }

        if (savedInstanceState == null || savedInstanceState.get(KEY_LIST_DATA) == null) {
            final String url = super.getResources().getString(
                    R.string.config_wallpaper_manifest_url);
            if (url != null && URLUtil.isValidUrl(url)) {
                // Add Loading Fragment
                final LoadingFragment fragment = new LoadingFragment();
                this.addFragment(fragment, LoadingFragment.TAG, true);

                // Load Data
                final RestClientHandler handler = new RestClientHandler(this);
                RestClient.get(this, url, handler);
            }
        } else {
            Log.i(TAG, "Restored Instance");
            this.mData = (ArrayList<NodeCategory>) savedInstanceState.get(KEY_LIST_DATA);
            this.mPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
            if (this.mPosition != -1) {
                mIgnoreSelection = true;
            }

            this.configureActionBar();
        }
    }

    @Override
    public void onResponse(ArrayList<NodeCategory> response) {
        this.mData = response;

        // Add ERROR Fragment
        if (this.mData == null) {
            final RetryFragment fragment = RetryFragment.getFragmentWithMessage();
            this.addFragment(fragment, RetryFragment.TAG, true);
            return;
        }

        if (this.mData.isEmpty()) {
            this.addFragment(null, null, true);
            Toast.makeText(getApplicationContext(), "Empty Manifest!", Toast.LENGTH_SHORT).show();
            return;
        }

        this.configureActionBar();
    }

    public void configureActionBar() {
        super.getActionBar().setDisplayHomeAsUpEnabled(false);
        super.getActionBar().setDisplayShowHomeEnabled(false);

        if (this.mData.size() == 1) {
            final NodeCategory node = this.mData.get(0);
            super.getActionBar().setDisplayShowTitleEnabled(true);
            super.getActionBar().setTitle(node.name);
            this.onCategorySelected(node);
        } else {
            super.getActionBar().setDisplayShowTitleEnabled(false);
            super.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            super.getActionBar().setListNavigationCallbacks(new Adapter(this, this, this.mData),
                    this);
            if (this.mPosition != -1) {
                this.mIgnoreSelection = true;
                super.getActionBar().setSelectedNavigationItem(this.mPosition);
            }
        }
    }

    public void addFragment(Fragment fragment, String tag, boolean clearStack) {
        final FragmentManager fm = super.getFragmentManager();
        final FragmentTransaction transaction = fm.beginTransaction();

        // Clear Back Stack
        if (clearStack) {
            for (int x = 0; x < fm.getBackStackEntryCount(); x++) {
                fm.popBackStack();
            }
        }

        if (fragment != null) {
            if (!clearStack) {
                transaction.replace(R.id.container, fragment, tag);
                transaction.addToBackStack(null);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            } else {
                transaction.replace(R.id.container, fragment, tag);
            }

            try {
                transaction.commit();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.i(TAG, "Item Selection: " + itemPosition);
        this.mPosition = itemPosition;
        if (this.mIgnoreSelection == true) {
            this.mIgnoreSelection = false;
            return false;
        }

        this.onCategorySelected(this.mData.get(itemPosition));
        return false;
    }

    @Override
    public void onCategorySelected(NodeCategory node) {
        final Fragment frag = new CategoryFragment();
        final Bundle args = new Bundle();
        args.putSerializable(CategoryFragment.BUNDLE_TAG, node.wallpaperList);
        frag.setArguments(args);
        this.addFragment(frag, CategoryFragment.FRAGMENT_TAG, true);
    }

    @Override
    public void onWallpaperSelected(NodeWallpaper node) {
        final WallpaperFragment frag = new WallpaperFragment();
        final Bundle args = new Bundle();
        args.putSerializable(WallpaperFragment.BUNDLE_TAG, node);
        frag.setArguments(args);
        this.addFragment(frag, WallpaperFragment.FRAGMENT_TAG, false);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent, LayoutInflater mInflater) {
        final TextView t = new TextView(this);
        t.setText(this.mData.get(position).name);
        t.setTextSize(20);
        t.setPadding(5, 6, 2, 6);
        t.setSingleLine(true);
        return t;
    }

    @Override
    public void onBackStackChanged() {
        final FragmentManager fm = super.getFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            this.configureActionBar();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                final FragmentManager fm = super.getFragmentManager();
                fm.popBackStack();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
