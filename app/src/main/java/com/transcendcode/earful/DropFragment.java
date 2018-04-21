
package com.transcendcode.earful;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DropFragment extends Fragment
{
    TextView mSyncTitle;
    TextView mCurrentBookDownload;
    ProgressBar mProgressBar;
    Button mResultIcon;

    private static Handler mUpdateHandler = new Handler();
    private ScreenUpdate mUpdateTimeTask = null;

    EarfulApplication mAppState;
    View mView;

    public static final String TAG = "DropFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        if (C.D)
            Log.i(TAG, "onCreateView()");
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.drop_progress_fragment, container, false);

        mAppState = ((EarfulApplication) getActivity().getApplicationContext());

        //text = (TextView) view.findViewById(R.id.progressText);
        mSyncTitle = (TextView) mView.findViewById(R.id.dropProgressText);
        mCurrentBookDownload = (TextView) mView.findViewById(R.id.dropProgressData);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.dropProgress);

        mResultIcon = (Button) mView.findViewById(R.id.resultIcon);
        mResultIcon.setVisibility(Button.INVISIBLE);

        mCurrentBookDownload.setText("Scan Starting");

        // Progress bar to show 1000 steps
        mProgressBar.setMax(1000);

        return mView;
    }

    private final class ScreenUpdate implements Runnable
    {
        public void run()
        {
            LibraryActivity parent = (LibraryActivity) getActivity();

            if (parent.mDropService != null && parent.mDropService.mSyncFileSize > 0)
            {
                setDataText(parent.mDropService.mSyncCurrentFile);
                // mProgressBar.setMax((int) parent.mDropService.mSyncFileSize);

                double percent_done = ((double) parent.mDropService.mSyncFileBytesDownloaded / (double) parent.mDropService.mSyncFileSize);

                mProgressBar.setProgress((int) (percent_done * 1000.0));
            }

            mUpdateHandler.postDelayed(this, 500);
        }
    }

    public void setDataText(final String t)
    {
        mCurrentBookDownload.post(new Runnable()
        {
            public void run()
            {
                mCurrentBookDownload.setText(t);
            }
        });
    }

    @Override
    public void onStart()
    {
        mUpdateTimeTask = new ScreenUpdate();
        mUpdateHandler.postDelayed(mUpdateTimeTask, 100);

        super.onStart();
    }

    @Override
    public void onStop()
    {
        mUpdateHandler.removeCallbacks(mUpdateTimeTask);
        mUpdateTimeTask = null;

        super.onStop();
    }
}
