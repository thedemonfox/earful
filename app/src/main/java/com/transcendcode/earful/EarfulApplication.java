package com.transcendcode.earful;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class EarfulApplication extends Application
{
	public static final String TAG = "EarfulApplication";
	private final String data_filename = "earful_library.data";
	protected static final String PREFS_FILE = "cur_data.xml";

	private static final String CUR_BOOK = "earful_playing_book";

	// private static final String cur_data_filename = "earful_book_new.data";

	// Note that this is a really insecure way to do this, and you shouldn't
	// ship code which contains your key & secret in such an obvious way.
	// Obfuscation is good.
	final static private String APP_KEY = "d3ilkdv33xjwvej";
	final static private String APP_SECRET = "gpwygls22akga4u";

	// If you'd like to change the access type to the full Dropbox instead of
	// an app folder, change this value.
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

	// You don't need to change these, leave them alone.
	// final static private String ACCOUNT_PREFS_NAME = "prefs";
	final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	DropboxAPI<AndroidAuthSession> mDBApi;

	/** The audiobook library. */
	private BookLibrary mBookLibrary = null;

	// The cache for drop box syncing
	private final String cache_filename = "drop_cache.data";
	public Map<String, String> state = null;

	public SharedPreferences prefs;

	@Override
	public void onCreate()
	{
		super.onCreate();

		if (C.D)
			Log.i(TAG, "EarfulApplication onCreate");

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// We create a new AuthSession so that we can use the Dropbox API.
		AndroidAuthSession session = buildSession();
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		loadLibrary();
		loadCache();
	}

	public boolean pluggedIn()
	{
		// Register for the battery changed event
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

		// Intent is sticky so using null as receiver works fine
		// return value contains the status
		Intent batteryStatus = this.registerReceiver(null, filter);

		// Are we charging / charged?
		// int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS,
		// -1);
		// boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
		// || status == BatteryManager.BATTERY_STATUS_FULL;

		// boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;

		// How are we charging?
		int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

		if (acCharge || usbCharge)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	// Check weather Internet connection is available or not
	public int checkConnectionType()
	{
		final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetInfo = conMgr.getActiveNetworkInfo();
		if (activeNetInfo != null && activeNetInfo.isAvailable() && activeNetInfo.isConnected())
		{
			int type = activeNetInfo.getType();
			if (type == ConnectivityManager.TYPE_MOBILE || type == ConnectivityManager.TYPE_MOBILE_DUN
					|| type == ConnectivityManager.TYPE_MOBILE_HIPRI || type == ConnectivityManager.TYPE_MOBILE_MMS
					|| type == ConnectivityManager.TYPE_MOBILE_SUPL || type == ConnectivityManager.TYPE_WIMAX)
			{
				return ConnectivityManager.TYPE_MOBILE;
			}
			else if (type == ConnectivityManager.TYPE_WIFI)
			{
				return ConnectivityManager.TYPE_WIFI;
			}
			else
			{
				// Unknown connection type, so to be safe say mobile
				return ConnectivityManager.TYPE_MOBILE;
			}
		}
		else
		{
			// return not connected
			return -1;
		}
	}

	public void saveCache()
	{
		if (state != null)
		{
			if (C.Save)
				Log.i(TAG, "Saving cache file");

			FileOutputStream fos;
			try
			{
				fos = openFileOutput(cache_filename, Context.MODE_PRIVATE);
				ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(state);
				out.close();
			} catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	@SuppressWarnings("unchecked")
	public void loadCache()
	{
		// state = new HashMap<String, Integer>();
		FileInputStream fis = null;
		ObjectInputStream in = null;

		if (C.Save)
			Log.i(TAG, "Loading cache file: " + cache_filename);

		try
		{
			fis = new FileInputStream(getFilesDir().getAbsolutePath() + "/" + cache_filename);
			in = new ObjectInputStream(fis);
			state = (HashMap<String, String>) in.readObject();
		} catch (InvalidClassException ex)
		{
			if (C.D)
				Log.i(TAG, "InvalidClassException: File probably changed formats, can't use it.");

			File file = new File(getFilesDir().getAbsolutePath() + File.separator + data_filename);
			if (file.delete())
			{
				if (C.Save)
					Log.i(TAG, data_filename + " was deleted.");
			}
			else
			{
				if (C.D)
					Log.i(TAG, "Delete operation is failed.");
			}

			state = null;

			return;
		} catch (IOException ex)
		{
			if (C.D)
				Log.i(TAG, "IOException, probably file was not found");
			state = null;

			return;
		} catch (ClassNotFoundException ex)
		{
			state = null;

			if (C.D)
				Log.i(TAG, "ClassNotFoundException: File probably changed formats, can't use it.");

			return;
		} finally
		{
			IOUtils.closeQuietly(in);
		}

	}

	public void clearDropboxCache()
	{
		File file = new File(getFilesDir().getAbsolutePath() + File.separator + data_filename);
		if (file.delete())
		{
			if (C.D)
				Log.i(TAG, data_filename + " was deleted.");
		}
		else
		{
			if (C.D)
				Log.i(TAG, "Delete operation is failed.");
		}

		state = null;

	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a local store, rather than
	 * storing user name & password, and re-authenticating each time (which is not to be done, ever).
	 */
	void storeKeys(String key, String secret)
	{
		// Save the access key for later
		// SharedPreferences prefs =
		// getPreferenceScreen().getSharedPreferences();
		Editor edit = prefs.edit();
		edit.putString(ACCESS_KEY_NAME, key);
		edit.putString(ACCESS_SECRET_NAME, secret);
		edit.commit();
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a local store, rather than
	 * storing user name & password, and re-authenticating each time (which is not to be done, ever).
	 * 
	 * @return Array of [access_key, access_secret], or null if none stored
	 */
	private String[] getKeys()
	{
		// SharedPreferences prefs =
		// getPreferenceScreen().getSharedPreferences();
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null)
		{
			String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		}
		else
		{
			return null;
		}
	}

	private void clearKeys()
	{
		// SharedPreferences prefs =
		// getPreferenceScreen().getSharedPreferences();
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	void logOut()
	{
		// Remove credentials from the session
		mDBApi.getSession().unlink();

		// Clear our stored keys
		clearKeys();
	}

	private AndroidAuthSession buildSession()
	{
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session;

		String[] stored = getKeys();
		if (stored != null)
		{
			AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
		}
		else
		{
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
		}

		return session;
	}

	// Used to force the media scanner to update a directory if a .nomedia file
	// is added
	// or removed
	public void StartMediaScanner(String dir)
	{
		File file = new File(dir);
		new SingleScanner(this, file);
	}

	public void loadLibrary()
	{
		mBookLibrary = new BookLibrary();
		FileInputStream fis = null;
		ObjectInputStream in = null;

		if (C.D)
			Log.i(TAG, "Loading file: " + data_filename);

		try
		{
			fis = new FileInputStream(getFilesDir().getAbsolutePath() + "/" + data_filename);
			in = new ObjectInputStream(fis);
			mBookLibrary = (BookLibrary) in.readObject();
		} catch (InvalidClassException ex)
		{
			if (C.D)
				Log.i(TAG, "InvalidClassException: File probably changed formats, can't use it.");

			File file = new File(getFilesDir().getAbsolutePath() + File.separator + data_filename);
			if (file.delete())
			{
				if (C.D)
					Log.i(TAG, data_filename + " was deleted.");
			}
			else
			{
				if (C.D)
					Log.i(TAG, "Delete operation is failed.");
			}

			return;
		} catch (IOException ex)
		{
			if (C.D)
				Log.i(TAG, "IOException, probably file was not found");
			return;
		} catch (ClassNotFoundException ex)
		{
			if (C.D)
				Log.i(TAG, "ClassNotFoundException: File probably changed formats, can't use it.");
			return;
		} finally
		{
			IOUtils.closeQuietly(in);
		}

	}

	public void saveLibrary()
	{
		if (C.D)
			Log.i(TAG, "Saving file: " + data_filename);

		try
		{
			FileOutputStream fos = openFileOutput(data_filename, Context.MODE_PRIVATE);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(mBookLibrary);
			out.close();
			if (C.D)
				Log.i(TAG, "File save complete: " + data_filename);
		} catch (IOException e)
		{
			if (C.D)
				Log.i(TAG, "IOException");
		}

	}

	public void clearLibrary()
	{
		mBookLibrary.clearLibrary();

		File file = new File(getFilesDir().getAbsolutePath() + File.separator + data_filename);
		if (file.delete())
		{
			if (C.D)
				Log.i(TAG, data_filename + " was deleted.");
		}
		else
		{
			if (C.D)
				Log.i(TAG, "Delete operation is failed.");
		}

	}

	public BookLibrary getLibrary()
	{
		return mBookLibrary;
	}

	private final LoadingCache<Integer, BookState> cache = CacheBuilder.newBuilder().maximumSize(50)
			.build(new CacheLoader<Integer, BookState>() {
				@Override
				public BookState load(Integer cur_hash) throws Exception
				{
					String data_filename = "earful_" + String.format(Locale.US, "%X", cur_hash) + ".data";
					BookState book = new BookState();
					FileInputStream fis = null;
					ObjectInputStream in = null;

					if (C.Save)
						Log.i(TAG, "Loading file: " + data_filename);

					try
					{
						fis = new FileInputStream(getFilesDir().getAbsolutePath() + "/" + data_filename);
						in = new ObjectInputStream(fis);
						book = (BookState) in.readObject();
						return book;
					} catch (InvalidClassException ex)
					{
						// ex.printStackTrace();
						if (C.D)
							Log.i(TAG, "InvalidClassException: File probably changed formats, can't use it.");

						File file = new File(getFilesDir().getAbsolutePath() + File.separator + data_filename);
						if (file.delete())
						{
							if (C.D)
								Log.i(TAG, data_filename + " was deleted.");
						}
						else
						{
							if (C.D)
								Log.i(TAG, "Delete operation is failed.");
						}

						throw new StateNotFoundException("InvalidClassException");
					} catch (IOException ex)
					{
						// ex.printStackTrace();
						if (C.D)
							Log.i(TAG, "IOException, probably file was not found");
						throw new StateNotFoundException("file was not found");
					} catch (ClassNotFoundException ex)
					{
						// ex.printStackTrace();
						if (C.D)
							Log.i(TAG, "ClassNotFoundException: File probably changed formats, can't use it.");
						throw new StateNotFoundException("changed formats");
					} finally
					{
						IOUtils.closeQuietly(in);
					}
				}
			});

	void clearPlayingBook()
	{
		// We need an Editor object to make preference changes.
		SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(CUR_BOOK);

		// Commit the edits!
		editor.commit();
	}


	void savePlayingBook(int hash)
	{
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(CUR_BOOK, hash);

		// Commit the edits!
		editor.commit();
	}

	void saveState(final BookState playing_book)
	{
		final String data_filename = "earful_" + Utility.hashCodeString(playing_book.mBookHash) + ".data";
		// static final String data_filename = "earful_book.data";

		if (C.Save)
		{
			Log.i(TAG, "Saving file: " + data_filename + " track:" + playing_book.mCurrentTrack + " time:"
					+ playing_book.mCurrentTime + "secs");
		}

		// Invalidate cache (or in this case update it)
		cache.put(playing_book.hashCode(), playing_book);

		// Put writing file for current position in file in a thread to prevent
		// it from slowing down the service
		Thread t = new Thread(new Runnable() {
			public void run()
			{
				try
				{
					FileOutputStream fos = openFileOutput(data_filename, Context.MODE_PRIVATE);
					ObjectOutputStream out = new ObjectOutputStream(fos);
					out.writeObject(playing_book);
					out.close();

					if (C.Save)
						Log.i(TAG, "File save complete: " + data_filename);
				} catch (IOException e)
				{
					Log.e(TAG, "File save error");
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	BookState loadState(int hash)
	{
		BookState state = null;
		try
		{
			state = cache.get(hash);
			return state;
		} catch (ExecutionException e)
		{
			if (C.D)
				Log.i(TAG, "State not found, creating default");
			state = new BookState();
			state.mBookHash = hash;
			return state;
		}
	}

	BookState loadState()
	{
		if (C.Save)
			Log.i(TAG, "Looking for current hash");

		SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
		int cur_hash = settings.getInt(CUR_BOOK, 0);

		try
		{
			BookState state = cache.get(cur_hash);
			return state;
		} catch (ExecutionException e)
		{
			return null;
		}
	}

}
