
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

public class ScanFragment extends Fragment
{
    TextView mCurrentBookText;
    ProgressBar mProgressBar1;
    //  ProgressBar mProgressBar2;
    LibraryScanService mScanService = null;

    private static Handler mUpdateHandler = new Handler();
    private ScreenUpdate mUpdateTimeTask = null;
    
    Button mResultIcon;
    //  OnScanFinishedListener mCallback;

    //   private List<Book> mPrevList = null;
    //   public List<Book> mScanList = null;
    EarfulApplication mAppState;
    View mView;

    // The container Activity must implement this interface so the frag can deliver messages
    /*
    public interface OnScanFinishedListener
    {
        // Called by HeadlinesFragment when a list item is selected 
        public void OnScanFinished(List<Book> scan_result);
    }
    */

    public static final String TAG = "ScanFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        if (C.D)
            Log.i(TAG, "onCreateView()");
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.progress_fragment, container, false);

        mAppState = ((EarfulApplication) getActivity().getApplicationContext());
        mScanService = ((LibraryActivity) getActivity()).mScanService;

        //text = (TextView) view.findViewById(R.id.progressText);
        mCurrentBookText = (TextView) mView.findViewById(R.id.progressData);
        mProgressBar1 = (ProgressBar) mView.findViewById(R.id.primaryProgressBar);
        mProgressBar1.setIndeterminate(false);

        mResultIcon = (Button) mView.findViewById(R.id.resultIcon);
        mResultIcon.setVisibility(Button.INVISIBLE);
        
        //     mProgressBar2 = (ProgressBar) mView.findViewById(R.id.secondaryProgressBar);
        //     mProgressBar2.setIndeterminate(false);

        mCurrentBookText.setText("Scan Starting");

        return mView;
    }

    private final class ScreenUpdate implements Runnable
    {
        /*
        public String mCurrentTitle;

        public int mTotalBooks = 0;
        public int mBookProgress = 0;

        public int mTotalFilesInBook = 0;
        public int mFileProgressInBook = 0;
        */
        public void run()
        {
            if (mScanService != null)
            {
                mCurrentBookText.setText(mScanService.mCurrentTitle);
                //setDataText(mScanService.mCurrentTitle);

                mProgressBar1.setMax(1000);
                float primaryProgress = (float) mScanService.mBookProgress / mScanService.mTotalBooks;
                float secondaryProgress = (float) mScanService.mFileProgressInBook / mScanService.mTotalFilesInBook;
                //mProgressBar1.setMax((int) mScanService.mTotalBooks);
                mProgressBar1.setSecondaryProgress((int) (primaryProgress * 1000.0f));
                mProgressBar1.setProgress((int) (secondaryProgress * 1000.0f));

                //  mProgressBar2.setMax((int) mScanService.mTotalFilesInBook);
                //  mProgressBar2.setProgress((int) mScanService.mFileProgressInBook);
            }

            mUpdateHandler.postDelayed(this, 50);
        }
    }

    /*
        @Override
        public void onAttach(Activity activity)
        {
            super.onAttach(activity);

            // This makes sure that the container activity has implemented
            // the callback interface. If not, it throws an exception.
            try
            {
                mCallback = (OnScanFinishedListener) activity;
            } catch (ClassCastException e)
            {
                throw new ClassCastException(activity.toString()
                        + " must implement OnScanFinishedListener");
            }
        }
    */
    public void setDataText(final String t)
    {
        mCurrentBookText.post(new Runnable()
        {
            public void run()
            {
                mCurrentBookText.setText(t);
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
    /*
        public void startScan(List<String> paths, List<Book> prev_scan)
        {
            ScannerTask task = new ScannerTask();
            mPrevList = prev_scan;
            task.execute(paths);
        }
    */

}
