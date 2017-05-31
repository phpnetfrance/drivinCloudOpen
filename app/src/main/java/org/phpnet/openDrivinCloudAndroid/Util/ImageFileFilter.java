package org.phpnet.openDrivinCloudAndroid.Util;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by clement on 16/12/16.
 */

public class ImageFileFilter implements FileFilter
{
    File file;
    private final String[] okFileExtensions =  new String[] {"jpg", "png", "gif","jpeg"};

    /**
     *
     */
    public ImageFileFilter(File newfile)
    {
        this.file=newfile;
    }

    public boolean accept(File file)
    {
        for (String extension : okFileExtensions)
        {
            if (file.getName().toLowerCase().endsWith(extension))
            {
                return true;
            }
        }
        return false;
    }

}
