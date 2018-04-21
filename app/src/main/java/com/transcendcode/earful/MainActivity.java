/**
 * TODO
 *
 * <h4>Description</h4>
 * TODO
 *
 * <h4>Notes</h4>
 * TODO
 *
 * <h4>References</h4>
 * TODO
 *
 * @author      $Author$
 *
 * @version     $Rev$
 *
 * @see         TODO
 */

package com.transcendcode.earful;

import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

// TODO: Auto-generated Javadoc
/**
 * The Class MainActivity.
 */
public class MainActivity extends Activity implements ServiceConnection
{

    /** The context. */
    private static Context context;

    public static final String TAG = "MainActivity";

    private boolean mIsBound = false;
    PlaybackService mBoundService;

    static boolean mLicenced = true;

    // Appplication state
    EarfulApplication mAppState;

    // Help overlay variables
    static final String SHOW_ACTIVITY_OVERLAY = "MainActivity_Help";
    private boolean mShowHelpOverlay = true;

    public void onServiceConnected(ComponentName className, IBinder service)
    {
        mBoundService = ((PlaybackService.LocalBinder) service).getService();

        // Don't jump into another activity if we are showing the help overlay
        if (!mShowHelpOverlay)
        {
            // Wait until the service is bound, and then either start the playback activity if a book is playing,
            // or start the library activity if a book is not playing.
            if (mBoundService.getPlaybackMode() == PlayState.PLAYING ||
                    mBoundService.getPlaybackMode() == PlayState.PAUSED)
            {
                if (C.D)
                    Log.i(TAG, "Playback service is playing a book: " + mBoundService.getPlaybackMode());

                //  Intent playbackActivity = new Intent(getBaseContext(), PlaybackActivity.class);
                //  startActivity(playbackActivity);

                // Start the library with a note to the library to start the playback activity. This way
                // the library is on the stack if the user hits the back key.
                Intent libraryActivity = new Intent(getBaseContext(), LibraryActivity.class);
                // libraryActivity.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle extras = new Bundle();
                extras.putBoolean(C.EXTRA_START_PLAY, true);
                libraryActivity.putExtras(extras);
                startActivity(libraryActivity);
            }
            else
            {
                Intent libraryActivity = new Intent(getBaseContext(), LibraryActivity.class);
                // libraryActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(libraryActivity);
            }
        }
        //  finish();
    }

    public void onServiceDisconnected(ComponentName className)
    {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        // Because it is running in our same process, we should never
        // see this happen.
        if (C.D)
            Log.i(TAG, "Bound service is now disconnected");

        mBoundService = null;
        mIsBound = false;
    }

    @Override
    public void onResume()
    {
        if (C.D)
            Log.i(TAG, "onResume - starting update thread");

        //  if (!myReceiverIsRegistered)
        // {
        //     registerReceiver(myReceiver, new IntentFilter(C.MSG_DROPBOX_SYNC_COMPLETED));
        // registerReceiver(myReceiver, new IntentFilter(C.MSG_PLAYBACK_CHANGED));
        // registerReceiver(myReceiver, new IntentFilter(C.MSG_PLAYBACK_SERVICE_LOADED));
        //      myReceiverIsRegistered = true;
        //   }

        super.onResume();
    }

    @Override
    public void onPause()
    {
        if (C.D)
            Log.i(TAG, "onPause - killing update thread");

        //   if (myReceiverIsRegistered)
        //  {
        //       unregisterReceiver(myReceiver);
        //      myReceiverIsRegistered = false;
        //   }

        super.onPause();
    }

    /**
     * Gets the app context.
     * 
     * @return the app context
     */
    public static Context getAppContext()
    {
        return MainActivity.context;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        MainActivity.context = this;
        mAppState = ((EarfulApplication) getApplicationContext());
        

   
        // TODO: FIX!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 

        if (C.D)
            Log.i(TAG, "MainActivity onCreate");

        setContentView(R.layout.loading);

        // Show help overlay if it has never been closed by the user before
        mShowHelpOverlay = mAppState.prefs.getBoolean(SHOW_ACTIVITY_OVERLAY, true);
        LinearLayout overlay_view = (LinearLayout) findViewById(R.id.helpOverlay);
        if (!mShowHelpOverlay)
        {
            overlay_view.setVisibility(View.INVISIBLE);
        }
        else
        {
            overlay_view.setVisibility(View.VISIBLE);
        }

        // Allow user to remove help overlay by clicking it
        final ImageView overlay_img = (ImageView) findViewById(R.id.overlay_img);
        overlay_img.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                LinearLayout overlay_view = (LinearLayout) findViewById(R.id.helpOverlay);
                overlay_view.setVisibility(View.INVISIBLE);
                mShowHelpOverlay = false;

                // Save the help shown state
                SharedPreferences.Editor editor = mAppState.prefs.edit();
                editor.putBoolean(SHOW_ACTIVITY_OVERLAY, mShowHelpOverlay);
                editor.commit();

                Intent libraryActivity = new Intent(getBaseContext(), LibraryActivity.class);
                startActivity(libraryActivity);
            }
        });

        // myReceiver = new ReceiveMessages();

    }

    @Override
    public void onStart()
    {
        if (C.D)
            Log.i(TAG, "MainActivity OnStart");

        //  mUpdateTimeTask = new ScreenUpdate();
        //  mUpdateHandler.postDelayed(mUpdateTimeTask, 10);

        Intent svc = new Intent(MainActivity.this, PlaybackService.class);
        startService(svc);

        doBindService();
        super.onStart();
    }

    @Override
    public void onStop()
    {
        if (C.D)
            Log.i(TAG, "MainActivity OnStop");

        //mUpdateHandler.removeCallbacks(mUpdateTimeTask);
        // mUpdateTimeTask = null;

        doUnbindService();
        // mChecker.onDestroy();

        super.onStop();
    }

    void doBindService()
    {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(MainActivity.this,
                PlaybackService.class), this, 0);
        mIsBound = true;
    }

    void doUnbindService()
    {
        if (mIsBound)
        {
            // Detach our existing connection.
            unbindService(this);
            mIsBound = false;
        }
    }

}
