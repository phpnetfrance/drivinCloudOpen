package org.phpnet.openDrivinCloudAndroid.Protocols;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by clement on 01/03/17.
 */
public class DownloadNotificationReceiver extends BroadcastReceiver{
    private static final String TAG = "DownloadNotificationRec";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: Received event "
        + intent.getAction() + " with download id " + intent.getIntExtra(DrivinCloudDownload.EXTRA_DOWNLOAD_ID, 0));

        if(intent.getAction().equals(DrivinCloudDownload.ACTION_CANCEL_DOWNLOAD)){
            int downloadId = intent.getIntExtra(DrivinCloudDownload.EXTRA_DOWNLOAD_ID, 0);
            if(downloadId > 0){
                Log.d(TAG, "onReceive: Canceling download "+downloadId);
                DrivinCloudDownload dl = DrivincloudDownloadTasks.getInstance().get(downloadId);
                Log.d(TAG, "onReceive: dl.getStatus().toString() = "+dl.getStatus().toString());
                dl.cancel();
                Log.d(TAG, "onReceive - AfterCancel: dl.getStatus().toString() = "+dl.getStatus().toString());
            }else{
                Log.d(TAG, "onReceive: Got a cancel event but invalid download id...");
            }
        }
    }
}
