/**
 * TODO
 *
 * <h4>Description</h4>
 * TODO
 *
 * <h4>Notes</h4>
 * TODO
 *
 * <h4>References</h4>
 * TODO
 *
 * @author      $Author$
 *
 * @version     $Rev$
 *
 * @see         TODO
 */

package com.transcendcode.earful;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import android.util.Log;

public class ImageFileFilter implements FileFilter
{

    protected static final String TAG = "ImageFileFilter";
    /**
     * allows Directories
     */
    private final boolean allowDirectories;

    public ImageFileFilter(boolean allowDirectories)
    {
        this.allowDirectories = allowDirectories;
    }

    @Override
    public boolean accept(File f)
    {
        if (f.isHidden() || !f.canRead())
        {
            return false;
        }

        if (f.isDirectory())
        {
            return checkDirectory(f);
        }
        return checkFileExtension(f);
    }

    private boolean checkFileExtension(File f)
    {
        String ext = getFileExtension(f);
        if (ext == null)
            return false;
        try
        {
            if (SupportedFileFormat.valueOf(ext.toUpperCase()) != null)
            {
                return true;
            }
        } catch (IllegalArgumentException e)
        {
            //Not known enum value
            return false;
        }
        return false;
    }

    private boolean checkDirectory(File dir)
    {
        if (!allowDirectories)
        {
            return false;
        }
        else
        {
            final ArrayList<File> subDirs = new ArrayList<File>();
            int songNumb = dir.listFiles(new FileFilter()
            {

                @Override
                public boolean accept(File file)
                {
                    if (file.isFile())
                    {
                        if (file.getName().equals(".nomedia"))
                            return false;

                        return checkFileExtension(file);
                    }
                    else if (file.isDirectory())
                    {
                        subDirs.add(file);
                        return false;
                    }
                    else
                        return false;
                }
            }).length;

            if (songNumb > 0)
            {
                if (C.D)
                    Log.i(TAG, "checkDirectory: dir " + dir.toString() + " return true con songNumb -> " + songNumb);
                return true;
            }

            for (File subDir : subDirs)
            {
                if (checkDirectory(subDir))
                {
                    if (C.D)
                        Log.i(TAG, "checkDirectory [for]: subDir " + subDir.toString() + " return true");
                    return true;
                }
            }
            return false;
        }
    }

    /*
    private boolean checkFileExtension( String fileName ) {
        String ext = getFileExtension(fileName);
        if ( ext == null) return false;
        try {
            if ( SupportedFileFormat.valueOf(ext.toUpperCase()) != null ) {
                return true;
            }
        } catch(IllegalArgumentException e) {
            //Not known enum value
            return false;    
        }
        return false; 
    }
    */

    public String getFileExtension(File f)
    {
        return getFileExtension(f.getName());
    }

    public String getFileExtension(String fileName)
    {
        int i = fileName.lastIndexOf('.');
        if (i > 0)
        {
            return fileName.substring(i + 1);
        }
        else
            return null;
    }

    /**
     * Files formats currently supported by Library
     */
    public enum SupportedFileFormat
    {
        PNG("png"),
        JPEG("jpeg"),
        JPG("jpg");

        private String filesuffix;

        SupportedFileFormat(String filesuffix)
        {
            this.filesuffix = filesuffix;
        }

        public String getFilesuffix()
        {
            return filesuffix;
        }
    }

}
