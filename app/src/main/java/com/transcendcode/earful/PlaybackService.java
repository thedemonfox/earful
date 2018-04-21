/**
 * TODO
 * 
 * <h4>Description</h4> TODO
 * 
 * <h4>Notes</h4> TODO
 * 
 * <h4>References</h4> TODO
 * 
 * @author $Author$
 * 
 * @version $Rev$
 * 
 * @see TODO
 */

package com.transcendcode.earful;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.transcendcode.earful.ShakeEventListener.OnShakeListener;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.RemoteControlClient;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

public class PlaybackService extends Service implements OnSharedPreferenceChangeListener, OnShakeListener
{
	private static final int FORCE_THRESHOLD_1 = 150;
	private static final int FORCE_THRESHOLD_2 = 300;

	private static final String SLEEP_MODE = "CurSleepMode";

	private int mSleepTrack;
	private int mSleepTime;	
	
	/** The media player. */
	private MediaPlayer mMediaPlayer = new MediaPlayer();
	private AudioManager mAudioManager;

	public static final String TAG = "PlaybackService";

	public static final String CMDNAME = "command";
	public static final String ACTION_TOGGLE_PLAYBACK = "com.transcendcode.earful.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.transcendcode.earful.action.PLAY";
	public static final String ACTION_PAUSE = "com.transcendcode.earful.action.PAUSE";
	public static final String ACTION_STOP = "com.transcendcode.earful.action.STOP";
	public static final String ACTION_SKIP = "com.transcendcode.earful.action.SKIP";
	public static final String ACTION_REWIND = "com.transcendcode.earful.action.REWIND";

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private static final int NOTIFICATION = 4242;

	private static final int TONE_FX = 1;

	private Book mPlayingBook = null;
	private BookState mPlayingBookState = null;

	private HeadsetStateReceiver receiver = null;
	private Runnable mSavePositionTask = null;
	private Runnable mSleepTask = null;
//	private Runnable mAutoSleepTask = null;

	private Handler mHandler = new Handler();

	// our RemoteControlClient object, which will use remote control APIs available in
	// SDK level >= 14, if they're available.
	RemoteControlClient mRemoteControlClient;

	// The component name of MusicIntentReceiver, for use with media button and remote control
	// APIs
	ComponentName mMediaButtonReceiverComponent;

	// http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
	// private ComponentName mRemoteControlResponder;

	/* Variables for sleep mode */
	private SoundManager mSoundManager;
	private SensorManager mSensorManager;
	private ShakeEventListener mShakeListener;
	private float mVolumeStep = 1.0f;
	public SleepState mSleepState = SleepState.OFF;
	private boolean mBeepOnSleep = false;
	private int mTimeToSleep = 15;
	private int mTimeToFade = 5;
	private String mSleepShake;

	/* Variables for auto sleep mode */
	
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	static boolean mInitialState = true;
	private SharedPreferences prefs;
	boolean mBound = false;

	Time mStoppedTime = new Time();

	private final IBinder mBinder = new LocalBinder();

	EarfulApplication mAppState;

	private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
	private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";

	private void bluetoothNotifyChange(String what)
	{
		if (mPlayingBook != null)
		{
			Intent i = new Intent(what);
			i.putExtra("id", Long.valueOf(mMediaPlayer.getAudioSessionId()));
			i.putExtra("artist", mPlayingBook.mAuthor);
			i.putExtra("album", mPlayingBook.mTitle);
			i.putExtra("track", mPlayingBookState.mCurrentTrack);
			i.putExtra("playing", (mPlayingBookState.mMode == PlayState.PLAYING));
			i.putExtra("ListSize", mPlayingBook.getTotalTracks());
			i.putExtra("duration", mPlayingBook.getCurrentTrackTime(mPlayingBookState));
			i.putExtra("position", mPlayingBookState.mCurrentTime);
			sendBroadcast(i);

		}
	}

	public PlayState getPlaybackMode()
	{
		if (mPlayingBook != null)
			return mPlayingBookState.mMode;
		else
			return PlayState.NOBOOK;
	}

