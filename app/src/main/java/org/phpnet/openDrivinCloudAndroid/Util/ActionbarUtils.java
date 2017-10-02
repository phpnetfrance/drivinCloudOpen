package org.phpnet.openDrivinCloudAndroid.Util;

import android.app.Activity;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.R;

/**
 * Created by clement on 28/10/16.
 */

public class ActionbarUtils {
    private static final String TAG = "Toolbar";
    /**
     * Little fix for action bar btn size
     * Will probably not be needed anymore and further apis
     * @param activity
     * @param actionBar
     * @param isUp set to true to draw the moveBack button
     */
    public static void setHomeAsUpIndicator(Activity activity, ActionBar actionBar, boolean isUp){
        if(isUp){
            actionBar.setHomeAsUpIndicator(R.mipmap.back_logo);
        }else{
            actionBar.setHomeAsUpIndicator(R.mipmap.back_logo_nude);
        }
        Log.d(TAG, "fixHomeButtonSize: Done...");
    }
}
