/**
 * TODO
 * 
 * <h4>Description</h4> TODO
 * 
 * <h4>Notes</h4> TODO
 * 
 * <h4>References</h4> TODO
 * 
 * @author $Author$
 * 
 * @version $Rev$
 * 
 * @see TODO
 */

package com.transcendcode.earful;

import java.util.Locale;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Book implements java.io.Serializable
{
	private static final long serialVersionUID = 9090509725464654482L;
	public String mTitle = "";
	public String mAuthor = "";
	public String mGenre = "";
	public String mBookPath = ""; // Used for sanity checks

	public String mCoverPath = null;

	public TreeMap<Integer, BookTrack> tracks = new TreeMap<Integer, BookTrack>();

	public boolean mNoTags = false;

	public Book()
	{}


	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(19, 37). // two randomly chosen prime numbers
				// if deriving: appendSuper(super.hashCode()).
				append(mTitle).append(mAuthor).append(mGenre).toHashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (obj.getClass() != getClass())
			return false;

		Book rhs = (Book) obj;
		return new EqualsBuilder().
		// if deriving: appendSuper(super.equals(obj)).
				append(mTitle, rhs.mTitle).append(mAuthor, rhs.mAuthor).append(mGenre, rhs.mGenre).isEquals();
	}

	public Book(String title)
	{
		mTitle = title;
	}

	public Integer addLastTrack(BookTrack track)
	{
		Integer highest_key = 1;
		if (!tracks.isEmpty())
			highest_key = tracks.lastKey() + 1;

		addTrack(highest_key, track);
		return highest_key;
	}

	public void addTrack(Integer track_num, BookTrack track)
	{
		tracks.put(track_num, track);
	}

	public BookTrack getTrack(int track)
	{
		return tracks.get(track);
	}

	public String getCurrentTrackPath(final BookState state)
	{
		// if (hashCode() != state.mBookHash) throw new StateMismatchException();

		if (tracks != null)
		{
			BookTrack track = tracks.get(state.mCurrentTrack);
			if (track != null)
			{
				return tracks.get(state.mCurrentTrack).mPath;
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	public int getCurrentTrackTime(final BookState state)
	{
		// if (hashCode() != state.mBookHash) throw new StateMismatchException();

		if (tracks != null)
		{
			BookTrack track = tracks.get(state.mCurrentTrack);

			if (track != null)
			{
				return tracks.get(state.mCurrentTrack).mLength;
			}
			else
			{
				return 0;
			}
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @return the mFirstTrack
	 */
	public Integer getFirstTrack()
	{
		return tracks.firstKey();
	}

	/**
	 * @return the mLastTrack
	 */
	public Integer getLastTrack()
	{
		return tracks.lastKey();
	}


	public Integer getTotalTracks()
	{
		return tracks.size();
	}

	public String hashCodeString()
	{
		String formatted = String.format(Locale.US, "%X", hashCode());
		return formatted;

	}
}
