package org.phpnet.openDrivinCloudAndroid.Protocols;

import java.util.HashMap;

/**
 * Created by clement on 02/03/17.
 *
 * Simple singleton to keep the Download list for further operations on downloads
 */

public class DrivincloudDownloadTasks {
    private static DrivincloudDownloadTasks mInstance;
    private HashMap<Integer, DrivinCloudDownload> downloads;

    private DrivincloudDownloadTasks(){
        downloads = new HashMap<>();
    };

    public static DrivincloudDownloadTasks getInstance(){
        if(mInstance == null){
            mInstance = new DrivincloudDownloadTasks();
        }
        return mInstance;
    }

    public void addDownload(DrivinCloudDownload download){
        downloads.put(download.getDownloadId(), download);
    }

    public DrivinCloudDownload get(int downloadId){
        return downloads.get(downloadId);
    }
}
