
package com.transcendcode.earful;

import java.io.File;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class LibraryActivity extends Activity implements ServiceConnection
{
    public static final String TAG = "LibraryActivity";
    LibraryFragment mFrag = null;

    private boolean mIsBound = false;
    PlaybackService mBoundService;
    DropSyncService mDropService;
    LibraryScanService mScanService;

    public static final int ID_MENU_SYNC = 1;

    // Scanning fragment and support variables
    private ScanFragment mScanFrag = null;
    private DropFragment mDropFrag = null;
    EarfulApplication mAppState;
    
    private boolean mIsActive = false;

    
    private void synchronizeFragmentManager() {
        if (!mIsActive) {
            return;
        }

        boolean doAddFrag = (mFrag != null && mFrag.getActivity() != this);

        boolean doRemoveScanFrag = (mScanService == null && mScanFrag != null);
        boolean doAddScanFrag = !doRemoveScanFrag && (mScanFrag != null && mScanFrag.getActivity() != this);

        boolean doRemoveDropFrag = (mDropService == null && mDropFrag != null);
        boolean doAddDropFrag = !doRemoveDropFrag && (mDropFrag != null && mDropFrag.getActivity() != this);

        if (doAddFrag ||
            doRemoveScanFrag ||
            doRemoveDropFrag ||
            doAddScanFrag ||
            doAddDropFrag)
        {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            if (doAddFrag)
            {
                transaction.add(android.R.id.content, mFrag);
            }

            if (doRemoveScanFrag)
            {
                transaction.remove(mScanFrag);
                mScanFrag = null;
            }
            else if (doAddScanFrag)
            {
                transaction.add(R.id.scan_container, mScanFrag);
            }

            if (doRemoveDropFrag)
            {
                transaction.remove(mDropFrag);
                mScanFrag = null;
            }
            else if (doAddDropFrag)
            {
                transaction.add(R.id.drop_container, mDropFrag);
            }

            transaction.commit();
        }
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            if (C.D)
                Log.i(TAG, "Got message: " + message);

            if (C.MSG_DROPBOX_SYNC_COMPLETED.equals(message))
            {
                startScan();
            }
        }
    };

    public boolean isServiceRunning(String serviceClassName)
    {
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (RunningServiceInfo runningServiceInfo : services)
        {
        		//if (C.D)
        		//	Log.i(TAG, "Service Running: " + runningServiceInfo.service.getClassName());
            if (runningServiceInfo.service.getClassName().equals(serviceClassName))
            {
            
                return true;
            }
        }
        return false;
    }

    public void onServiceConnected(ComponentName className, IBinder service)
    {
        // This is called when the connection with the service has been
        // established, giving us the service object we can use to
        // interact with the service.  Because we have bound to a explicit
        // service that we know is running in our own process, we can
        // cast its IBinder to a concrete class and directly access it.
        if (className.getClassName().equals("com.transcendcode.earful.PlaybackService"))
        {
            mBoundService = ((PlaybackService.LocalBinder) service).getService();

            mFrag = new LibraryFragment();
            mFrag.setArguments(getIntent().getExtras());
            mFrag.setHasOptionsMenu(true);
/*
            if (getIntent() != null && getIntent().getExtras() != null
                    && getIntent().getExtras().getBoolean(C.EXTRA_START_PLAY, false))
            {
                Intent playbackActivity = new Intent(getBaseContext(), PlaybackActivity.class);
                Bundle extras = new Bundle();
                extras.putString(C.EXTRA_SOURCE, C.SOURCE_MAIN);
                playbackActivity.putExtras(extras);
                startActivity(playbackActivity);
            }
            */
        }
        else if (className.getClassName().equals("com.transcendcode.earful.DropSyncService"))
        {
            mDropService = ((DropSyncService.LocalBinder) service).getService();

            mDropFrag = new DropFragment();
            mDropFrag.setArguments(getIntent().getExtras());
        }
        else if (className.getClassName().equals("com.transcendcode.earful.LibraryScanService"))
        {
            mScanService = ((LibraryScanService.LocalBinder) service).getService();

            mScanFrag = new ScanFragment();
            mScanFrag.setArguments(getIntent().getExtras());
        }

        synchronizeFragmentManager();
    }

    public void onServiceDisconnected(ComponentName className)
    {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        // Because it is running in our same process, we should never
        // see this happen.
        if (C.D)
            Log.i(TAG, "Bound service is now disconnected");

        if (className.getClassName().equals("com.transcendcode.earful.PlaybackService"))
        {
            mBoundService = null;
            mIsBound = false;
        }
        else if (className.getClassName().equals("com.transcendcode.earful.DropSyncService"))
        {
            mDropService = null;
        }
        else if (className.getClassName().equals("com.transcendcode.earful.LibraryScanService"))
        {
            mScanService = null;
        }

        synchronizeFragmentManager();
    }

    public void dropSync()
    {
        String dropDirectory = mAppState.prefs.getString("dropDirectoryPref", "");

        File dir = new File(dropDirectory);

        if (dropDirectory.equals(""))
        {
            Toast.makeText(this, "Please setup a directory for Dropbox download in Settings", Toast.LENGTH_LONG)
                    .show();
        }
        else if (!dir.exists() || !dir.canWrite())
        {
            Toast.makeText(this, "Problem with Dropbox directory. Re-setup the directory.", Toast.LENGTH_LONG)
                    .show();
        }
        else
        {
            Intent svc = new Intent(this, DropSyncService.class);
            startService(svc);

            bindService(new Intent(LibraryActivity.this,
                    DropSyncService.class), this, 0);
        }
    }

    public void startScan()
    {
        Intent svc = new Intent(this, LibraryScanService.class);
        startService(svc);

        bindService(new Intent(LibraryActivity.this,
                LibraryScanService.class), this, 0);

        //  mScanFrag.startScan(Collections.unmodifiableList(paths), old_list);       
    }

    /*
        public void OnScanFinished(List<Book> scan_result)
        {
            BookLibrary lib = mAppState.getLibrary();
            lib.bookList = scan_result;
            mAppState.saveLibrary();

            getSupportFragmentManager().beginTransaction().remove(mScanFrag).commit();

            //mIsScanning = false;
        }
    */
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // super.onCreate(savedInstanceState);
        super.onCreate(null);

        mAppState = (EarfulApplication) getApplicationContext();

        /*
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE)
        {
            // If the screen is now in landscape mode, we can show the
            // dialog in-line with the list so we don't need this activity.
            finish();
            return;
        }
        */

        Intent svc = new Intent(LibraryActivity.this, PlaybackService.class);
        startService(svc);

        doBindService();

        // Let's connect to any background processes that might be running
        if (isServiceRunning("com.transcendcode.earful.DropSyncService"))
        {
            bindService(new Intent(LibraryActivity.this,
                    DropSyncService.class), this, 0);
        }

        if (isServiceRunning("com.transcendcode.earful.LibraryScanService"))
        {
          if (C.D)
            Log.i(TAG, "LibraryScanService is running");

            
            bindService(new Intent(LibraryActivity.this,
                    LibraryScanService.class), this, 0);
        }
        else
          if (C.D)
            Log.i(TAG, "LibraryScanService is NOT running");

        if (savedInstanceState == null)
        {
            // During initial setup, plug in the details fragment.

        }
        
        // TODO: Spin out a thread to check the playback service cache entires and delete one's not in the library
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;
        synchronizeFragmentManager();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;
    }

    @Override
    public void onDestroy()
    {
        doUnbindService();

        super.onDestroy();
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //   adapter.notifyDataSetChanged();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter(C.EARFUL_EVENT));
    }

    @Override
    public void onStop()
    {
        // Call the adapter just in case another book was selected
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);

        super.onStop();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();

        getMenuInflater().inflate(R.menu.activity_main, menu);

        if (mAppState.mDBApi.getSession().isLinked())
        {
            MenuItem item = menu.add(Menu.NONE, LibraryActivity.ID_MENU_SYNC, Menu.NONE, "Dropbox Sync");

            item.setIcon(R.drawable.ic_menu_download);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        // Handle item selection
        switch (item.getItemId())
        {
            case LibraryActivity.ID_MENU_SYNC:
                dropSync();
                return true;

            case R.id.menu_settings:
                Intent settingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(settingsActivity);
                return true;

            case R.id.rescanLibrary:
                startScan();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void bookSelected(int position)
    {
        Intent playbackActivity = new Intent(getBaseContext(),
                PlaybackActivity.class);
        Bundle extras = new Bundle();
        extras.putInt(C.EXTRA_BOOK, position);
        extras.putString(C.EXTRA_SOURCE, C.SOURCE_LIBRARY);
        playbackActivity.putExtras(extras);
        startActivity(playbackActivity);
    	//	overridePendingTransition(R.anim.pull_in_from_right, R.anim.hold);
    }

    void doBindService()
    {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        mIsBound = bindService(new Intent(LibraryActivity.this,
                PlaybackService.class), this, 0);
    }

    void doUnbindService()
    {
        if (mIsBound || mDropService != null)
        {
            // Detach our existing connection.
            unbindService(this);
            mIsBound = false;
        }
    }
}
