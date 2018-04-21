
package com.transcendcode.earful;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Listener that detects shake gesture.
 */
public class ShakeEventListener implements SensorEventListener
{
    private int mThreshold = 150;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 2;

    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastTime;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;

    /** OnShakeListener that is called when shake is detected. */
    private OnShakeListener mShakeListener;

    /**
     * Interface for shake gesture.
     */
    public interface OnShakeListener
    {

        /**
         * Called when shake gesture is detected.
         */
        void onShake();
    }

    public void setThreshold(int _threshold)
    {
        mThreshold = _threshold;
    }
    
    public void setOnShakeListener(OnShakeListener listener)
    {
        mShakeListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent se)
    {
        long now = System.currentTimeMillis();
        final float[] values = se.values;

        if ((now - mLastForce) > SHAKE_TIMEOUT)
        {
            mShakeCount = 0;
        }

        if ((now - mLastTime) > TIME_THRESHOLD)
        {
            long diff = now - mLastTime;
            float speed = Math.abs(values[SensorManager.DATA_X]
                    + values[SensorManager.DATA_Y]
                    + values[SensorManager.DATA_Z] - mLastX - mLastY - mLastZ)
                    / diff * 10000;
            if (speed > mThreshold)
            {
                if ((++mShakeCount >= SHAKE_COUNT)
                        && (now - mLastShake > SHAKE_DURATION))
                {
                    mLastShake = now;
                    mShakeCount = 0;
                    mShakeListener.onShake();
                }
                mLastForce = now;
            }
            mLastTime = now;
            mLastX = values[SensorManager.DATA_X];
            mLastY = values[SensorManager.DATA_Y];
            mLastZ = values[SensorManager.DATA_Z];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }

}
