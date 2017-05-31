package org.phpnet.openDrivinCloudAndroid.Listener;

import android.app.AlertDialog;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Navigate;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Decode;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by germaine on 10/07/15.
 */
public class ClickMove {

    private static final String TAG = "ClickMove";
    private View moveLL;
    private List<MyFile> listFile;
    private List<MyFile> filesDouble = new LinkedList<>();
    private List<MyFile> filesUnique = new LinkedList<>();
    private LayoutInflater layoutInflater;
    private AlertDialog alert;
    private Navigate navigate;

    public ClickMove(Navigate navigateFragment) {
        View viewMove = navigateFragment.getActivity().findViewById(R.id.move_bar);
        TextView nbElems = (TextView) viewMove.findViewById(R.id.move_nb_elements);

        this.moveLL = viewMove;
        this.listFile = navigateFragment.getListFile();
        this.layoutInflater = navigateFragment.getLayoutInflater(null);
        this.navigate = navigateFragment;
        nbElems.setText(
                navigate.getContext().getResources().getQuantityString(
                        R.plurals.nb_elems_move,
                        CurrentUser.getInstance().listSelectedFiles.size(),
                        CurrentUser.getInstance().listSelectedFiles.size()
                )
        );
        viewMove.setVisibility(View.VISIBLE);
    }


    private void refresh() {
        ArrayList<MyFile> l = null;
        l = Decode.getInstance().getListFile(CurrentUser.getInstance().currentDirURL());
        navigate.updateListFile(l);
        moveLL.setVisibility(View.GONE);
    }


    private void update() {
        filesDouble.remove(0);
        alert.dismiss();
        if (filesDouble.size() > 0) {
            createAlertDialog(filesDouble.get(0));
        } else {
            refresh();
        }
    }


    private View.OnClickListener listenerOmettre = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            update();
        }
    };


    private View.OnClickListener listenerOmettreTout = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            alert.dismiss();
            refresh();
        }
    };


    private final Runnable runRemplace = new Runnable() {
        @Override
        public void run() {
            try {
                CurrentUser currentUser = CurrentUser.getInstance();
                String path = currentUser.currentDirURL() + filesDouble.get(0).name;
                String source = currentUser.getCurrentDirMoveURL() + filesDouble.get(0).name;
                String destination = currentUser.currentDirURL() + filesDouble.get(0).name;
                String tmpDir = currentUser.serverURL.toString() + new Date().getTime() + "/";
                currentUser.wdr.mkcolMethod(tmpDir);
                currentUser.wdr.moveMethod(source, tmpDir + filesDouble.get(0).name);
                currentUser.wdr.deleteMethod(path);
                currentUser.wdr.moveMethod(tmpDir + filesDouble.get(0).name, destination);
                currentUser.wdr.deleteMethod(tmpDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    private View.OnClickListener listenerRemplacer = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                Thread threadRemplace = new Thread(runRemplace);
                threadRemplace.start();
                threadRemplace.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            update();
        }
    };


    private View.OnClickListener listenerRemplacerTout = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            alert.dismiss();
            for (int i = 0, n = filesDouble.size(); i < n; ++i) {
                try {
                    Thread threadRemplace = new Thread(runRemplace);
                    threadRemplace.start();
                    threadRemplace.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                filesDouble.remove(0);
            }
            refresh();
        }
    };


    private void createAlertDialog(MyFile file) {
        alert = new AlertDialog.Builder(AcceuilActivity.getContext()).create();

        View view = layoutInflater.inflate(R.layout.alert_dialog, null);
        view.findViewById(R.id.button_omettre).setOnClickListener(listenerOmettre);
        view.findViewById(R.id.button_remplacer).setOnClickListener(listenerRemplacer);
        view.findViewById(R.id.button_omettre_tout).setOnClickListener(listenerOmettreTout);
        view.findViewById(R.id.button_remplacer_tout).setOnClickListener(listenerRemplacerTout);

        alert.setView(view);
        alert.setMessage("Ce dossier contient déjà un " + file.getType() + " " + file.name);
        alert.show();
    }


    private MyFile contains(MyFile selectedFile) {
        for (MyFile file : listFile) {
            if (file.same(selectedFile))
                return file;
        }
        return null;
    }


    private Runnable getRunMove(final MyFile fileSelected) {
        Runnable runMove = new Runnable() {
            @Override
            public void run() {
                CurrentUser currentUser = CurrentUser.getInstance();
                String source = currentUser.getCurrentDirMoveURL() + fileSelected.name;
                String destination = null;
                destination = currentUser.currentDirURL() + fileSelected.name;
                try {
                    currentUser.wdr.moveMethod(source, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        return runMove;
    }

    private void decoupe() {
        listFile = navigate.getListFile();
        for (MyFile file : CurrentUser.getInstance().listSelectedFiles) {
            MyFile doubleFile = contains(file);
            if (doubleFile != null) {
                Log.d(TAG, "decoupe: double detected "+file.getUrl()+file.name);
                filesDouble.add(file);
            } else {
                filesUnique.add(file);
            }
        }
    }

    private View.OnClickListener listenerMove = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CurrentUser.getInstance().move = false;
            CurrentUser user = CurrentUser.getInstance();
            Log.d(TAG, "onClickMove from "+user.getCurrentDirMoveURL()+" to "+user.currentDirURL());
            decoupe();
            for (MyFile selectedFile : filesUnique) {
                try {
                    Thread threadMove = new Thread(getRunMove(selectedFile));
                    threadMove.start();
                    threadMove.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (filesDouble.size() > 0) {
                createAlertDialog(filesDouble.get(0));
            } else {
                refresh();
            }
        }

    };

    private View.OnClickListener listenerCancel = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            moveLL.setVisibility(View.GONE);
            CurrentUser.getInstance().move = false;
        }
    };

    /*
    * Retoure true si le déplacement est possible
    * false sinon
    * */
    private boolean possibleMove() {
        CurrentUser currentUser = CurrentUser.getInstance();
        String source = currentUser.getCurrentDirMoveURL();
        String destination = null;
        destination = currentUser.currentDirURL().toString();

        if (source.equals(destination))
            return false;

        for (MyFile file : currentUser.listSelectedFiles) {
            if (!file.isDir())
                continue;
            String srcDir = currentUser.getCurrentDirMoveURL() + file.name;
            String dstDir = null;
            dstDir = currentUser.currentDirURL().toString();
            if (srcDir.length() <= dstDir.length()) {
                String prefix = dstDir.substring(0, srcDir.length());
                if (prefix.equals(srcDir)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void onClickMove() {

        //Barre avec les boutons déplacer et annuler
        View move = moveLL.findViewById(R.id.move_action);
        if (possibleMove()) {
            ((TextView) move).setTextColor(navigate.getContext().getResources().getColor(R.color.colorAccent));
            move.setOnClickListener(listenerMove);
        } else {
            ((TextView) move).setTextColor(Color.GRAY);
        }

        View cancel = moveLL.findViewById(R.id.cancel_action);
        cancel.setOnClickListener(listenerCancel);
    }
}
