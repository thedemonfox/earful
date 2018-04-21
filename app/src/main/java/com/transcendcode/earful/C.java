package com.transcendcode.earful;

public class C
{
	public static final boolean D = true;
	public static final boolean Save = false;
	public static final boolean Drop = false;

	public static final boolean DISABLE_SCAN_CACHE = false;

	public static final String EARFUL_EVENT = "earful_event";

	public static final String MSG_DROPBOX_SYNC_COMPLETED = "DROPBOX_SYNC_COMPLETED";
	public static final String MSG_PLAYBACK_CHANGED = "PLAYBACK_CHANGED";
	public static final String MSG_PLAYBACK_SERVICE_LOADED = "PLAYBACK_SERVICE_LOADED";
	public static final String MSG_SCAN_COMPLETED = "SCAN_COMPLETED";
	public static final String MSG_NEW_BOOK_PLAYING = "NEW_BOOK_PLAYING";
	public static final String MSG_BOOK_NOT_ACCESSIBLE = "BOOK_NOT_ACCESSIBLE";

	// Extra sent along with the play fragment when launched by the library fragment
	public static final String EXTRA_BOOK = "booknum";

	// Extra sent along with the play fragment
	public static final String EXTRA_SOURCE = "source";
	public static final String SOURCE_LIBRARY = "library";
	public static final String SOURCE_NOTIFICATION = "notification";
	public static final String SOURCE_MAIN = "main";

	public static final String EXTRA_START_PLAY = "play";
}
