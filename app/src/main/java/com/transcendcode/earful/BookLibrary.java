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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class BookLibrary.
 */
public class BookLibrary implements java.io.Serializable
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6248667562216869922L;

	/** The book list. */
	public List<Book> bookList = new ArrayList<Book>();

	public static final String TAG = "BookLibrary";

	public void clearLibrary()
	{
		if (bookList != null && bookList.size() > 0)
			bookList.clear();
	}

	Book findBook(int hash)
	{
		for (Book B : bookList)
		{
			if (B.hashCode() == hash)
			{
				return B;
			}
		}
		return null;
	}

	/**
	 * The Class DirectoryFilter.
	 */
	public static class DirectoryFilter implements FileFilter
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		@Override
		public boolean accept(File pathname)
		{
			// If this is a directory this we will add it to the directory list
			if (pathname.isDirectory())
			{
				return true;
			}
			return false;
		}

	}

	/**
	 * Sanity check.
	 */
	/*
	 * public void sanityCheck() { if (C.D) Log.i("LIBRARY", "Sanity Check");
	 * 
	 * if (C.D) Log.i("LIBRARY", "Quick check that book directories still exsist");
	 * 
	 * List<Book> originalList = bookList; List<Book> list =
	 * Collections.synchronizedList(originalList); synchronized (list) { for (Book book : list) { File
	 * dir = new File(book.mBookPath); if (!dir.exists() || !dir.isDirectory()) { if (C.D)
	 * Log.i("LIBRARY", "Removing book path: " + book.mBookPath); list.remove(book); } } } }
	 */
}
