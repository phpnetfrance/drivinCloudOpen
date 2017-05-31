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

import java.io.IOException;
import java.util.List;

/**
 * Created by germaine on 23/07/15.
 */
public class ClickCreateFolder {
    private static final String TAG = "ClickCreateFolder";

    private ClickCreateFolder() {
    }

    protected static final ClickCreateFolder instance = new ClickCreateFolder();

    public static ClickCreateFolder getInstance() {
        return instance;
    }

    private String nameString;

    private Runnable runCreateDir = new Runnable() {
        @Override
        public void run() {
            try {
                CurrentUser currentUser = CurrentUser.getInstance();
                Log.d(TAG, "run: mkcol("+Uri.decode(currentUser.currentDirURL().buildUpon().appendPath(nameString).build().toString())+"/)");
                currentUser.wdr.mkcolMethod(Uri.decode(currentUser.currentDirURL().buildUpon().appendPath(nameString).build().toString())+"/");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public void onClickCreateFolder(final List<MyFile> listFile) {
        final Context context = AcceuilActivity.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Nom du dossier : ");
        final EditText nameDir = new EditText(context);
        nameDir.setText("Nouveau dossier");
        builder.setView(nameDir);

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
                nameString = nameDir.getText().toString();
                CurrentUser currentUser = CurrentUser.getInstance();
                for (MyFile file : listFile) {
                    if (file.name.equals(nameString) || file.name.equals(nameString + "/")) {
                        currentUser.showToast(context, "Un élement de même nom existe déjà dans ce repertoire.");
                        onClickCreateFolder(listFile);
                        return;
                    }
                }
                if (MyFile.interdit(nameString)) {
                    currentUser.showToast(context, "Un caractère interdit a été utilisé.\nErreur de création.");
                    onClickCreateFolder(listFile);
                    return;
                }
                try {
                    Thread threadCreateDir = new Thread(runCreateDir);
                    threadCreateDir.start();
                    threadCreateDir.join();

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
