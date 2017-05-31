package org.phpnet.openDrivinCloudAndroid.Listener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v7.view.ActionMode;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Navigate;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by germaine on 01/07/15.
 */
public class ClickDelete {
    private static final String TAG = "ClickDelete";

    private ClickDelete() {
    }

    protected static final ClickDelete instance = new ClickDelete();

    public static ClickDelete getInstance() {
        return instance;
    }


    /*Thread correspondant à l'action d'une confirmation de suppression*/
    private Thread deleteYes(final List<MyFile> selectedFiles) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (MyFile item : selectedFiles) {
                        CurrentUser currentUser = CurrentUser.getInstance();
                        String target = Uri.decode(currentUser.currentDirURL().buildUpon().appendPath(item.name).build().toString());
                        Log.d(TAG, "run: deleteMethod("+target+")");
                        currentUser.wdr.deleteMethod(target);

                        /*Update arbre de recherche*/
                        //CurrentUser.getInstance().racine.remove(item);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return thread;
    }


    /*Affiche une boite de dialogue pour demander confirmation
    et supprime en cas de confirmation
    * */
    public void onClickDelete(final List<MyFile> selectedFiles, final Navigate navigateFragment, final ActionMode mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AcceuilActivity.getContext());
        final String s = (selectedFiles.size() > 1) ? "s" : "";
        builder.setMessage(selectedFiles.size() + " élement" + s + " sélectionné" + s + ".\nValider la suppression ?");
        builder.setTitle("Suppression");

        /*Défini l'action du boutton Non*/
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //CurrentUser.getInstance().showToast(AcceuilActivity.getContext(), AcceuilActivity.getContext().getString(R.string.noDelete));
            }
        });

        /*Défini l'action du boutton Oui*/
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Thread thread = deleteYes(selectedFiles);
                    thread.start();
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CurrentUser.getInstance().showToast(AcceuilActivity.getContext(), selectedFiles.size() + " élement" + s + " supprimé" + s);
                ArrayList<MyFile> newListFile = new ArrayList<>();
                for (MyFile f : navigateFragment.getListFile()) {
                    if (!selectedFiles.contains(f))
                        newListFile.add(f);
                }
                navigateFragment.updateListFile(newListFile);
                mode.finish();
            }
        });

        builder.show();
    }

}
