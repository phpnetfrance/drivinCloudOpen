package org.phpnet.openDrivinCloudAndroid.Listener;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;

import java.io.File;
import java.io.IOException;

/**
 * Created by germaine on 01/07/15.
 */
public class ClickFile {
    private static final String TAG = "ClickFile";
    //Chememin du fichier dans le drive
    private String URL;

    //Type du fichier (que audio ou pdf pour l'instant)
    //Extention possible
    private String mmie;

    //Nom
    private String name;

    private File file;

    public ClickFile(String URL, String name, String mmie) {
        this.URL = URL;
        this.name = name;
        this.mmie = mmie;
    }


    private Runnable getRunDownload() {
        Runnable runDownload = new Runnable() {
            @Override
            public void run() {
                try {
                    CurrentUser curr = CurrentUser.getInstance();
                    file = new File(curr.appDir + name);
                    file.createNewFile();
                    Log.d(TAG, "run: Downloaded file will be : "+file.getAbsolutePath());
                    file.setReadable(true, false);
                    curr.wdr.getMethod(URL, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        return runDownload;
    }

    public void onClickFile() {
        try {
            Log.d(TAG, "onClickFile: Downloading "+URL+"/"+name+" to "+file);
            Thread threadDownload = new Thread(getRunDownload());
            threadDownload.start();
            threadDownload.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Uri path = Uri.fromFile(file);

        Log.d(TAG, "onClickFile: Finished download of "+path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, mmie);

        try {
            AcceuilActivity.getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            CurrentUser.getInstance().showToast(AcceuilActivity.getContext(), "Aucune application disponible pour visionner le fichier.");
        }
    }


}