	private final class MyCompletionListener implements OnCompletionListener
	{
		public void onCompletion(MediaPlayer arg0)
		{
			if (mPlayingBook != null)
				nextTrack(true);
		}
	}

	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange)
		{
			if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
			{
				// Lower the volume
			}
			else if (focusChange == AudioManager.AUDIOFOCUS_GAIN)
			{
				// Raise it back to normal
			}
		}
	};

	/**
	 * Class for clients to access. Because we know this service always runs in the same process as
	 * its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder
	{
		PlaybackService getService()
		{
			return PlaybackService.this;
		}
	}

	public class HeadsetStateReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// http://stackoverflow.com/questions/6287116/android-registering-a-headset-button-click-with-broadcastreceiver
			if (Intent.ACTION_MEDIA_BUTTON.equals(intent))
			{
				if (mPlayingBookState.mMode == PlayState.PAUSED)
				{
					mMediaPlayer.start();
					mPlayingBookState.mMode = PlayState.PLAYING;

					Intent i = new Intent(C.EARFUL_EVENT);
					i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

					mAppState.saveState(mPlayingBookState);
				}
				else if (mPlayingBookState.mMode == PlayState.PLAYING)
				{
					mMediaPlayer.pause();
					mPlayingBookState.mMode = PlayState.PAUSED;

					Intent i = new Intent(C.EARFUL_EVENT);
					i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

					mAppState.saveState(mPlayingBookState);
				}
			}
			else
			{
				if (mInitialState)
				{
					mInitialState = false;
					if (C.D)
						Log.i(TAG, "mInitialState" + intent.getAction());
				}
				else
				{
					if (C.D)
						Log.i(TAG, "Broadcast Receiver " + intent.getAction());
					if (intent.getAction().compareTo(Intent.ACTION_HEADSET_PLUG) == 0)
					{
						// http://developer.android.com/reference/android/content/Intent.html#ACTION_HEADSET_PLUG
						if (intent.getIntExtra("state", 0) == 1) // Headphone plugged in
						{
							if (C.D)
								Log.i(TAG, "Headphone plugged in");

							// Be sure to check if we are playing a book and then check if it is paused before
							// restarting it
							if (mPlayingBook != null && mPlayingBookState.mMode == PlayState.PAUSED)
							{
								mMediaPlayer.start();
								mPlayingBookState.mMode = PlayState.PLAYING;

								Intent i = new Intent(C.EARFUL_EVENT);
								i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
								LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
								mAppState.saveState(mPlayingBookState);
							}
						}
						else
						// Headphone unplugged
						{
							if (C.D)
								Log.i(TAG, "Headphone unplugged");

							// Then pause the playback if the book was already playing

							if (mMediaPlayer.isPlaying())
							{
								mMediaPlayer.pause();
								mPlayingBookState.mMode = PlayState.PAUSED;

								Intent i = new Intent(C.EARFUL_EVENT);
								i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
								LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

								mAppState.saveState(mPlayingBookState);
							}
						}
					}
				}
			}
		}
	}


	OnErrorListener mErrorListener = new OnErrorListener() {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra)
		{
			if (C.D)
				Log.i(TAG, "Error during playing file what:" + what + " extra:" + extra);
			return false;
		}
	};

	OnInfoListener mInfoListener = new OnInfoListener() {

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra)
		{
			if (C.D)
				Log.i(TAG, "Info during playing file what:" + what + " extra:" + extra);
			return false;
		}
	};

	/**
	 * This is a wrapper around the new startForeground method, using the older APIs if it is not
	 * available.
	 */
	void startForegroundCompat(int id, Notification notification)
	{
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null)
		{
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try
			{
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e)
			{
				// Should not happen.
				Log.w("MyApp", "Unable to invoke startForeground", e);
			} catch (IllegalAccessException e)
			{
				// Should not happen.
				Log.w("MyApp", "Unable to invoke startForeground", e);
			}
			return;
		}

		// Fall back on the old API.
		// setForeground(true);
		// mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older APIs if it is not
	 * available.
	 */
	void stopForegroundCompat(int id)
	{
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null)
		{
			mStopForegroundArgs[0] = Boolean.TRUE;
			try
			{
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e)
			{
				// Should not happen.
				Log.w(TAG, "Unable to invoke stopForeground", e);
			} catch (IllegalAccessException e)
			{
				// Should not happen.
				Log.w(TAG, "Unable to invoke stopForeground", e);
			}
			return;
		}

		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		// mNM.cancel(id);
		// setForeground(false);
	}

	@Override
	public void onCreate()
	{
		if (C.D)
			Log.i(TAG, "Service onCreate");
		mAppState = (EarfulApplication) getApplication();

		IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		receiver = new HeadsetStateReceiver();
		this.registerReceiver(receiver, receiverFilter);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
		mMediaPlayer.setOnErrorListener(mErrorListener);
		mMediaPlayer.setOnInfoListener(mInfoListener);

		mSoundManager = new SoundManager(this);
		mSoundManager.create();
		mSoundManager.load(TONE_FX, R.raw.tone1);

		mShakeListener = new ShakeEventListener();

		mShakeListener.setOnShakeListener(this);

		// Load the prefs
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (prefs != null)
		{
			mBeepOnSleep = prefs.getBoolean("sleepBeep", false);
			mTimeToSleep = Integer.valueOf(prefs.getString("sleepTimePref", "15"));
			mSleepShake = prefs.getString("sleepGPref", "Small");
			mTimeToFade = Integer.valueOf(prefs.getString("fadeTimePref", "5"));

			if (mSleepShake.equals("Small"))
			{
				mShakeListener.setThreshold(FORCE_THRESHOLD_1);
			}
			else
			{
				mShakeListener.setThreshold(FORCE_THRESHOLD_2);
			}

			// Check the sleep mode last setting
			if (prefs.getBoolean(SLEEP_MODE, false))
			{
				StartSleep();
			}
		}
		else
		{
			if (C.D)
				Log.i(TAG, "Error Loading Preferences");
		}

		mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);

		if (C.D)
			Log.i(TAG, "Loading State");
		mPlayingBookState = mAppState.loadState();
		if (mPlayingBookState == null)
		{
			if (C.D)
				Log.i(TAG, "No current state found.");
		}
		else
		{
			mPlayingBook = mAppState.getLibrary().findBook(mPlayingBookState.mBookHash);
			if (C.D)
				Log.i(TAG, "Book found: " + mPlayingBook.mTitle);
			if (C.D)
				Log.i(TAG, "Current Track: " + mPlayingBookState.mCurrentTrack + " of " + mPlayingBook.getLastTrack());
			if (C.D)
				Log.i(
						TAG,
						"Current Time: " + mPlayingBookState.mCurrentTime + "secs of "
								+ mPlayingBook.getCurrentTrackTime(mPlayingBookState) + "secs");

			if (mPlayingBookState.mCurrentTime > mPlayingBook.getCurrentTrackTime(mPlayingBookState))
			{
				if (C.D)
					Log.i(TAG, "Saved time invalid resetting to 0");
				mPlayingBookState.mCurrentTime = 0;
			}

			if (C.D)
				Log.i(TAG, "Mode: " + mPlayingBookState.mMode);

			if (mPlayingBookState.mMode == PlayState.PLAYING)
			{
				playBook(true);
			}
			else if (mPlayingBookState.mMode == PlayState.PAUSED)
			{
				playBook(false);
			}
		}

		mSleepTask = new SleepTaskRunnable();

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		mStoppedTime.setToNow();
		// if (C.D)
		// Log.i(TAG, "Shutdown timer set to " + mStoppedTime.format2445());
