package org.phpnet.openDrivinCloudAndroid.Protocols;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by clement on 29/12/16.
 */

public class DrivinCloudDownload extends AsyncTask<Void, Integer, Integer>{

    private static final String TAG = "DrivinCloudDownload";
    public static final String ACTION_CANCEL_DOWNLOAD = "org.phpnet.drivinCloudAndroid.ACTION_CANCEL_DOWNLOAD";
    public static final String EXTRA_DOWNLOAD_ID = "org.phpnet.drivinCloudAndroid.EXTRA_DOWNLOAD_ID";
    private BroadcastReceiver receiver;
    protected String url;
    protected ArrayList<DownloadListener> listeners;
    protected CurrentUser user;
    protected HttpClient davClient;
    protected AtomicBoolean cancelDownload;
    protected boolean saved;
    protected File target;
    protected NotificationManager notificationManager;
    protected Notification notification;
    protected int downloadId;
    protected NotificationCompat.Builder notificationBuilder;
    protected Context context;
    protected String contentCompleted;
    protected String contentCanceled;
    protected String title;
    protected String content;
    protected String fileContentType;
    private long lastBroadcast;

    public DrivinCloudDownload(String url, File target) {
        user = CurrentUser.getInstance();
        listeners = new ArrayList<>();
        this.url = url.replaceAll(" ","%20");
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(user.serverURL.getHost());
        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        int maxHostConnections = 20;
        params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
        connectionManager.setParams(params);
        davClient = new HttpClient(connectionManager);
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user.username, user.password);
        davClient.getState().setCredentials(AuthScope.ANY, creds);
        davClient.setHostConfiguration(hostConfig);
        cancelDownload = new AtomicBoolean(false);
        saved = false;
        this.target = target;
        downloadId = (int) (Math.random()*10000);
        Log.d(TAG, "DrivinCloudDownload: dlID="+downloadId);
        fileContentType="application/octet-stream";
        DrivincloudDownloadTasks.getInstance().addDownload(this);
    }

    public Notification getNotification() {
        return notification;
    }

    public void createNotification(Context context, String title, String content, String contentCompleted, String contentCanceled, int icon) {
        this.contentCompleted = contentCompleted;
        this.title = title;
        this.contentCanceled = contentCanceled;
        this.content = content;
        this.context = context;


        Intent intent = new Intent(this.context, DownloadNotificationReceiver.class);
        intent.setAction(ACTION_CANCEL_DOWNLOAD);
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        Log.d(TAG, "createNotification: downloadId = "+intent.getIntExtra(EXTRA_DOWNLOAD_ID, 0));
        PendingIntent pIntent = PendingIntent.getBroadcast(context, (int) (Math.random()*10000), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setProgress(100, 50, true)
                .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_close, context.getString(R.string.download_notification_cancel), pIntent).build());
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(notificationManager!=null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = notificationBuilder.build();
        }else{
            notification = notificationBuilder.getNotification();
        }
        notificationManager.notify(downloadId, notification);
    }

    public int getDownloadId() {
        return downloadId;
    }

    @Override
    protected void onCancelled(Integer integer) {
        Log.d(TAG, "onCancelled: Cancelling...");
        super.onCancelled(integer);
    }

    private int download() throws IOException {
        GetMethod get = new GetMethod(url);
        FileOutputStream fileOS = null;
        long transferedBytes = 0;
        long totalToTransfer = 0;
        int status;
        try {
            status = davClient.executeMethod(get);
            if(isSuccess(status)){
                Log.d(TAG, "download: Starting download of file "+url+" to "+target.getAbsolutePath());
                BufferedInputStream fileReader = new BufferedInputStream(get.getResponseBodyAsStream());
                target.createNewFile();
                fileOS = new FileOutputStream(target);
                Header fileLength = get.getResponseHeader("Content-Length");
                fileContentType = get.getResponseHeader("Content-Type").getValue();
                Log.d(TAG, "download: "+fileLength.toString()+" ("+url+")");
                totalToTransfer = (fileLength != null &&
                        fileLength.getValue().length() >0) ?
                        Long.parseLong(fileLength.getValue()) : 0;

                byte[] boeuf = new byte[4096];
                int res = 0;
                while ((res = fileReader.read(boeuf)) != -1){
                    synchronized (cancelDownload){
                        if(cancelDownload.get() || this.isCancelled()){
                            get.abort();
                            super.cancel(true);

                            Log.d(TAG, "download: Canceled download "+downloadId+" ("+url+")");

                            break;
                        }
                        fileOS.write(boeuf, 0, res);
                        transferedBytes += res;
                        synchronized (listeners) {
                            for (DownloadListener listener :
                                    listeners) {
                                broadcastProgress(transferedBytes, totalToTransfer, url);
                            }
                        }

                    }
                }

                //Download completed?
                if(transferedBytes == totalToTransfer){
                    Log.d(TAG, "download: File download finished ("+url+")");
                    //TODO Report modification time on the local file
                    saved = true;
                    broadcastComplete();
                    get.releaseConnection();
                }

            }else{
                Log.d(TAG, "download: Error while trying to download file "+url+" ["+HttpStatus.getStatusText(status)+"]");
                //Todo handle error
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if(fileOS!=null) fileOS.close();
            //If the file is not 100% downloaded, delete it
            if(!saved && target.exists()){
                Log.d(TAG, "download: Download didn't complete, deleting file. ("+transferedBytes+"/"+totalToTransfer+")");
                target.delete();
                if(notificationBuilder!=null) notificationBuilder
                        .setProgress(0,0,false)
                        .setContentText(contentCanceled);
            }else if(saved && notificationBuilder!=null){
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(target), fileContentType);
                Log.d(TAG, "download: Setting URI to open file "+intent.getDataString()+" of type "+fileContentType.toLowerCase());
                PendingIntent openFileIntent = PendingIntent.getActivity(context, 0, intent, 0);
                //TODO Hide progressbar, may require a custom view...
                notificationBuilder
                        .setContentText(contentCompleted)
                        .setProgress(0,0,false)
                        .setContentIntent(openFileIntent);

            }

            notificationBuilder.mActions.clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notificationManager.notify(downloadId, notificationBuilder.build());
            }else{
                notificationManager.notify(downloadId, notificationBuilder.getNotification());
            }
        }
        return status;
    }

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }

    public void addDownloadListener(DownloadListener listener){
        listeners.add(listener);
    }

    public void removeDownloadListener(DownloadListener listener){
        listeners.remove(listener);
    }

    private void broadcastProgress(long downloadedBytes, long totalBytes, String url){
        //Only update the notification once every 1/4 a sec, otherwise it makes the whole UI Thread lag
        if(lastBroadcast > System.currentTimeMillis()-250) {
            return;
        }
        Log.d(TAG, "broadcastProgress: Brocasting progress to listeners for download "+downloadId);
        lastBroadcast = System.currentTimeMillis();

        //Update android progress listeners
        publishProgress((int) (downloadedBytes/totalBytes)*100);

        //Update notification manager
        if(notificationManager!=null){
            notificationBuilder.setProgress(100, (int)(((float)downloadedBytes/totalBytes)*100), false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notificationManager.notify(downloadId, notificationBuilder.build());
            }else{
                notificationManager.notify(downloadId, notificationBuilder.getNotification());
            }
        }

        for (DownloadListener l:
             listeners) {
            l.progress(downloadedBytes, totalBytes, url);
        }
    }

    private void broadcastComplete(){
        for (DownloadListener l:
            listeners) {
            l.complete();
        }
    }

    public void cancel(){
        cancelDownload.set(true);
        //Update notification
        notificationBuilder.setContentText(contentCanceled);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.mActions.clear(); //Remove the cancel action button, this is currently the only way to do this

        if(notificationManager!=null) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = notificationBuilder.build();
        }else{
            notification = notificationBuilder.getNotification();
        }
        notificationManager.notify(downloadId, notification);
        super.cancel(false);
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            download();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface DownloadListener{
        void progress(long downloadedBytes, long totalBytes, String url);
        void cancel();
        void complete();
    }

    @Override
    protected void onCancelled() {
        cancelDownload.set(true);
        super.cancel(false);
    }
}
