package com.transcendcode.earful;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

public final class Alarm extends BroadcastReceiver
{
	public static final String TAG = "Alarm";
	public static final String ALARM_TIME = "NextAlarmTime";
	public static final int MINS2MS = (60 * 1000);

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		// PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
		// wl.acquire();
		boolean connectionGood = false;
		boolean powerGood = false;

		if (C.D)
			Log.i(TAG, "Alarm !!!!!!!!!!");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String connectionPref = prefs.getString("internetConnectionPref", "wifi");

		int currentConnect = ((EarfulApplication) context.getApplicationContext()).checkConnectionType();
		if (connectionPref.equals("wifi") && currentConnect == ConnectivityManager.TYPE_WIFI)
		{
			if (C.D)
				Log.i(TAG, "WiFi is required to start syncing and we are connected to it");

			connectionGood = true;
		}
		else if (connectionPref.equals("cell")
				&& (currentConnect == ConnectivityManager.TYPE_WIFI || currentConnect == ConnectivityManager.TYPE_MOBILE))
		{
			if (C.D)
				Log.i(TAG, "Any data connection is required to start syncing and we got it");

			connectionGood = true;
		}

		String powerPref = prefs.getString("powerSourcePref", "ac");

		// Are we charging / charged?
		// IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		// Intent batteryStatus = context.registerReceiver(null, ifilter);
		// int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

		boolean isCharging = ((EarfulApplication) context.getApplicationContext()).pluggedIn();

		// ((status == BatteryManager.BATTERY_STATUS_CHARGING) ||
		// (status == BatteryManager.BATTERY_STATUS_FULL));

		if (powerPref.equals("ac") && isCharging)
		{
			if (C.D)
				Log.i(TAG, "AC is required and we are plugged in");

			powerGood = true;
		}
		else if (powerPref.equals("battery"))
		{
			if (C.D)
				Log.i(TAG, "Battery is required and obviously we are fine there");

			powerGood = true;
		}
		// How are we charging?
		/*
		 * int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1); boolean
		 * usbCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_USB); boolean acCharge =
		 * (chargePlug == BatteryManager.BATTERY_PLUGGED_AC);
		 */

		if (connectionGood && powerGood)
		{
			if (C.D)
				Log.i(TAG, "Connection and Power is good, starting sync");

			Intent svc = new Intent(context, DropSyncService.class);
			context.startService(svc);
		}

		int time = Integer.parseInt(prefs.getString("syncTimePref", "0"));
		updateNextAlarmTime(context, time);

		// wl.release();
	}

	public static boolean isAlarmSet(Context context)
	{
		Intent i = new Intent(context, Alarm.class);
		boolean alarmUp = (PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_NO_CREATE) != null);

		return alarmUp;
	}

	private static void updateNextAlarmTime(Context context, int timeInMins)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		edit.putLong(ALARM_TIME, System.currentTimeMillis() + (MINS2MS * timeInMins));
		edit.commit();

		if (C.D)
			Log.i(
					TAG,
					"updateNextAlarmTime: " + timeInMins + " so next alarm will be close to wall time: "
							+ System.currentTimeMillis() + (MINS2MS * timeInMins));
	}

	public static void setAlarm(Context context, int timeInMins)
	{
		if (C.D)
			Log.i(TAG, "setAlarm() for " + timeInMins + " mins.");

		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, Alarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), MINS2MS * timeInMins, pi); // Millisec
																																																						// *
																																																						// Second
																																																						// *
																																																						// Minute

		updateNextAlarmTime(context, timeInMins);
	}

	public static void cancelAlarm(Context context)
	{
		Intent intent = new Intent(context, Alarm.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
}