/*
		mAutoSleepTask = new Runnable() {
			public void run()
			{
				if (prefs != null)
				{
					boolean sleep_auto_on = prefs.getBoolean("sleepOn", false);

					if (sleep_auto_on)
					{
						Calendar auto_sleep_time = TimePreference.getTimeFor(prefs, "sleepOnTime");
						if (Calendar.getInstance().after(auto_sleep_time))
						{
							if (C.D)
								Log.i(TAG, "Turning on sleep mode");
							StartSleep();
							return; // Exit this task
						}
					}
				}
				mHandler.postDelayed(this, 5 * 60 * 1000); // Run again after 30 secs
			}
		};
*/
		mSavePositionTask = new Runnable() {
			public void run()
			{
				if (mMediaPlayer != null && mMediaPlayer.isPlaying())
				{
					// convert from ms
					mPlayingBookState.mCurrentTime = mMediaPlayer.getCurrentPosition() / 1000;
					mAppState.saveState(mPlayingBookState);
				}

				Time now = new Time();
				now.setToNow();

				// If the mediaplayer is not playing, and it has been more than 5 minutes, AND no client is
				// bound to us let the service die

				if (!SafeIsActive() && now.toMillis(true) > mStoppedTime.toMillis(true) + 300 * 1000 && !mBound)
				{
					if (C.D)
						Log.i(TAG, "Shutting down playback service mBound:" + mBound);
					stopSelf();
				}

				mHandler.postDelayed(this, 30 * 1000); // Run again after 30 secs
			}
		};

		prefs.registerOnSharedPreferenceChangeListener(this);

		mHandler.postDelayed(mSavePositionTask, 1000);
