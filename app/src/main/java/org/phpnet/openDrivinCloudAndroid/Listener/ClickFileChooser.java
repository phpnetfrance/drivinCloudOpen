package org.phpnet.openDrivinCloudAndroid.Listener;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.FileChooserActivity;
import org.phpnet.openDrivinCloudAndroid.Adapter.FileChooser;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Decode;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by germaine on 03/07/15.
 */
public class ClickFileChooser {

    private List<MyFile> listFile;
    private Uri currentDirUrl;

    public ClickFileChooser() {
        currentDirUrl = CurrentUser.getInstance().currentDirURL();
        listFile = Decode.getInstance().getListFile(currentDirUrl);
    }


    /*Verification des doublons*/
    private boolean isDouble(String itemName) {
        for (MyFile file : listFile) {
            if (file.name.equals(itemName) || file.name.equals(itemName + "/")) {
                return true;
            }
        }
        return false;
    }


    private Runnable getRunUploadFile(final FileChooser item, final String urlDrive) {
        Runnable runUpload = new Runnable() {
            @Override
            public void run() {
                try {
                    File data = item.getSrc();
                    String path;
                    if (urlDrive.equals(currentDirUrl) && isDouble(item.fileName)) {
                        path = urlDrive + "[" + getCopieIndex(item) + "]" + item.fileName;
                    } else {
                        path = urlDrive + item.fileName;
                    }

                    CurrentUser.getInstance().wdr.putMethod(path, data);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        return runUpload;
    }


    /*
    * Upload de item dans urlDrive
    * */
    private void uploadFile(FileChooser item, String urlDrive) {
        Runnable runUpload = getRunUploadFile(item, urlDrive);
        Thread upload = new Thread(runUpload);
        upload.start();
        try {
            upload.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private int getCopieIndex(FileChooser item) {
        String newName = item.fileName;
        int copie = 1;
        while (isDouble("[" + copie + "]" + newName)) {
            ++copie;
        }
        return copie;
    }

    /*Actualise la liste de l'acceuil après upload*/
    private void upDateAcceuil() {
        FileChooserActivity.currentDir = FileChooserActivity.ROOT;
        Context context = AcceuilActivity.getContext();

        if (FileChooserActivity.getActivity() != null)
            FileChooserActivity.getActivity().finish();
        context.startActivity(new Intent(context, context.getClass()));
    }

    /*
    * Upload UN unique fichier
    * */
    public void onClickFileChooser(final FileChooser item) {
        CurrentUser currentUser = CurrentUser.getInstance();
        if (MyFile.interdit(item.fileName)) {
            currentUser.showToast(FileChooserActivity.getContext(), "Le fichier contient un caractère interdit.\n" +
                    "Les caractères spéciaux autorisés sont - _ @ ] [ & ' . espace");
        } else {
            uploadFile(item, currentUser.currentDirURL().toString());
            upDateAcceuil();
        }
    }


    /*
    * Retourne le nom a donnée au fichier uploader
    * */
    private String getName(FileChooser item, String urlDrive) {
        String name;
        if (urlDrive.equals(currentDirUrl) && isDouble(item.fileName)) {
            name = "[" + getCopieIndex(item) + "]" + item.fileName;
        } else {
            name = item.fileName;
        }
        return name;
    }


    private Runnable getRunUnploadDir(final String urlDrive, final String name) {
        Runnable runUpload = new Runnable() {
            @Override
            public void run() {
                try {
                    CurrentUser.getInstance().wdr.mkcolMethod(urlDrive + name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        return runUpload;
    }

    /*
    * Upload item situé dans urlPhone dans urlDrive
    * */
    private void upload(final FileChooser item, final String urlDrive, final String urlPhone) {
        if (item.isDirectory) {
            final String name = getName(item, urlDrive);
            try {
                Thread threadUpload = new Thread(getRunUnploadDir(urlDrive, name));
                threadUpload.start();
                threadUpload.join();

                for (File file : new File(urlPhone).listFiles()) {
                    if (file.canRead() && !file.isHidden())
                        upload(new FileChooser(file), urlDrive + name + "/", urlPhone + file.getName() + "/");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            uploadFile(item, urlDrive);
        }
    }

    /*
    * Upload la liste des items selectionnés
    * */
    public void onClickFileChooser(List<FileChooser> listItem) {
        for (final FileChooser item : listItem) {
            upload(item, CurrentUser.getInstance().currentDirURL().toString(), item.path);
        }
        upDateAcceuil();
    }
}
