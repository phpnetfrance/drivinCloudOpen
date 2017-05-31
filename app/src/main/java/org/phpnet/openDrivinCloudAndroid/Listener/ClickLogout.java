package org.phpnet.openDrivinCloudAndroid.Listener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.LoginActivity;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.R;

/**
 * Created by germaine on 17/07/15.
 */
public class ClickLogout {
    private ClickLogout() {
    }

    protected static final ClickLogout instance = new ClickLogout();

    public static ClickLogout getInstance() {
        return instance;
    }

    private Runnable getRunLogout() {
        Runnable logout = new Runnable() {
            @Override
            public void run() {
                CurrentUser currentUser = CurrentUser.getInstance();
                currentUser.logout();
            }
        };
        return logout;
    }

    public void onClickLogout(){
        onClickLogout(false);
    }

    public void onClickLogout(@Nullable boolean force) {
        if(force){
            Thread threadLogout = new Thread(getRunLogout());
            threadLogout.start();
            try {
                threadLogout.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            AlertDialog.Builder builder = new AlertDialog.Builder(AcceuilActivity.getContext());
            builder.setMessage(R.string.dialog_confirm_logout);
            builder.setTitle(R.string.logout);
            final boolean userAnswer = false;
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    AcceuilActivity.getActivity().startActivity(new Intent(AcceuilActivity.getContext(), LoginActivity.class)); //Go moveBack to login screen
                    AcceuilActivity.getActivity().finish(); //Finish this activity
                    try {
                        Thread threadLogout = new Thread(getRunLogout());
                        threadLogout.start();
                        threadLogout.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            builder.show();
        }
    }
}