//		mHandler.postDelayed(mAutoSleepTask, 1000);
		
		Intent i = new Intent(C.EARFUL_EVENT);
		i.putExtra("message", C.MSG_PLAYBACK_SERVICE_LOADED);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

	}

	public int getCurrentTrack()
	{
		if (mPlayingBook != null)
			return mPlayingBookState.mCurrentTrack;
		else
			return 0;
	}

	public int getCurrentTime()
	{
		if (SafeIsActive())
			return mMediaPlayer.getCurrentPosition() / 1000;
		else
			return 0;
	}

	public boolean SafeIsPlaying()
	{
		if (mMediaPlayer != null && mPlayingBook != null && mMediaPlayer.isPlaying()
				&& mPlayingBookState.mMode == PlayState.PLAYING)
			return true;
		else
			return false;
	}

	public boolean SafeIsActive()
	{
		if (mMediaPlayer != null && mPlayingBook != null
				&& (mPlayingBookState.mMode == PlayState.PLAYING || mPlayingBookState.mMode == PlayState.PAUSED))
			return true;
		else
			return false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{

		if (intent != null && C.D)
			Log.i(TAG, "onStartCommand = " + intent.getAction());
		if (intent != null)
		{
			String action = intent.getAction();
			if (action == null)
				return (START_STICKY);
			// if (SERVICECMD.equals(action))
			// {
			// String cmd = intent.getStringExtra(CMDNAME);
			if (action.equals(ACTION_TOGGLE_PLAYBACK))
				processTogglePlaybackRequest();
			// else if (action.equals(ACTION_PLAY)) play();
			else if (action.equals(ACTION_PAUSE))
				pause();
			else if (action.equals(ACTION_SKIP))
				nextTrack(true);
			else if (action.equals(ACTION_STOP))
				stop();
			else if (action.equals(ACTION_REWIND))
				prevTrack();
			// 
		}

		return (START_STICKY);
	}

	public void processTogglePlaybackRequest()
	{
		if (mPlayingBookState.mMode == PlayState.PAUSED)
		{
			mMediaPlayer.start();
			mPlayingBookState.mMode = PlayState.PLAYING;

			Intent i = new Intent(C.EARFUL_EVENT);
			i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

			mAppState.saveState(mPlayingBookState);
		}
		else if (mPlayingBookState.mMode == PlayState.PLAYING)
		{
			mMediaPlayer.pause();
			mPlayingBookState.mMode = PlayState.PAUSED;

			Intent i = new Intent(C.EARFUL_EVENT);
			i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

			mAppState.saveState(mPlayingBookState);
		}
	}

	@Override
	public void onDestroy()
	{
		if (C.D)
			Log.i(TAG, "Service onDestroy");

		prefs.unregisterOnSharedPreferenceChangeListener(this);
		mSensorManager.unregisterListener(mShakeListener);

		// Cancel the persistent notification.
		stopForegroundCompat(NOTIFICATION);
		mHandler.removeCallbacks(mSavePositionTask);

		if (mPlayingBook != null)
		{
			mAppState.saveState(mPlayingBookState);
		}

		if (mMediaPlayer != null)
		{
			mMediaPlayer.release();
		}

		// Remove the Intent.ACTION_HEADSET_PLUG receiver

		if (receiver != null)
		{
			unregisterReceiver(receiver);
		}

		// mAudioManager.unregisterMediaButtonEventReceiver(mRemoteControlResponder);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		mBound = true;
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		mBound = false;
		if (C.D)
			Log.i(TAG, "Last client unbound");
		return false;
	}

	public boolean playNewBook(int hash)
	{
		mPlayingBook = mAppState.getLibrary().findBook(hash);
		mPlayingBookState = mAppState.loadState(hash);
		mAppState.savePlayingBook(hash);
		return playBook(true);
	}

	private boolean playBook(boolean start_playing)
	{
		mMediaPlayer.reset();

		try
		{
			if (mPlayingBook.getCurrentTrackPath(mPlayingBookState) == null)
			{
				if (C.D)
					Log.i(TAG, "Failure to getCurrentTrackPath for playBook");
				mPlayingBookState.setCurrentTrack(mPlayingBook.getFirstTrack());
				mPlayingBookState.mCurrentTime = 0;
				mAppState.saveState(mPlayingBookState);
			}

			mMediaPlayer.setDataSource(mPlayingBook.getCurrentTrackPath(mPlayingBookState));
			mMediaPlayer.prepare();

			mMediaPlayer.setOnCompletionListener(new MyCompletionListener());


			// if (time_secs > 0)
			// {
			// mMediaPlayer.seekTo(time_secs * 1000); // convert from seconds to ms
			// }

			mMediaPlayer.seekTo(mPlayingBookState.mCurrentTime * 1000);

			if (start_playing)
			{
				mMediaPlayer.start();
				mPlayingBookState.mMode = PlayState.PLAYING;
			}

			Intent i = new Intent(C.EARFUL_EVENT);
			i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

			mAppState.saveState(mPlayingBookState);

			showNotification();

			bluetoothNotifyChange(AVRCP_META_CHANGED);

			MediaButtonHelper.registerMediaButtonEventReceiverCompat(mAudioManager, mMediaButtonReceiverComponent);

			// Use the remote control APIs to set the playback state

			if (mRemoteControlClient == null)
			{
				Intent mintent = new Intent(Intent.ACTION_MEDIA_BUTTON);
				mintent.setComponent(mMediaButtonReceiverComponent);
				mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(this, 0, mintent, 0));
				mAudioManager.registerRemoteControlClient(mRemoteControlClient);
			}

			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);

			mRemoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY
					| RemoteControlClient.FLAG_KEY_MEDIA_PAUSE | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
					| RemoteControlClient.FLAG_KEY_MEDIA_STOP);

			// Update the remote controls
			mRemoteControlClient
					.editMetadata(true)
					.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, mPlayingBook.mAuthor)
					.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, mPlayingBook.mTitle)
					.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, String.valueOf(mPlayingBookState.mCurrentTrack))
					.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, mPlayingBookState.mCurrentTime)
					.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK,
							BitmapFactory.decodeFile(mPlayingBook.mCoverPath)).apply();

			return true;

		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mPlayingBookState.mMode = PlayState.STOPPED;
		return false;

	}

	public void start()
	{
		if (C.D)
			Log.i(TAG, "start()");

		mVolumeStep = 1.0f;
		mMediaPlayer.setVolume(mVolumeStep, mVolumeStep);

		mMediaPlayer.start();
		mPlayingBookState.mMode = PlayState.PLAYING;
	}

	/*
	 * public void selectBook(Book book) { this.mPlayingBook = book; }
	 */
	public void pause()
	{
		if (mMediaPlayer.isPlaying())
		{
			mMediaPlayer.pause();
			mPlayingBookState.mMode = PlayState.PAUSED;
			mAppState.saveState(mPlayingBookState);

			// Tell any remote controls that our playback state is 'playing'.
			if (mRemoteControlClient != null)
			{
				mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
			}

			if (C.D)
				Log.i(TAG, "Paused");
		}
		else
		{
			if (C.D)
				Log.i(TAG, "Can't pause, no book playing");
		}
	}

	public void shutdown()
	{
		stopSelf();
	}

	public void stop()
	{
		if (mPlayingBook != null)
		{
			mMediaPlayer.stop();
			mPlayingBookState.mMode = PlayState.STOPPED;

			mAppState.clearPlayingBook();

			mPlayingBook = null;



			mStoppedTime.setToNow();

			if (C.D)
				Log.i(TAG, "Stopped at time " + mStoppedTime.format2445());

			stopForegroundCompat(NOTIFICATION);

			// Tell any remote controls that our playback state is 'stopped'.
			if (mRemoteControlClient != null)
			{
				mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
			}
		}
		else
		{
			if (C.D)
				Log.i(TAG, "Can't stop book, no book playing");
		}
	}

	/*
	 * public int getDuration() { if (mMediaPlayer.isPlaying()) { // convert from ms to seconds return
	 * mMediaPlayer.getDuration() / 1000; } else if (mPlayingBook != null) { // Log.i(TAG,
	 * "returning mPlayingBook.getCurrentTime() " + mPlayingBook.getCurrentTime()); return
	 * mPlayingBook.mCurrentTime; } else { if (C.D) Log.i(TAG, "getDuration():mPlayingBook is null");
	 * return 0; } // return mMediaPlayer.getDuration(); }
	 */
	public int getCurrentPosition()
	{
		return mMediaPlayer.getCurrentPosition() / 1000; // convert ms to secs
	}

	public void seekTo(int pos)
	{
		mMediaPlayer.seekTo(pos * 1000); // convert from secs to ms
	}

	public void nextTrack(boolean startPlaying)
	{
		if (C.D)
			Log.i(TAG, "Next Track");

		if (mPlayingBook.tracks.higherKey(mPlayingBookState.mCurrentTrack) != null)
		{
			int next_track = mPlayingBook.tracks.higherKey(mPlayingBookState.mCurrentTrack);
			if (next_track <= mPlayingBook.getLastTrack())
			{
				mPlayingBookState.setCurrentTrack(next_track);
				mPlayingBookState.mCurrentTime = 0;
				mAppState.saveState(mPlayingBookState);

				if (startPlaying || mMediaPlayer.isPlaying())
				{
					playBook(true);
				}
			}
			else
			{
				if (C.D)
					Log.i(TAG, "No More Tracks");
				stop();
				// mPlayingBook.mMode = PlayState.PAUSED;
			}
		}
		else
		{
			if (C.D)
				Log.i(TAG, "No More Tracks");
			stop();

		}

	}

	public void selectTrack(int track)
	{
		if (C.D)
			Log.i(TAG, "selectTrack: " + track);
		mPlayingBookState.setCurrentTrack(track);
		mPlayingBookState.mCurrentTime = 0;
		mAppState.saveState(mPlayingBookState);

		if (mMediaPlayer.isPlaying())
		{
			playBook(true);
		}
	}

	public void prevTrack()
	{
		if (C.D)
			Log.i(TAG, "prev Track");

		int prev_track = mPlayingBook.tracks.lowerKey(mPlayingBookState.mCurrentTrack);
		if (prev_track >= mPlayingBook.getFirstTrack())
		{
			mPlayingBookState.setCurrentTrack(prev_track);
			mPlayingBookState.mCurrentTime = 0;
			mAppState.saveState(mPlayingBookState);

			if (mMediaPlayer.isPlaying())
			{
				playBook(true);
			}
		}
	}

	public int getPlayingBook()
	{
		if (mPlayingBook != null)
			return mPlayingBook.hashCode();
		else
			return 0;
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification()
	{
		if (C.D)
			Log.i(TAG, "showNotification()");

		// In this sample, we'll use the same text for the ticker and the expanded notification
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_notify).setContentTitle("Playing")
						.setOnlyAlertOnce(true).setOngoing(true).setContentText(mPlayingBook.mTitle);

		if (mPlayingBook != null)
		{
			mBuilder.setContentText("Playing " + mPlayingBook.mTitle + " " + mPlayingBookState.mCurrentTrack + "-"
					+ mPlayingBook.getLastTrack());
		}
		else
		{
			mBuilder.setContentText("No Book Selected");
		}

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, PlaybackActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		resultIntent.putExtra(C.EXTRA_SOURCE, C.SOURCE_NOTIFICATION);
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		android.support.v4.app.TaskStackBuilder stackBuilder = android.support.v4.app.TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(PlaybackActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		// NotificationManager mNotificationManager = (NotificationManager)
		// getSystemService(Context.NOTIFICATION_SERVICE);
		// mNotificationManager.notify(NOTIFICATION, mBuilder.build());
		startForeground(NOTIFICATION, mBuilder.build());
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (C.D)
			Log.i(TAG, "Service onSharedPreferenceChanged, key=" + key);

		mBeepOnSleep = prefs.getBoolean("sleepBeep", false);
		mTimeToSleep = Integer.valueOf(prefs.getString("sleepTimePref", "15"));
		mSleepShake = prefs.getString("sleepGPref", "Small");
		mTimeToFade = Integer.valueOf(prefs.getString("fadeTimePref", "5"));

		if (mSleepShake.equals("Small"))
		{
			mShakeListener.setThreshold(FORCE_THRESHOLD_1);
		}
		else
		{
			mShakeListener.setThreshold(FORCE_THRESHOLD_2);
		}

	};

	private final class SleepTaskRunnable implements Runnable
	{
		public void run()
		{

			// Check if we are in the waiting state
			if (mSleepState == SleepState.WAITING)
			{
				if (C.D)
					Log.i(TAG, "Time to start the sleep cycle");

				// If so, play the beep (if configured to do so)
				if (mBeepOnSleep)
				{
					mSoundManager.play(TONE_FX);
				}

				// and start the sleep muting of the audiobook
				mSleepState = SleepState.HUSHING;
				mHandler.postDelayed(this, 1000); // Run again after a second to do another mute step
				mSleepTrack = mPlayingBookState.mCurrentTrack;
				mSleepTime = mPlayingBookState.mCurrentTime;
			}
			else if (mSleepState == SleepState.HUSHING)
			{
				float hush_step = 1.0f / (mTimeToFade * 60.0f);
				mVolumeStep -= hush_step;

				if (mVolumeStep <= 0.0f)
				{
					if (C.D)
						Log.i(TAG, "Audiobook muted, time to sleep");

					mSleepState = SleepState.SLEEP;

					// This is the final state, audio should now be muted. Now we pause the book, and stay at
					// this state
					// with no further work to be done.
					mPlayingBookState.mCurrentTrack = mSleepTrack;
					mPlayingBookState.mCurrentTime = mSleepTime;

					pause();

					Intent i = new Intent(C.EARFUL_EVENT);
					i.putExtra("message", C.MSG_PLAYBACK_CHANGED);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
				}
				else
				{
					mMediaPlayer.setVolume(mVolumeStep, mVolumeStep);

					if (C.D)
						Log.i(TAG, "Volume attenuated to " + mVolumeStep);

					mHandler.postDelayed(this, 1000);
				}

			}
			else
			{
				if (C.D)
					Log.i(TAG, "This shouldn't happen: " + mSleepState);
			}

		}
	}

	public void StopSleep()
	{
		if (C.D)
			Log.i(TAG, "StopSleep()");

		mSleepState = SleepState.OFF;

		// Save the sleep state
		SharedPreferences.Editor editor = mAppState.prefs.edit();
		editor.putBoolean(SLEEP_MODE, false);
		editor.commit();

		mMediaPlayer.setVolume(1.0f, 1.0f);

		mSensorManager.unregisterListener(mShakeListener);
	}

	private void initSleep()
	{
		mHandler.removeCallbacks(mSleepTask);

		prefs = getSharedPreferences(SettingsActivity.KEY, Activity.MODE_PRIVATE);
		mTimeToSleep = prefs.getInt("sleepTimePref", 15);

		if (C.D)
			Log.i(TAG, "Sleep mode is enabled so starting a time for " + mTimeToSleep + " mins to fade out");

		mSleepState = SleepState.WAITING;
		mVolumeStep = 1.0f;
		mMediaPlayer.setVolume(mVolumeStep, mVolumeStep);
		mHandler.postDelayed(mSleepTask, mTimeToSleep * 60 * 1000);
	}

	public boolean StartSleep()
	{
		if (C.D)
			Log.i(TAG, "StartSleep()");

		// Save the sleep state
		SharedPreferences.Editor editor = mAppState.prefs.edit();
		editor.putBoolean(SLEEP_MODE, true);
		editor.commit();

		mSoundManager.play(TONE_FX);

		// If sleep mode is enabled, then set a timer to change the mode
		if (SafeIsActive())
		{
			// mSleepOn = true;

			mSensorManager.registerListener(mShakeListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);

			initSleep();

			return true;
		}
		else
		{
			return false;
		}

	}

	@Override
	public void onShake()
	{
		if (C.D)
			Log.i(TAG, "Shake!");

		if (SleepState.isOn(mSleepState))
		{
			initSleep();
		}
	}

}
