package com.transcendcode.earful;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

public class DropSyncService extends IntentService
{

	private static final String TAG = "DropSyncService";
	EarfulApplication mAppState = null;
	private SharedPreferences mPrefs;

	public long mSyncTotalBytesDownloaded;
	public int mSyncTotalFilesDownloaded;
	public int mSyncFileDeleted;

	volatile public long mSyncFileBytesDownloaded;
	volatile public long mSyncFileSize;

	public int mFileTotal = 0;

	public Time mSyncStartTime = new Time();
	public String mSyncCurrentFile = null;
	public int mSyncDownloadSpeed;
	public boolean mRunning = true;
	boolean mBound = false;

	long downloadStartTime;

	private static final int NOTIFICATION = 4243;

	private static final int MAX_ENTRIES = 10000;

	private final IBinder mBinder = new LocalBinder();

	public DropSyncService()
	{
		super("Earful DropSync Service");
	}

	/**
	 * Class for clients to access. Because we know this service always runs in the same process as
	 * its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder
	{
		DropSyncService getService()
		{
			return DropSyncService.this;
		}
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
		if (C.Drop)
			Log.i(TAG, "Last client unbound");
		return false;
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		mAppState = ((EarfulApplication) getApplicationContext());
	}

	public float getDownloadSpeed()
	{
		return 0.0f;
		/*
		 * long elapsedTime = System.currentTimeMillis() - downloadStartTime; return 1000f *
		 * mTotalDownloaded / elapsedTime;
		 */
	}

	boolean copyFile(Entry source, File destination) throws IOException
	{
		if (C.Drop)
			Log.i(TAG, "Copy " + source.path + " to " + destination.getCanonicalPath());

		FileOutputStream outputStream = null;
		try
		{
			outputStream = new FileOutputStream(destination);

			// Update the variables that are used outside this service for tracking of what is going on
			mSyncCurrentFile = source.fileName();
			mSyncFileBytesDownloaded = 0L;
			downloadStartTime = System.currentTimeMillis();

			DropboxFileInfo info = mAppState.mDBApi.getFile(source.path, null, outputStream, new ProgressListener() {
				@Override
				public void onProgress(long bytes, long total)
				{
					mSyncFileSize = total;
					mSyncFileBytesDownloaded = bytes;

					// double percent_done = ((double) mSyncFileBytesDownloaded / mSyncFileSize);

					// Log.i(TAG, "bytes " + mSyncFileBytesDownloaded + "   of total " + mSyncFileSize +
					// " percent_done"
					// + percent_done
					// + "  so " + (int) (percent_done * 1000.0));
				}
			});
			mSyncTotalBytesDownloaded += mSyncFileBytesDownloaded;
			if (C.Drop)
				Log.i(TAG, "The file's rev is: " + info.getMetadata().rev);
		} catch (DropboxException e)
		{
			Log.e(TAG, "Something went wrong while downloading.");
			if (outputStream != null)
				outputStream.close();
			destination.delete();
			return false;
		} catch (FileNotFoundException e)
		{
			Log.e(TAG, "File not found.");
			return false;
		} finally
		{
			if (outputStream != null)
			{
				try
				{
					outputStream.close();
				} catch (IOException e)
				{}
			}
		}

		return true;

	}

	public void synchronize(Entry source, File destination, List<String> keys_used) throws IOException, DropboxException
	{
		if (C.Drop)
			Log.i(TAG, "synchronize(" + source.path + "," + destination.getCanonicalPath() + ")");

		if (source.isDir)
		{
			if (!destination.exists())
			{
				if (!destination.mkdirs())
				{
					throw new IOException("Could not create path " + destination);
				}
				else
				{
					if (C.Drop)
						Log.i(TAG, destination.getCanonicalPath() + " was created.");
				}
			}
			else if (!destination.isDirectory())
			{
				throw new IOException("Source and Destination not of the same type:" + source.fileName() + " , "
						+ destination.getCanonicalPath());
			}

			Entry dir_entry = mAppState.mDBApi.metadata(source.path, MAX_ENTRIES, null, true, null);
			List<Entry> src_contents = dir_entry.contents;

			Set<String> srcNames = new HashSet<String>();

			for (Entry e : src_contents)
			{
				srcNames.add(e.fileName());
			}

			String[] dests = destination.list();

			// delete files not present in source
			for (String fileName : dests)
			{
				if (!srcNames.contains(fileName))
				{
					if (C.Drop)
						Log.i(TAG, "Delete " + destination.getCanonicalPath() + " name " + fileName);
					File tbd = new File(destination, fileName);

					// Have to recursively delete entire contents of directory
					if (tbd.isDirectory())
					{
						if (!Utility.DeleteRecursive(tbd))
						{
							if (C.Drop)
								Log.i(TAG, "Error recursive deleting: " + tbd.getAbsolutePath());
						}
					}
					else if (!tbd.delete())
					{
						if (C.Drop)
							Log.i(TAG, "Error deleting: " + tbd.getAbsolutePath());
					}

					mSyncFileDeleted++;
				}
			}

			// copy each file from source
			for (Entry fileName : src_contents)
			{
				File destFile = new File(destination, fileName.fileName());
				synchronize(fileName, destFile, keys_used);
			}
		}
		else
		{
			// State variable to check if an abort was commanded
			if (!mRunning)
			{
				return;
			}

			if (destination.exists() && destination.isDirectory())
			{
				if (C.Drop)
					Log.i(TAG, "Delete " + destination.getCanonicalPath());

				mAppState.state.remove(destination.getAbsolutePath());
				destination.delete();

				mSyncFileDeleted++;
			}
			if (destination.exists())
			{

				// Use the hashmap to check the state of the synced directory
				String destHashCode = mAppState.state.get(source.path);

				if (destHashCode == null)
				{
					if (C.Drop)
						Log.i(TAG, "File not found in cache: " + source.path);
				}
				else if (!source.rev.equals(destHashCode))
				{
					if (C.Drop)
					{
						Log.i(TAG, "hash code for " + source.path + " doesn't match");
						Log.i(TAG, "local : " + destHashCode);
						Log.i(TAG, "remote: " + source.rev);
					}
				}

				if (destHashCode == null || !source.rev.equals(destHashCode))
				{
					// if (C.D)
					// Log.i(TAG, "Hash code doesn't match so downloading");

					// showNotification("Downloading " + source.fileName());
					// mSyncCurrentFile = source.fileName();

					keys_used.add(source.path);
					if (copyFile(source, destination))
					{
						if (C.Drop)
							Log.i(TAG, "Adding " + source.path + " with code " + source.rev + " to cache");

						mAppState.state.put(source.path, source.rev);
						mAppState.saveCache();
					}
					else
					{
						// Make sure this isn't in the cache
						mAppState.state.remove(source.path);
					}

					mSyncTotalFilesDownloaded++;
				}
				else
				{
					// Be sure to add skipped files to the list of keys found
					// else they would be deleted and re-downloaded next pass

					keys_used.add(source.path);
				}

			}
			else
			{
				// showNotification("Downloading " + source.fileName());
				// mSyncCurrentFile = source.fileName();

				keys_used.add(source.path);

				if (copyFile(source, destination))
				{
					if (C.Drop)
						Log.i(TAG, "Adding " + source.path + " with code " + source.rev + " to cache");

					mAppState.state.put(source.path, source.hash);
					mAppState.saveCache();
				}
				else
				{
					// Make sure this isn't in the cache
					mAppState.state.remove(source.path);
				}
				mSyncTotalFilesDownloaded++;
			}
		}
	}

	@Override
	public void onHandleIntent(Intent intent)
	{
		if (C.Drop)
			Log.i(TAG, "DropSyncService invoked");

		// DateTime start = new DateTime();

		showNotification("Earful Dropbox Sync");

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = mPrefs.edit();
		// editor.putString("sync_start_time", start.toString());
		// editor.commit();

		List<String> keys_used = new ArrayList<String>();

		if (mAppState.state == null)
		{
			mAppState.state = new HashMap<String, String>();
			if (C.Drop)
				Log.i(TAG, "New HashMap Created");
		}

		String dropDirectory = mPrefs.getString("dropDirectoryPref", "");

		if (dropDirectory.equals(""))
		{
			if (C.Drop)
				Log.i(TAG, "No drop directory is defined, aborting sync");
			return;
		}
		try
		{
			if (mAppState.mDBApi.getSession().isLinked())
			{
				if (C.Drop)
					Log.i(TAG, "Retrieving Dropbox file list");

				// Used for display of downloaded details
				mSyncTotalFilesDownloaded = 0;
				mSyncFileDeleted = 0;

				mSyncStartTime.setToNow();

				// String destHashCode = mAppState.state.get("/");
				Entry rootEntry = mAppState.mDBApi.metadata("/", MAX_ENTRIES, null, false, null);
				File destDir = new File(dropDirectory);
				try
				{
					synchronize(rootEntry, destDir, keys_used);
					// mAppState.state.put("/", rootEntry.hash);
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			else
			{
				if (C.Drop)
					Log.i(TAG, "Dropbox not linked");
			}
		} catch (DropboxException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// DateTime end = new DateTime();
		// editor.putString("sync_completed_time", end.toString());
		editor.putInt("sync_downloaded", mSyncTotalFilesDownloaded);
		editor.putInt("sync_deleted", mSyncFileDeleted);

		editor.commit();

		stopForeground(true);

		// Skip the pruning if the download has been aborted. The reason is that we haven't looked at
		// the keys
		// so we will probably end up pruning ones that we would have seen had the scan completed its
		// full
		// lifecycle.
		if (mRunning)
		{
			Map<String, String> new_map = new HashMap<String, String>();
			for (String s : keys_used)
			{
				if (C.Drop)
					Log.i(TAG, "Adding: " + s);
				String val = mAppState.state.get(s);
				new_map.put(s, val);
			}
			mAppState.state.clear();

			if (C.Drop)
				Log.i(TAG, "Number of values pruned: " + (new_map.size() - mAppState.state.size()));

			mAppState.state = new_map;
			mAppState.saveCache();
		}
		else
		{
			if (C.Drop)
				Log.i(TAG, "Aborted so skipped pruning");
		}

		// Intent i = new Intent("com.transcendcode.earful.DROPBOX_SYNC_COMPLETED");
		// sendBroadcast(i);

		if (C.Drop)
			Log.i(TAG, "Broadcasting message: " + C.MSG_DROPBOX_SYNC_COMPLETED);
		Intent i = new Intent(C.EARFUL_EVENT);
		i.putExtra("message", C.MSG_DROPBOX_SYNC_COMPLETED);
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);

	}

	private void showNotification(String title)
	{
		if (C.D)
			Log.i(TAG, "showNotification()");

		// In this sample, we'll use the same text for the ticker and the expanded notification
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this).setSmallIcon(R.drawable.stat_notify_sync).setContentTitle("Dropbox Sync")
						.setOnlyAlertOnce(true).setOngoing(true).setContentText(title);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, LibraryActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		resultIntent.putExtra(C.EXTRA_SOURCE, C.SOURCE_NOTIFICATION);
		
		// The stack builder object will contain an artificial back stack for the started Activity.
		// This ensures that navigating backward from the Activity leads out of your application to the
		// Home screen.
		android.support.v4.app.TaskStackBuilder stackBuilder = android.support.v4.app.TaskStackBuilder.create(this);
		
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(LibraryActivity.class);
		
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(resultPendingIntent);
		startForeground(NOTIFICATION, mBuilder.build());
	}
}
