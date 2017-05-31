package org.phpnet.openDrivinCloudAndroid.Listener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.EditText;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Decode;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by germaine on 23/07/15.
 */
public class ClickCreateFile {
    private static final String TAG = "ClickCreateFile";

    private ClickCreateFile() {
    }

    protected static final ClickCreateFile instance = new ClickCreateFile();

    public static ClickCreateFile getInstance() {
        return instance;
    }

    private String nameString;

    private Runnable runCreateFile = new Runnable() {
        @Override
        public void run() {
            try {
                CurrentUser currentUser = CurrentUser.getInstance();
                File createFile = new File(currentUser.appDir + "/createFile");
                if (!createFile.exists()) {
                    createFile.createNewFile();
                }
                boolean success = currentUser.wdr.putMethod(Uri.decode(currentUser.currentDirURL().buildUpon().appendPath(nameString).build().toString()), createFile);
                Log.d(TAG, "run: putMethod("+Uri.decode(currentUser.currentDirURL().buildUpon().appendPath(nameString).build().toString())+", createFile)");
                Log.d(TAG, "run: Got response code "+currentUser.wdr.getStatusCode()+" ("+currentUser.wdr.getStatusMessage()+")");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public void onClickCreateFile(final List<MyFile> listFile) {
        final Context context = AcceuilActivity.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Nom du document : ");
        final EditText nameFile = new EditText(context);
        nameFile.setText("nouveau.txt");
        builder.setView(nameFile);

        builder.setMessage("Les caractères spéciaux autorisés sont : - _ @ ] [ & ' . espace");

        /*Défini l'action du boutton Non*/
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        /*Défini l'action du boutton Oui*/
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                nameString = nameFile.getText().toString();
                CurrentUser currentUser = CurrentUser.getInstance();
                for (MyFile file : listFile) {
                    if (file.name.equals(nameString) || file.name.equals(nameString + "/")) {
                        currentUser.showToast(context, "Un élement de même nom existe déjà dans ce repertoire.");
                        onClickCreateFile(listFile);
                        return;
                    }
                }
                if (MyFile.interdit(nameString)) {
                    currentUser.showToast(context, "Un caractère interdit a été utilisé.\nErreur de création.");
                    onClickCreateFile(listFile);
                    return;
                }
                try {
                    Thread threadCreateFile = new Thread(runCreateFile);
                    threadCreateFile.start();
                    threadCreateFile.join();

                    //Mise à jour de la liste après création du fichier
                    AcceuilActivity.getActivity().getNavigateFragment().setListFile(Decode.getInstance().getListFile(currentUser.currentDirURL()));
                    AcceuilActivity.getActivity().getNavigateFragment().setSortPreference();
                    AcceuilActivity.getActivity().updateListFile();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        builder.show();
    }
}

