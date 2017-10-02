package org.phpnet.openDrivinCloudAndroid.Listener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.view.ActionMode;
import android.widget.EditText;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Navigate;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by germaine on 01/07/15.
 */
public class ClickRename {
    private static final String TAG = "ClickRename";

    private ClickRename() {
    }

    protected static final ClickRename instance = new ClickRename();

    public static ClickRename getInstance() {
        return instance;
    }


    private boolean exits(String newName, List<MyFile> listFile) {
        for (MyFile file : listFile) {
            if (file.name.equals(newName) || file.name.equals(newName + "/"))
                return false;
        }
        return true;
    }


    public void onClickRename(final MyFile file, final Navigate navigate, final ActionMode mode) {
        final ArrayList<MyFile> listFile = navigate.getListFile();
        final Context context = AcceuilActivity.getContext();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Renommage");

        builder.setMessage("Veuillez spécifier un nouveau nom pour le " + file.getType() + " '" + file.name + "'." +
                "\nLes caractères spéciaux autorisés sont - _ @ ] [ & ' . espace");

        final EditText newName = new EditText(context);
        newName.setText((file.isDir()) ? file.name.substring(0, file.name.length() - 1) : file.name);
        builder.setView(newName);

        /*Défini l'action du boutton Non*/
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        final Runnable runRename = new Runnable() {
            @Override
            public void run() {
                CurrentUser currentUser = CurrentUser.getInstance();
                String path = null;
                try {
                    path = URLDecoder.decode(currentUser.currentDirURL().toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    String oldName = new String(file.name);
                    currentUser.wdr.moveMethod(path + "/" + oldName, path + "/" + newName.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        /*Défini l'action du boutton Oui*/
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CurrentUser currentUser = CurrentUser.getInstance();
                try {
                    String newNameFile = newName.getText().toString();

                    if (file.name.equals(newNameFile) || newNameFile.equals("")) {
                        return;
                    }

                    /*Cas des caracteres interdit*/
                    if (MyFile.interdit(newNameFile)) {
                        currentUser.showToast(context, "Un caractère interdit a été utilisé.\nErreur de renommage.");
                        onClickRename(file, navigate, mode);
                        return;
                    }

                    if (exits(newNameFile, listFile)) {
                        Thread threadRename = new Thread(runRename);
                        threadRename.start();
                        threadRename.join();
                        listFile.get(listFile.indexOf(file)).name = newNameFile + ((file.isDir()) ? "/" : "");
                        listFile.get(listFile.indexOf(file)).updateType();
                        navigate.updateListFile(listFile);
                        mode.finish();
                    } else {
                        //Un fichier de meme nom existe déjà
                        String msgErr = "Impossible de renommer le " + file.getType() + " car ce nom est déjà utilisé.";
                        currentUser.showToast(context, msgErr);
                        onClickRename(file, navigate, mode);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    currentUser.showToast(context, "Erreur de renommage.");
                }
            }
        });

        builder.show();
    }
}
