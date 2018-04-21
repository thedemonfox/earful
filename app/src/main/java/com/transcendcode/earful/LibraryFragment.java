package com.transcendcode.earful;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
//import com.google.android.gms.ads.*;

public class LibraryFragment extends Fragment
{
	EarfulApplication mAppState;
	public static final String TAG = "LibraryFragment";
	private BookLibrary mBookLibrary = null;
	private static BookListAdapter adapter;
	View mView = null;
	//private AdView mAdView;

	// Fragment controllers
	private Book mSelectedBook = null;
	private TextView mHelpText = null;
	private ListView lv = null;

	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Get extra data included in the Intent
			String message = intent.getStringExtra("message");
			if (C.D)
				Log.i(TAG, "Got message: " + message);

			if (C.MSG_SCAN_COMPLETED.equals(message))
			{
				final LibraryActivity parent = (LibraryActivity) getActivity();

				mBookLibrary = mAppState.getLibrary();

				if (mBookLibrary == null || mBookLibrary.bookList.isEmpty() || mBookLibrary.bookList.size() == 0)
				{
					lv.setVisibility(View.INVISIBLE);
					mHelpText.setVisibility(View.VISIBLE);
				}
				else
				{
					adapter = new BookListAdapter(mView.getContext(), mBookLibrary.bookList, parent.mBoundService);
					lv.setAdapter(adapter);
					adapter.notifyDataSetChanged();

					mHelpText.setVisibility(View.INVISIBLE);
					lv.setVisibility(View.VISIBLE);
				}
			}
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (C.D)
			Log.i(TAG, "onCreateView()");

		// Inflate the layout for this fragment
		mView = inflater.inflate(R.layout.library_view, container, false);
		mAppState = ((EarfulApplication) getActivity().getApplicationContext());
		mBookLibrary = mAppState.getLibrary();
		final LibraryActivity parent = (LibraryActivity) getActivity();
//
//		mAdView = (AdView) mView.findViewById(R.id.adView);
//
//		if (mAdView != null)
//		{
//			AdRequest adRequest = new AdRequest.Builder()
//				.addTestDevice("767484182451ABD468862A26D4D78CF0")
//				.build();
//			mAdView.loadAd(adRequest);
//		}
		lv = (ListView) mView.findViewById(R.id.bookScrollList);
		mHelpText = (TextView) mView.findViewById(R.id.helpNotice);

		mHelpText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				Intent settingsActivity = new Intent(getActivity(), SettingsActivity.class);
				startActivity(settingsActivity);
			}
		});

		if (mBookLibrary == null || mBookLibrary.bookList.isEmpty())
		{
			lv.setVisibility(View.INVISIBLE);
			mHelpText.setVisibility(View.VISIBLE);
		}
		else
		{
			mHelpText.setVisibility(View.INVISIBLE);
			lv.setVisibility(View.VISIBLE);
		}

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			LinearLayout layout = (LinearLayout) mView.findViewById(R.id.backgroundView);
			layout.setBackgroundResource(R.drawable.dark_background);
		}
		else
		// Configuration.ORIENTATION_LANDSCAPE
		{
			// load the original BitMap (500 x 500 px)
			Bitmap bitmapOrg = BitmapFactory.decodeResource(getResources(), R.drawable.dark_background);

			int width = bitmapOrg.getWidth();
			int height = bitmapOrg.getHeight();

			// create a matrix for the manipulation
			Matrix matrix = new Matrix();

			// rotate the Bitmap
			matrix.postRotate(90);

			// recreate the new Bitmap
			Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);

			// make a Drawable from Bitmap to allow to set the BitMap
			// to the ImageView, ImageButton or what ever
			BitmapDrawable bmd = new BitmapDrawable(mView.getContext().getResources(), resizedBitmap);

			LinearLayout layout = (LinearLayout) mView.findViewById(R.id.backgroundView);
			layout.setBackgroundDrawable(bmd);
		}
		
		if (C.D)
			Log.i(TAG, "Creating BookListAdapter");
		adapter = new BookListAdapter(mView.getContext(), mBookLibrary.bookList, parent.mBoundService);

		if (adapter == null || lv == null)
		{
			if (C.D)
				Log.i(TAG, "Error Creating BookListAdapter");
		}
		else
		{
			// Assign adapter to ListView
			lv.setAdapter(adapter);

			int index = 0;
			if (mSelectedBook != null)
			{
				for (int i = 0; i < mBookLibrary.bookList.size(); i++)
				{
					if (mBookLibrary.bookList.get(i).hashCode() == mSelectedBook.hashCode())
					{
						index = i;
						break;
					}
				}
			}

			lv.setSelectionFromTop(index, 0);
			lv.setOnItemClickListener(new ListView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					((LibraryActivity) getActivity()).bookSelected(position);

				}
			});
		}
		return mView;
	}

	@Override
	public void onStart()
	{
		if (C.D)
			Log.i(TAG, "onStart()");

		super.onStart();

		// adapter.notifyDataSetChanged();
		LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
				new IntentFilter(C.EARFUL_EVENT));
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onStop()
	{
		if (C.D)
			Log.i(TAG, "onStart()");

		// Call the adapter just in case another book was selected

		LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mMessageReceiver);

		super.onStop();
	}

	@Override
	public void onPause()
	{
		//mAdView.pause();

		super.onPause();
	}

	@Override
	public void onResume()
	{
		//mAdView.resume();
		super.onResume();
	}


	@Override
	public void onDestroy()
	{
		//mAdView.destroy();
		super.onDestroy();
	}
}
