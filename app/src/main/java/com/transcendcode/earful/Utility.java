
package com.transcendcode.earful;

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class Utility
{
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth)
        {
            if (width > height)
            {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            }
            else
            {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
    
  	public static String hashCodeString(int hash) 
  	{
  		String formatted = String.format(Locale.US, "%X", hash);
  		return formatted;
  		
  	}
    
    public static boolean DeleteRecursive(File fileOrDirectory)
    {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        return fileOrDirectory.delete();
    }

    public static boolean isMyServiceRunning(Activity _activity)
    {
        ActivityManager manager = (ActivityManager) _activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if ("com.transcendcode.earful.PlaybackService".equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    public static String convertMillis(int Millis)
    {
        String convert = String.format(Locale.US, "%02d:%02d:%02d",
                Millis / (1000 * 60 * 60), (Millis % (1000 * 60 * 60)) / (1000 * 60),
                ((Millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);
        return convert;
    }

    public static String convertSecs(int secs)
    {
        String convert = String.format(Locale.US, "%02d:%02d:%02d",
                secs / (60 * 60), (secs % (60 * 60)) / (60), ((secs % (60 * 60)) % (60)));
        return convert;
    }

    /**
     * This method convets dp unit to equivalent device specific value in pixels.
     * 
     * @param dp A value in dp(Device independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent Pixels equivalent to dp according to device
     */
    public static float convertDpToPixel(float dp, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * This method converts device specific pixels to device independent pixels.
     * 
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent db equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context)
    {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;

    }

}
