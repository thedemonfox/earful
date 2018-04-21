
package com.transcendcode.earful;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;

import com.transcendcode.earful.BookLibrary.DirectoryFilter;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class LibraryScanService extends IntentService
{

    private static final String TAG = "LibraryScanService";
    EarfulApplication mAppState = null;

    public String mCurrentTitle;

    public int mTotalBooks = 0;
    public int mBookProgress = 0;

    public int mTotalFilesInBook = 0;
    public int mFileProgressInBook = 0;

    private List<Book> mPrevList = null;
    public List<Book> mScanList = null;
    private BookLibrary mBookLibrary = null;

    private final IBinder mBinder = new LocalBinder();

    public LibraryScanService()
    {
        super("Earful Library Service");
    }

    /**
     * Class for clients to access. Because we know this service always runs in the same process as its clients, we
     * don't need to deal with IPC.
     */
    public class LocalBinder extends Binder
    {
        LibraryScanService getService()
        {
            return LibraryScanService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        if (C.D)
            Log.i(TAG, "Last client unbound");
        return false;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        super.onStart(intent, startId);

        mAppState = ((EarfulApplication) getApplicationContext());
    }

    /*
    parent.startScan(paths, Collections.unmodifiableList(mBookLibrary.bookList));
    
    public void startScan(List<String> paths, List<Book> prev_scan)
    {
        ScannerTask task = new ScannerTask();
        mPrevList = prev_scan;
        task.execute(paths);
    }
    */

    @Override
    public void onHandleIntent(Intent intent)
    {
        if (C.D)
            Log.i(TAG, "LibraryScanService invoked");

        List<String> paths = new ArrayList<String>();
        for (int i = SearchDirectories.FOLDER_REQUEST_CODE1; i <= SearchDirectories.FOLDER_REQUEST_CODE5; i++)
        {
            String str = "directory" + i;
            String dir = mAppState.prefs.getString(str, "");

            if (C.D)
                Log.i(TAG, "prefs: " + str + " is " + dir);
            if (!dir.equals(""))
            {
                paths.add(dir);
            }
        }

        String dir = mAppState.prefs.getString("dropDirectoryPref", "");
        if (!dir.equals(""))
        {
            if (C.D)
                Log.i(TAG, "dropsync dir is " + dir);
            paths.add(dir);
        }

        if (paths.size() > 0)
        {
            // Remove duplicate directories
            HashSet<String> hs = new HashSet<String>();
            hs.addAll(paths);
            paths.clear();
            paths.addAll(hs);

            //   final LibraryActivity parent = (LibraryActivity) getActivity();

            //    ScannerTask task = new ScannerTask();
            mBookLibrary = mAppState.getLibrary();

            mPrevList = Collections.unmodifiableList(mBookLibrary.bookList);

            mScanList = new ArrayList<Book>();
            //    task.execute(paths);

            List<String> pathList = paths;

            for (String path : pathList)
            {
                File rootDir = new File(path);
                DirectoryFilter filter = new DirectoryFilter();
                List<File> readList = findDirectories(rootDir, filter);

                // A sanity check before we waste some time scanning nothing
                if (readList != null && readList.size() > 0)
                {

                    mTotalBooks = readList.size();
                    mBookProgress = 0;
                    // mProgressBar1.setMax(readList.size());
                    // mProgressBar1.setProgress(0);

                    try
                    {
                        processDirectories(readList);
                    } catch (CannotReadException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (TagException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InvalidAudioFrameException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            // mCallback.OnScanFinished(Collections.unmodifiableList(mScanList));
            //BookLibrary lib = mAppState.getLibrary();
            mBookLibrary.bookList = mScanList;
            mAppState.saveLibrary();

            if (C.D)
                Log.i(TAG, "Broadcasting message: " + C.MSG_SCAN_COMPLETED);
            Intent i = new Intent(C.EARFUL_EVENT);
            i.putExtra("message", C.MSG_SCAN_COMPLETED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
    }

    public void addTrack(Book object, int track, File f, int length)
    {
        if (C.D)
            Log.i(TAG, "Adding new track #" + track
                    + "  "
                    + f.getName() + " to book " + object.mTitle + " with length " + length /*audioHeader.getTrackLength()*/);

        BookTrack book_track = new BookTrack();

        book_track.mPath = f.getAbsolutePath();
        book_track.mLength = length;

        object.addTrack(track, book_track);
    }
    
    private void addTrackByName(Book object, File f, int length)
    {
        if (C.D)
            Log.i(TAG, "Adding new track " +
                    f.getName() + " to book " + object.mTitle + " with length " + length /*audioHeader.getTrackLength()*/);

        BookTrack book_track = new BookTrack();

        book_track.mPath = f.getAbsolutePath();
        book_track.mLength = length;

        Integer num = object.addLastTrack(book_track);
        
        if (C.D)
            Log.i(TAG, "Added as track " + num);
    
    }


    /**
     * Find directories.
     * 
     * @param rootDir the root dir
     * @param filter the filter
     * @return the list
     */
    private List<File> findDirectories(File rootDir, FileFilter filter)
    {
        List<File> result = new ArrayList<File>();
        if (!rootDir.exists() || !rootDir.isDirectory())
        {
            if (C.D)
                Log.i(TAG, rootDir.toString() + " doesn't exsist!");
            return result;
        }

        //Add all files that comply with the given filter
        File[] files = rootDir.listFiles(filter);
        for (File f : files)
        {
            if (!result.contains(f))
            {
                //      mLogger.log("Found: " + f.toString());
                result.add(f);
            }
        }

        return result;
    }

    public void processFile(File f, String img_path) throws CannotReadException,
            IOException, TagException,
            InvalidAudioFrameException
    {
        if(C.D)
            Log.i(TAG, "Scanning file: " + f.toString());

        MP3File mp3File;
        String title = null;
        String author = null;
        String genre = null;
        int track = -1;
        int length = 0;
        //String img_path;

       // try { Thread.sleep( 1000 ); } catch (InterruptedException e) {}
        
        // Try to find this track in the previous scan, if we find it use that
        boolean prevScanFound = false;

        for (Book b : mPrevList)
        {
            for (Entry<Integer, BookTrack> entry : b.tracks.entrySet())
            {
                Integer key = entry.getKey();
                BookTrack t = entry.getValue();

                // TODO: Should also add some checks file time differences, file size differences
                if (f.getAbsolutePath().equals(t.mPath))
                {
                    if (C.D)
                        Log.i(TAG, "Found a previously scanned version of this file");

                    prevScanFound = true;

                    title = b.mTitle;
                    author = b.mAuthor;
                    genre = b.mGenre;
                    length = t.mLength;
                    //img_path = b.mCoverPath;
                    track = key;
                }
            }

        }

        if (C.DISABLE_SCAN_CACHE)
            prevScanFound = false;

        if (!prevScanFound)
        {
            mp3File = (MP3File) AudioFileIO.read(f);
            MP3AudioHeader audioHeader = (MP3AudioHeader) mp3File.getAudioHeader();

            length = audioHeader.getTrackLength();

            // Check if it has ID3v2 tags, and use those if so
            ID3v24Tag id3v2 = mp3File.getID3v2TagAsv24(); // = new AbstractID3();
            ID3v1Tag id3v1 = null;
            if (id3v2 == null)
            {
                if (C.D)
                    Log.i(TAG, "No id3v2 tag");

                id3v1 = mp3File.getID3v1Tag();
                if (id3v1 == null)
                {
                    if (C.D)
                        Log.i(TAG, "No id3v1 tag, use filename");
                    title = f.getParentFile().getName();
                    author = null;
                    genre = null;
                    track = -1;
                }
                else
                {
                    title = id3v1.getFirst(FieldKey.ALBUM);
                    author = id3v1.getFirst(FieldKey.ARTIST);
                    genre = id3v1.getFirst(FieldKey.GENRE);
                    track = Integer.valueOf(id3v1.getFirstTrack());
                }

                id3v1 = null;
            }
            else
            {
                // Make sure title is not null and also that it contains Latin characters including diacriticals
                // http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html#ubc
                if (id3v2 != null && id3v2.getFirst(ID3v24Frames.FRAME_ID_ALBUM) != null
                        && id3v2.getFirst(ID3v24Frames.FRAME_ID_ALBUM).matches("\\A\\p{ASCII}*\\z"))
                {
                    title = id3v2.getFirst(ID3v24Frames.FRAME_ID_ALBUM);
                }

                if (id3v2 != null && id3v2.getFirst(ID3v24Frames.FRAME_ID_ARTIST) != null
                        && id3v2.getFirst(ID3v24Frames.FRAME_ID_ARTIST).matches("\\A\\p{ASCII}*\\z"))
                {
                    author = id3v2.getFirst(ID3v24Frames.FRAME_ID_ARTIST);
                }

                if (id3v2 != null && id3v2.getFirst(ID3v24Frames.FRAME_ID_GENRE) != null
                        && id3v2.getFirst(ID3v24Frames.FRAME_ID_GENRE).matches("\\A\\p{ASCII}*\\z"))
                {
                    genre = id3v2.getFirst(ID3v24Frames.FRAME_ID_GENRE);
                }

                if (id3v2.getFirst(ID3v24Frames.FRAME_ID_TRACK) != null
                        && !id3v2.getFirst(ID3v24Frames.FRAME_ID_TRACK).equals(""))
                {
                    track = Integer.valueOf(id3v2.getFirst(ID3v24Frames.FRAME_ID_TRACK));
                }

                id3v2 = null;
            }
                          
        }

        boolean found = false;
        for (Book object : mScanList)
        {
            if (title != null && object != null && object.mTitle != null && object.mTitle.equals(title))
            {
                found = true;

                if(track > -1)
                    addTrack(object, track, f, length);
                else
                    addTrackByName(object, f, length);

                //Log("Adding new track #" + id3.getFirst(ID3v24Frames.FRAME_ID_TRACK)
                //        + "  "
                //        + f.getName() + " to book " + object.mTitle);
            }
        }

        mCurrentTitle = title;

        if (!found)
        {
            ImageFileFilter img_filter = new ImageFileFilter(false);

            if (C.D)
                Log.i(TAG, "Found new book: " + title + " in " + f.getParentFile().getName());

            //   setDataText(title);

            Book newBook = new Book();

            File d = f.getParentFile();

            if (C.D)
                Log.i(TAG, "Setting book path: " + d.toString());
            newBook.mBookPath = d.toString(); 

            File[] img_files = d.listFiles(img_filter);
            for (File i : img_files)
            {
                // TODO: Need to limit image size here
                if (C.D)
                    Log.i(TAG, "Found image file: " + i.getAbsolutePath());
                //Bitmap bitmap = BitmapFactory.decodeFile(i.getAbsolutePath());
              //newBook.mCover = bitmap;
                newBook.mCoverPath = i.getAbsolutePath();
                //newBook.mCover = null;
                
                break;
            }

            if(title != null)
            {
                newBook.mTitle = title;
            }
            else
            {
                newBook.mTitle = f.getParentFile().getName(); 
                newBook.mNoTags = true;
            }
            newBook.mAuthor = author;
            newBook.mGenre = genre;

            //Log.i(TAG, "Adding new track #" + id3.getFirst(ID3v24Frames.FRAME_ID_TRACK) + "  "
            //        + f.getName()
            //        + " to book " + newBook.getTitle());
            
            if(track > -1)
                addTrack(newBook, track, f, length);
            else
                addTrackByName(newBook, f, length);
            
            mScanList.add(newBook);
        }

        mp3File = null;
    }

    /**
     * Process directories.
     * 
     * @param dirs the dirs
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws CannotReadException the cannot read exception
     * @throws TagException the tag exception
     * @throws ReadOnlyFileException the read only file exception
     * @throws InvalidAudioFrameException the invalid audio frame exception
     */
    public void processDirectories(List<File> dirs) throws IOException,
            CannotReadException,
            TagException,
            InvalidAudioFrameException
    {

        Filewalker fw = new Filewalker();

        for (File d : dirs)
        {

            fw.walk(d.toString(), null);
        }
    }

    private class Filewalker
    {

        public void walk(String path, String img_path)
        {
            AudioFileFilter filter = new AudioFileFilter();
            //ImageFileFilter img_filter = new ImageFileFilter(false);
            File root = new File(path);
            File[] list = root.listFiles(filter);

            mTotalFilesInBook = list.length;
            mFileProgressInBook = 0;
            // mProgressBar2.setMax(list.length);
            // mProgressBar2.setProgress(0);

            for (File f : list)
            {

                if (f.isDirectory())
                {
                    walk(f.getAbsolutePath(), img_path);
                    if (C.D)
                        Log.i(TAG, "\u001B[34m" + "Dir:" + f.getAbsoluteFile() + "\u001B[m");
                }
                else
                {
                    try
                    {
                        processFile(f, img_path);
                    } catch (CannotReadException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (TagException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InvalidAudioFrameException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mFileProgressInBook++;
                    //       mProgressBar2.incrementProgressBy(1);

                }
            }

            mBookProgress++;
            //mProgressBar1.incrementProgressBy(1);

        }

    }
}
