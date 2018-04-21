package com.transcendcode.earful;

import java.util.List;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The Class BookListAdapter.
 */
public class BookListAdapter extends ArrayAdapter<Book>
{

	/** The context. */
	private final Context mContext;

	/** The list. */
	private final List<Book> mList;

	public int cur_position = 0;

	private PlaybackService mService;

	public static final String TAG = "BookListAdapter";

	/**
	 * Instantiates a new book list adapter.
	 * 
	 * @param context the context
	 * @param list the list
	 * @param mBoundService
	 */
	public BookListAdapter(Context _context, List<Book> _list, PlaybackService _service)
	{
		super(_context, R.layout.rowlayout, _list);
		this.mContext = _context;
		this.mList = _list;
		this.mService = _service;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.label);
		final ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

		rowView.setBackgroundColor(Color.BLACK);

		if (mList != null)
		{
			if (mService != null && mService.getPlayingBook() != 0
					&& mList.get(position).hashCode() == mService.getPlayingBook())
			{
				rowView.setBackgroundResource(R.drawable.book_select);
				cur_position = position;
			}
			else
			{
				rowView.setBackgroundColor(Color.BLACK);
			}

			ViewTreeObserver vto = imageView.getViewTreeObserver();
			vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				@Override
				public boolean onPreDraw()
				{
					if (mList.get(position).mCoverPath != null)
					{
						if (C.Save)
							Log.i(TAG, "No bitmap loaded for cover, loading " + mList.get(position).mCoverPath + " into x: "
									+ imageView.getMeasuredWidth() + " y:" + imageView.getMeasuredHeight());
						// First decode with inJustDecodeBounds=true to check dimensions
						final BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = true;

						// Calculate inSampleSize
						options.inSampleSize = Utility.calculateInSampleSize(options, 100, 100);

						// Decode bitmap with inSampleSize set
						options.inJustDecodeBounds = false;

						imageView.setImageBitmap(BitmapFactory.decodeFile(mList.get(position).mCoverPath, options));

						int padding_in_dp = 4;
						final float scale = mContext.getResources().getDisplayMetrics().density;
						int padding_in_px = (int) (padding_in_dp * scale + 0.5f);

						imageView.setPadding(0, 0, 0, padding_in_px);
					}
					else
					{
						imageView.setImageResource(R.drawable.default_cover);
					}

					// Be sure to remove the listener after the view is set
					if (imageView.getViewTreeObserver().isAlive())
					{
						imageView.getViewTreeObserver().removeOnPreDrawListener(this);
					}

					return true;
				}
			});

			if (mList.get(position).mAuthor != null)
				textView.setText(mList.get(position).mTitle + "\n" + mList.get(position).mAuthor);
			else
				textView.setText(mList.get(position).mTitle);
		}

		return rowView;
	}
}
