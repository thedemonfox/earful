package com.transcendcode.earful;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class PlaybackActivity extends Activity implements ServiceConnection
{
	@Override
	public void onBackPressed()
	{
		Intent libraryActivity = new Intent(getBaseContext(), LibraryActivity.class);
		libraryActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(libraryActivity);

		
		// TODO Auto-generated method stub
//		super.onBackPressed();
	}

	public static final String TAG = "PlaybackActivity";
	PlayFragment mFrag = null;

	/* License stuff */
	DeviceUuidFactory mDeviceUuid = null;
	
	// Service connection
	private boolean mIsBound = false;
	PlaybackService mBoundService;

	public void onServiceConnected(ComponentName className, IBinder service)
	{
		if (C.D)
			Log.i(TAG, "onServiceConnected()");

		// This is called when the connection with the service has been
		// established, giving us the service object we can use to
		// interact with the service. Because we have bound to a explicit
		// service that we know is running in our own process, we can
		// cast its IBinder to a concrete class and directly access it.
		mBoundService = ((PlaybackService.LocalBinder) service).getService();

		mFrag = new PlayFragment();
		mFrag.setArguments(getIntent().getExtras());
		getFragmentManager().beginTransaction().add(android.R.id.content, mFrag).commit();

		/*
		 * if (mBoundService.getPlaybackMode() == PlayState.PLAYING || mBoundService.getPlaybackMode()
		 * == PlayState.PAUSED) { if (C.D) Log.i(TAG, "Playback service is playing a book: " +
		 * mBoundService.getPlaybackMode());
		 * 
		 * // mSelectedBook = mBoundService.getPlayingBook(); }
		 */

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
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(null);

		new Handler();
		getActionBar().setTitle("Playing");

		/*
		 * if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
		 * // If the screen is now in landscape mode, we can show the // dialog in-line with the list so
		 * we don't need this activity. finish(); return; }
		 */

		Intent svc = new Intent(PlaybackActivity.this, PlaybackService.class);
		startService(svc);

		doBindService();

		if (savedInstanceState == null)
		{
			if (C.D)
				Log.i(TAG, "savedInstanceState is null");
		}
		else
		{
			if (C.D)
				Log.i(TAG, "savedInstanceState is NOT null");
		}

		/*
		 * if (savedInstanceState == null) { // During initial setup, plug in the details fragment.
		 * mFrag = new PlayFragment(); mFrag.setArguments(getIntent().getExtras());
		 * getSupportFragmentManager().beginTransaction().add(android.R.id.content, mFrag).commit(); }
		 */

		mDeviceUuid = new DeviceUuidFactory(this);
	}

	@Override
	public void onDestroy()
	{
		doUnbindService();

		super.onDestroy();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.clear();

		getMenuInflater().inflate(R.menu.activity_playback, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.menu_settings:
				Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
				startActivity(settingsActivity);
				return true;
/*
			case R.id.menu_library:
				Intent libraryActivity = new Intent(getBaseContext(), LibraryActivity.class);
				libraryActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(libraryActivity);
				return true;
*/
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	void doBindService()
	{
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).

		bindService(new Intent(PlaybackActivity.this, PlaybackService.class), this, 0);
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
