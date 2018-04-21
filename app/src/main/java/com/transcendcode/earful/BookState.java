package com.transcendcode.earful;

public class BookState implements java.io.Serializable
{
	private static final long serialVersionUID = -4697021494690880792L;
	public int mBookHash = 0;
	public int mCurrentTime = 0;
	public int mCurrentTrack = 1;

	// Only used by the playback Service, should be ignored by others
	public PlayState mMode = PlayState.STOPPED;
	
	public void setCurrentTrack(int mCurrentTrack)
	{
		this.mCurrentTrack = mCurrentTrack;
	}
}
