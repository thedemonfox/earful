package com.transcendcode.earful;

//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayFragment extends Fragment
{
	EarfulApplication mAppState;
	public static final String TAG = "PlayFragment";

	private ScreenUpdate mUpdateTimeTask = null;
	private static Handler mUpdateHandler = new Handler();
	View mView;

	private Book mSelectedBook = null;
	private BookState mSelectedBookState = null;

	// Help overlay variables
	static final String SHOW_ACTIVITY_OVERLAY = "PlaybackActivity_Help";
	private boolean mShowHelpOverlay = true;

	//private AdView mAdView;


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

			if (C.MSG_PLAYBACK_CHANGED.equals(message))
			{
				updateButtons();
			}

		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		mView = inflater.inflate(R.layout.playback_view, container, false);
		mAppState = ((EarfulApplication) getActivity().getApplicationContext());
//
//		mAdView = (AdView) mView.findViewById(R.id.adView);
//		if (mAdView != null)
//		{
//			AdRequest adRequest = new AdRequest.Builder().addTestDevice("767484182451ABD468862A26D4D78CF0").build();
//			mAdView.loadAd(adRequest);
//		}

		// Help image only supported in portrait mode I am afraid
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			// If the screen is now in landscape mode, we can show the
			// dialog in-line with the list so we don't need this activity.

			// Show help overlay if it has never been closed by the user before
			mShowHelpOverlay = mAppState.prefs.getBoolean(SHOW_ACTIVITY_OVERLAY, true);
			if (!mShowHelpOverlay)
			{
				LinearLayout overlay_view = (LinearLayout) mView.findViewById(R.id.helpOverlay);
				overlay_view.setVisibility(View.INVISIBLE);
			}

			// Allow user to remove help overlay by clicking it
			final ImageView overlay_img = (ImageView) mView.findViewById(R.id.overlay_img);
			overlay_img.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					LinearLayout overlay_view = (LinearLayout) mView.findViewById(R.id.helpOverlay);
					overlay_view.setVisibility(View.INVISIBLE);
					mShowHelpOverlay = false;

					// Save the help shown state
					SharedPreferences.Editor editor = mAppState.prefs.edit();
					editor.putBoolean(SHOW_ACTIVITY_OVERLAY, mShowHelpOverlay);
					editor.commit();
				}
			});
		}

		if (C.D)
			Log.i(TAG, "Let's find out where we were launched from");

		if (getActivity().getIntent() != null && getActivity().getIntent().getExtras() != null)
		{
			Bundle bundle = getActivity().getIntent().getExtras();
			String from = bundle.getString(C.EXTRA_SOURCE);

			// Check if we have been launched by the library
			PlaybackActivity parent = (PlaybackActivity) getActivity();
			PlaybackService boundService = null;
			
			if (parent != null)
				boundService = parent.mBoundService;
			
			BookLibrary library = mAppState.getLibrary();
			
			if (C.SOURCE_LIBRARY.equals(from))
			{

				if (C.D)
					Log.i(TAG, "Launched from library");

				int booknum = bundle.getInt(C.EXTRA_BOOK);

				// See if the service has a playing book, if it doesn't then maybe we are picking a new book
				if (boundService.getPlayingBook() != 0
						&& boundService.getPlayingBook() == library.bookList.get(booknum).hashCode())
				{
					if (C.D)
						Log.i(TAG,
								"The currently playing book is the one selected so doing mSelectedBook = parent.mBoundService.getPlayingBook()");

					// mSelectedBook = parent.mBoundService.getPlayingBook();
					setBook(library.findBook(boundService.getPlayingBook()));
				}
				else
				{
					if (C.D)
						Log.i(TAG, "Selecting a non-playing book, so setting mSelectedBook to one in the library");

					setBook(library.bookList.get(booknum));
				}

			}
			else if (C.SOURCE_NOTIFICATION.equals(from) || C.SOURCE_MAIN.equals(from))
			{
				if (C.D)
					Log.i(TAG, "Launched from notification");

				setBook(library.findBook(boundService.getPlayingBook()));
			}
			else
			{
				if (C.D)
					Log.i(TAG, "Don't know where we were launched from");

			}

		}
		else
		{
			if (C.D)
				Log.i(TAG, "No intent extras found");
		}

		// PlaybackActivity parent = (PlaybackActivity) getActivity();

		// myReceiver = new ReceiveMessages();

		// Set up the background image for this view
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

		// if (parent.mBoundService != null)
		{
			updateButtons();
			/*
			 * if (parent.mBoundService.getPlayingBook() != 0 && parent.mBoundService.getPlayingBook() ==
			 * mSelectedBook.hashCode() && (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING ||
			 * parent.mBoundService.getPlaybackMode() == PlayState.PAUSED)) { if (C.D) Log.i(TAG,
			 * "Initing view with data from Bound Service");
			 * updatePlayingData(parent.mBoundService.getPlayingBook()); } else {
			 */
			if (C.D)
				Log.i(TAG, "Initing view with data from selection data");

			updateDisplay(mSelectedBook, mSelectedBookState);

			final SquareButton prevTrackButton = (SquareButton) mView.findViewById(R.id.prev_track);

			prevTrackButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					PlaybackActivity parent = (PlaybackActivity) getActivity();

					if (parent.mBoundService.getPlayingBook() != 0
							&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
							&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
					{
						if (mSelectedBook.tracks.lowerKey(mSelectedBookState.mCurrentTrack) != null)
						{
							parent.mBoundService.prevTrack(); // TODO: Can use a return value from this
							updateDisplay(mSelectedBook, mSelectedBookState);
							// updatePlayingData(parent.mBoundService.getPlayingBook());
						}
					}
					else
					{
						mSelectedBookState.mCurrentTrack--; // TODO: Look into what play service does
						if (mSelectedBookState.mCurrentTrack < mSelectedBook.getFirstTrack())
						{
							mSelectedBookState.mCurrentTrack = mSelectedBook.getFirstTrack();
						}

						updateDisplay(mSelectedBook, mSelectedBookState);
						mAppState.saveState(mSelectedBookState);
					}
					// mBoundService.prevTrack();
				}
			});

			final SquareButton nextTrackButton = (SquareButton) mView.findViewById(R.id.next_track);

			nextTrackButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					final PlaybackActivity parent = (PlaybackActivity) getActivity();

					if (parent.mBoundService.getPlayingBook() != 0
							&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
							&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
					{
						if (mSelectedBook.tracks.higherKey(mSelectedBookState.mCurrentTrack) != null)
						{
							parent.mBoundService.nextTrack(false);
							updateDisplay(mSelectedBook, mSelectedBookState);
						}
					}
					else
					{

						mSelectedBookState.mCurrentTrack++; // TODO: Fix
						if (mSelectedBookState.mCurrentTrack > mSelectedBook.getLastTrack())
						{
							mSelectedBookState.mCurrentTrack = mSelectedBook.getLastTrack();
						}

						updateDisplay(mSelectedBook, mSelectedBookState);
						mAppState.saveState(mSelectedBookState);
					}
				}
			});

			final SquareButton advanceTimeButton = (SquareButton) mView.findViewById(R.id.forward_30secs);

			// Handle the forward 30 second button
			advanceTimeButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					final PlaybackActivity parent = (PlaybackActivity) getActivity();

					if (parent.mBoundService.getPlayingBook() != 0
							&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
							&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
					{
						int cur_ms = parent.mBoundService.getCurrentPosition();
						int dur_ms = mSelectedBook.getTrack(mSelectedBookState.mCurrentTrack).mLength;

						// Check if the advance of time will outrun the length of the file
						if (cur_ms + 30 < dur_ms)
						{
							parent.mBoundService.seekTo(cur_ms + 30);
						}
						else
						{
							if (C.D)
								Log.i(TAG, "Can't advance: cur_ms + 30 =  " + (cur_ms + 30) + ", dur_ms = " + dur_ms);
						}
					}
					else
					{
						mSelectedBookState.mCurrentTime += 30;
						if (mSelectedBookState.mCurrentTime > mSelectedBook.getCurrentTrackTime(mSelectedBookState))
						{
							mSelectedBookState.mCurrentTime = mSelectedBook.getCurrentTrackTime(mSelectedBookState);
						}

						updateDisplay(mSelectedBook, mSelectedBookState);
						mAppState.saveState(mSelectedBookState);
					}
				}
			});

			final SquareButton regressTimeButton = (SquareButton) mView.findViewById(R.id.back_30secs);

			// Handle the back 30 second button
			regressTimeButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					final PlaybackActivity parent = (PlaybackActivity) getActivity();

					if (parent.mBoundService.getPlayingBook() != 0
							&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
							&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
					{
						int cur_ms = parent.mBoundService.getCurrentPosition();

						// Check if the advance of time will outrun the length of the file
						if (cur_ms - (30) > 0)
						{
							parent.mBoundService.seekTo(cur_ms - (30));
						}
						else
						// We will reset the current track to 0 time
						{
							parent.mBoundService.seekTo(0);
							if (C.D)
								Log.i(TAG, "Can't regress: cur_ms - (30) =  " + (cur_ms - (30)));
						}
					}
					else
					{
						mSelectedBookState.mCurrentTime -= 30;
						if (mSelectedBookState.mCurrentTime < 0)
						{
							mSelectedBookState.mCurrentTime = 0;
						}

						updateDisplay(mSelectedBook, mSelectedBookState);
						mAppState.saveState(mSelectedBookState);
					}
				}
			});

			final ImageView coverView = (ImageView) mView.findViewById(R.id.bookCover);
			TextView authorView = (TextView) mView.findViewById(R.id.bookAuthor);
			TextView titleView = (TextView) mView.findViewById(R.id.bookTitle);
			TextView lastTrackView = (TextView) mView.findViewById(R.id.endTrack);
			TextView currentTimeView = (TextView) mView.findViewById(R.id.currentTime);
			TextView playingTrackView = (TextView) mView.findViewById(R.id.playingTrack);
			SeekBar currentTime = (SeekBar) mView.findViewById(R.id.timeSeekBar);
			SeekBar currentTrack = (SeekBar) mView.findViewById(R.id.trackSeekBar);

			ViewTreeObserver vto = coverView.getViewTreeObserver();
			vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw()
				{
					if (mSelectedBook.mCoverPath != null)
					{
						// We need to cache the cover for this book
						if (C.Save)
							Log.i(
									TAG,
									"No bitmap loaded for cover, loading " + mSelectedBook.mCoverPath + " into x: "
											+ coverView.getWidth() + " y:" + coverView.getHeight());

						// First decode with inJustDecodeBounds=true to check dimensions
						final BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = true;

						// Bitmap bitmap = BitmapFactory.decodeFile(mSelectedBook.mCoverPath, options);

						// Calculate inSampleSize
						options.inSampleSize = Utility.calculateInSampleSize(options, coverView.getWidth(), coverView.getHeight());

						// Decode bitmap with inSampleSize set
						options.inJustDecodeBounds = false;

						coverView.setImageBitmap(BitmapFactory.decodeFile(mSelectedBook.mCoverPath, options));
					}
					else
					{
						coverView.setImageResource(R.drawable.default_cover);
					}

					// Be sure to remove the listener after the view is set
					if (coverView.getViewTreeObserver().isAlive())
					{
						coverView.getViewTreeObserver().removeOnPreDrawListener(this);
					}

					return true;
				}
			});

			authorView.setText(mSelectedBook.mAuthor);
			titleView.setText(mSelectedBook.mTitle);
			lastTrackView.setText(mSelectedBook.getLastTrack().toString());
			playingTrackView.setText(String.valueOf(mSelectedBookState.mCurrentTrack));
			currentTimeView.setText(Utility.convertSecs(mSelectedBookState.mCurrentTime));

			currentTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				int selectedTrack;

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
				{
					final PlaybackActivity parent = (PlaybackActivity) getActivity();

					if (fromUser
							&& parent.mBoundService.getPlayingBook() != 0
							&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
							&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
					{
						selectedTrack = progress + 1;
						mSelectedBookState.mCurrentTime = 0;
						TextView playingTrackView = (TextView) mView.findViewById(R.id.playingTrack);
						playingTrackView.setText(String.valueOf(selectedTrack));
					}
					else if (fromUser) // Then we are probably selecting
					{
						mSelectedBookState.mCurrentTrack = progress + 1;
						mSelectedBookState.mCurrentTime = 0;

						updateDisplay(mSelectedBook, mSelectedBookState);
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
				{
					mUpdateHandler.removeCallbacks(mUpdateTimeTask);
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
				{
					final PlaybackActivity parent = (PlaybackActivity) getActivity();

					if (parent.mBoundService.getPlayingBook() != 0
							&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
							&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
					{
						parent.mBoundService.selectTrack(selectedTrack);
					}

					mAppState.saveState(mSelectedBookState);
					mUpdateHandler.postDelayed(mUpdateTimeTask, 10);
				}
			});

			currentTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
				{
					final PlaybackActivity parent = (PlaybackActivity) getActivity();

					if (fromUser
							&& parent.mBoundService.getPlayingBook() != 0
							&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
							&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
					{
						parent.mBoundService.seekTo(progress);
						mSelectedBookState.mCurrentTime = progress;

						updateDisplay(mSelectedBook, mSelectedBookState);
					}
					else if (fromUser) // Then we are probably selecting
					{
						mSelectedBookState.mCurrentTime = progress;

						updateDisplay(mSelectedBook, mSelectedBookState);
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
				{
					mUpdateHandler.removeCallbacks(mUpdateTimeTask);
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
				{
					mAppState.saveState(mSelectedBookState);
					mUpdateHandler.postDelayed(mUpdateTimeTask, 10);
				}
			});

		}

		return mView;
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
	}

	@Override
	public void onStart()
	{
		mUpdateTimeTask = new ScreenUpdate();
		mUpdateHandler.postDelayed(mUpdateTimeTask, 10);

		LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
				new IntentFilter(C.EARFUL_EVENT));

		super.onStart();
	}

	@Override
	public void onStop()
	{
		mUpdateHandler.removeCallbacks(mUpdateTimeTask);
		mUpdateTimeTask = null;

		LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mMessageReceiver);

		super.onStop();
	}

	public void updateDisplay(final Book data, final BookState cur_data)
	{
		// final PlaybackActivity parent = (PlaybackActivity) getActivity();

		if (C.D)
			Log.i(TAG, "PlayFragment updateDisplay");

		TextView playingTrackView = (TextView) mView.findViewById(R.id.playingTrack);

		if (cur_data != null)
		{
			playingTrackView.setText(String.valueOf(cur_data.mCurrentTrack));
	
			TextView currentTimeView = (TextView) mView.findViewById(R.id.currentTime);
			// currentTimeView.setText(Utility.convertSecs(parent.mBoundService.getCurrentPosition()));
			currentTimeView.setText(Utility.convertSecs(cur_data.mCurrentTime));
	
			SeekBar currentTrack = (SeekBar) mView.findViewById(R.id.trackSeekBar);
			currentTrack.setMax(data.getLastTrack() - 1);
			currentTrack.setProgress(cur_data.mCurrentTrack - 1);
	
			SeekBar currentTime = (SeekBar) mView.findViewById(R.id.timeSeekBar);
			TextView endTimeView = (TextView) mView.findViewById(R.id.endTime);
	
			if (data.getTrack(cur_data.mCurrentTrack) != null)
			{
				currentTime.setEnabled(true);
				currentTime.setMax(data.getTrack(cur_data.mCurrentTrack).mLength);
				currentTime.setProgress(cur_data.mCurrentTime);
	
				endTimeView.setEnabled(true);
				endTimeView.setText(Utility.convertSecs(data.getTrack(cur_data.mCurrentTrack).mLength));
			}
			else
			{
				currentTime.setEnabled(false);
				endTimeView.setEnabled(false);
			}
			
			updateButtons();
		}
	}

	private final class ScreenUpdate implements Runnable
	{

		public void run()
		{
			final PlaybackActivity parent = (PlaybackActivity) getActivity();

			if (parent.mBoundService != null && parent.mBoundService.getPlayingBook() != 0 // Is there a
																																											// valid book
																																											// loaded into
																																											// the
																																											// service?
					&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode() // If the book
																																								// selected matches
																																								// the book being
																																								// played
					&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || // If the book is
																																							// playing or paused
					parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
			{
				// updatePlayingData(parent.mBoundService.getPlayingBook()); // If so, then update the
				// playing information from the service
				mSelectedBookState.mCurrentTrack = parent.mBoundService.getCurrentTrack();
				mSelectedBookState.mCurrentTime = parent.mBoundService.getCurrentTime();
			}
			else
			{
				// mSelectedBookState = mAppState.loadState(mSelectedBook.hashCode());
			}
			// else
			// {
			// TODO: We need to add some more checks here for weird books
			// if (mSelectedBook.getTrack(mSelectedTrack) != null)
			// {
			// updateDisplay(mSelectedBook.mCurrentTrack, mSelectedBook.getLastTrack(),
			// mSelectedBook.mCurrentTime,
			// mSelectedBook.getCurrentTrackTime());
			// }
			// }
			updateDisplay(mSelectedBook, mSelectedBookState);

			mUpdateHandler.postDelayed(this, 1000);
		}
	}

    private void setBook(Book book)
    {
        if (book != null)
        {
            mSelectedBook = book;
            mSelectedBookState = mAppState.loadState(book.hashCode());
        }
    }
    
    private void updateButtons()
    {
        updatePlayButton();
        updateStopButton();
        updateSleepButton();
    }

	private void updatePlayButton()
	{
		final PlaybackActivity parent = (PlaybackActivity) getActivity();
		final SquareButton playButton = (SquareButton) mView.findViewById(R.id.play);

		if (parent.mBoundService.getPlayingBook() != 0 && parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
				&& parent.mBoundService.getPlaybackMode() == PlayState.PLAYING)
		{
			playButton.setBackgroundResource(R.drawable.pause);
		}
		else
		{
			playButton.setBackgroundResource(R.drawable.play);
		}

		playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				final PlaybackActivity parent = (PlaybackActivity) getActivity();

				// Handle selecting a new book from the one playing
				if (parent.mBoundService.getPlayingBook() != mSelectedBook.hashCode())
				{
					if (C.D)
						Log.i(TAG, "Clicked Play/Pause button - selecting a new book from the one playing");
					if (parent.mBoundService.playNewBook(mSelectedBook.hashCode()))
					{
						playButton.setBackgroundResource(R.drawable.pause);
					}
				}
				else
				// Playback view is showing the actively playing book
				{
					if (C.D)
						Log.i(
								TAG,
								"Clicked Play/Pause button - showing the actively playing book - "
										+ parent.mBoundService.getPlaybackMode());
					if (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING)
					{
						parent.mBoundService.pause();
						playButton.setBackgroundResource(R.drawable.play);
					}
					else if (parent.mBoundService.getPlaybackMode() == PlayState.PAUSED)
					{
						parent.mBoundService.start();
						playButton.setBackgroundResource(R.drawable.pause);
					}
					else
					{
						if (parent.mBoundService.playNewBook(mSelectedBook.hashCode()))
						{
							playButton.setBackgroundResource(R.drawable.pause);
						}
					}
				}
				updateStopButton();
				updateSleepButton();
			}
		});
	}

	private void updateSleepButton()
	{
		final SquareButton sleepButton = (SquareButton) mView.findViewById(R.id.sleep);
		final PlaybackActivity parent = (PlaybackActivity) getActivity();

		if (parent.mBoundService.getPlayingBook() != 0
				&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
		{
			sleepButton.setVisibility(View.VISIBLE);
		}
		else
		{
			sleepButton.setVisibility(View.INVISIBLE);
		}

		if (SleepState.isOn(parent.mBoundService.mSleepState))
		{
			sleepButton.setBackgroundResource(R.drawable.sleep_active);
		}
		else
		{
			sleepButton.setBackgroundResource(R.drawable.sleep);
		}

		sleepButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				final PlaybackActivity parent = (PlaybackActivity) getActivity();
				if (SleepState.isOn(parent.mBoundService.mSleepState))
				{
					parent.mBoundService.StopSleep();
					sleepButton.setBackgroundResource(R.drawable.sleep);
				}
				else
				{
					if (parent.mBoundService.StartSleep())
					{
						sleepButton.setBackgroundResource(R.drawable.sleep_active);
					}
				}
			}
		});
	}

	private void updateStopButton()
	{
		final PlaybackActivity parent = (PlaybackActivity) getActivity();
		final SquareButton stopButton = (SquareButton) mView.findViewById(R.id.stopbook);
		if (parent.mBoundService.getPlayingBook() != 0
				&& parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
				&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
		{
			stopButton.setVisibility(View.VISIBLE);
		}
		else
		{
			stopButton.setVisibility(View.INVISIBLE);
		}

		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				final PlaybackActivity parent = (PlaybackActivity) getActivity();
				if (parent.mBoundService.getPlayingBook() == mSelectedBook.hashCode()
						&& (parent.mBoundService.getPlaybackMode() == PlayState.PLAYING || parent.mBoundService.getPlaybackMode() == PlayState.PAUSED))
				{
					mSelectedBookState.mCurrentTrack = 1;
					mSelectedBookState.mCurrentTime = 0;
					parent.mBoundService.stop();
					updateDisplay(mSelectedBook, mSelectedBookState);
					updateButtons();
				}
			}
		});
	}
}
