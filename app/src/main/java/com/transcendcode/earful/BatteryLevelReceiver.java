/*
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.transcendcode.earful;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 
 */
public class BatteryLevelReceiver extends BroadcastReceiver
{

	public static final String TAG = "BatteryLevelReceiver";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String intentAction = intent.getAction();

		if (C.D)
			Log.i(TAG, "onReceive: " + intentAction);

		if (Intent.ACTION_BATTERY_LOW.equals(intentAction))
		{
			// TODO: Should abort our download on dropbox if we can
		}
		else if (Intent.ACTION_BATTERY_OKAY.equals(intentAction))
		{

		}
	}
}
