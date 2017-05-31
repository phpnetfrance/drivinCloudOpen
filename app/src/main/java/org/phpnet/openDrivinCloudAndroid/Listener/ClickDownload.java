package org.phpnet.openDrivinCloudAndroid.Listener;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Decode;
import org.phpnet.openDrivinCloudAndroid.Protocols.DrivinCloudDownload;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by germaine on 01/07/15.
 */
public class ClickDownload {
    private static final String TAG = "ClickDownload";

    private ClickDownload() {
    }

    protected static final ClickDownload instance = new ClickDownload();

    private static final Context context = AcceuilActivity.getContext();

    public static ClickDownload getInstance() {
        return instance;
    }

    private String getPath() {
        String state = Environment.getExternalStorageState();
        if (state != null && state.equals(Environment.MEDIA_MOUNTED)) {
            /*Media valable en lecture et écriture*/
            File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            return download.getAbsolutePath();
        } else {
            return null;
        }
    }

    private void downloadFile(String downloadDir, String nameFile, Uri urlFile) {
        /*try {
            File target = new File(downloadDir + "/" + nameFile);
            target.createNewFile();
            CurrentUser.getInstance().wdr.getMethod(urlFile, target);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        File target = new File(downloadDir + "/" + nameFile);

        DrivinCloudDownload dl = new DrivinCloudDownload(urlFile.toString(), target);
        dl.addDownloadListener(new DrivinCloudDownload.DownloadListener() {
            @Override
            public void progress(long downloadedBytes, long totalBytes, String url) {
                Log.d(TAG, "progress: "+downloadedBytes+"/"+totalBytes+" ("+((float)downloadedBytes/totalBytes)*100+"%"+")");
            }

            @Override
            public void cancel() {
                Log.d(TAG, "cancel: Received cancel event");
            }
        });
        Log.d(TAG, "downloadFile: Starting download of file "+urlFile);
        dl.createNotification(context, nameFile, context.getString(R.string.downloading), context.getString(R.string.download_finished), context.getString(R.string.download_canceled), R.drawable.ic_drivincloud_notificon);
        dl.execute();


    }


    /*
    * String downloadDir : dossier
    * */
    private void downloadDir(String downloadDir, String name, Uri url) {
        File root = new File(downloadDir + "/" + name);
        root.mkdir();
        ArrayList<MyFile> listFile = null;
        listFile = Decode.getInstance().getListFile(url.buildUpon().appendPath(name).build());
        for (MyFile file : listFile) {
            if (file.isDir()) {
                //downloadDir(downloadDir + "/" + name, file.name, url + name + "/");
                downloadDir(downloadDir + "/" + name, file.name, url.buildUpon().appendPath(name).build());
            } else {
                //downloadFile(downloadDir + "/" + name, file.name, url + name + "/" + file.name);
                downloadFile(downloadDir+ "/" +name, file.name, url.buildUpon().appendPath(name).appendPath(file.name).build());
            }
        }
    }


    /*extention zip tar to add...*/
    private String getExtention(MyFile item) {
        switch (item.getTypeFile()) {
            case TXT:
                return "text/*";
            case PDF:
                return "application/pdf";
            case IMG:
                return "image/jpeg";
            case DIR:
                return "vnd.android.cursor.dir/*";
            case AUDIO:
                return "audio/*";
            case NOTYPE:
            default:
        }
        return "nop";
    }


    public void onClickDownload(final List<MyFile> selectedFiles) {
        final CurrentUser currentUser = CurrentUser.getInstance();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Iterator<MyFile> it = selectedFiles.iterator();
                    while (it.hasNext()) {
                        MyFile item = it.next();
                        String downloadDir = getPath();
                        String pathFile = downloadDir + "/" + item.name;

                        if (item.isDir()) {
                            downloadDir(getPath(), item.name, currentUser.currentDirURL());
                        } else {
                            downloadFile(downloadDir, item.name, currentUser.currentDirURL().buildUpon().appendPath(item.name).build());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        new Thread(runnable).start();
        currentUser.showToast(context, context.getResources().getString(R.string.download));
    }

    public void onClickDownload(MyFile file) {
        List<MyFile> list = new ArrayList<MyFile>();
        list.add(file);
        onClickDownload(list);
    }
}
