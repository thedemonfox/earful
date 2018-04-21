
package com.transcendcode.earful;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver
{
    public static final String TAG = "MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()))
        {
            if (C.D)
                Log.i(TAG, "Start up broadcast! Starting Alarm!");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autosync = prefs.getBoolean("dropAutoSyncPref", false);

            if (autosync)
            {
                if (C.D)
                    Log.i(TAG, "Autosync is enabled so starting alarm");

                int time = Integer.parseInt(prefs.getString("syncTimePref", "0"));

                if (time > 0)
                {
                    Alarm.setAlarm(context, time);
                }
                else
                {
                    Alarm.cancelAlarm(context);
                    if (C.D)
                        Log.i(TAG, "Sync time was set to 0 so we aren't going start autosync");
                }
            }
            else
            {
                if (C.D)
                    Log.i(TAG, "Autosync is not enabled, start up ignored");
            }
        }
    }
}
