package org.phpnet.openDrivinCloudAndroid.Loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Decode;
import org.phpnet.openDrivinCloudAndroid.Common.ListFile;
import org.phpnet.openDrivinCloudAndroid.Common.TypeFile;
import org.phpnet.openDrivinCloudAndroid.R;

import java.util.List;

/**
 * Created by clement on 13/10/16.
 */

public class RemoteFileLoader extends AsyncTaskLoader<List<MyFile>> {

    private static final String TAG = "RemoteFileLoader";
    private int orderBy;

    public RemoteFileLoader(Context context, int orderBy) {
        super(context);
        //Log.d(TAG, "RemoteFileLoader: orderBy "+getContext().getString(orderBy));
        this.orderBy = orderBy;
    }

    private RemoteFileLoader(Context c){
        super(c);
    }



    @Override
    public List<MyFile> loadInBackground(){
        Log.d(TAG, "loadInBackground: "+CurrentUser.getInstance().currentDirURL());
        ListFile listFiles = new ListFile();
        listFiles.addAll(Decode.getInstance().getListFile(CurrentUser.getInstance().currentDirURL()));
        try {
            switch (orderBy) {
                case R.string.sortDate:
                    Log.d(TAG, "setSortPreference: date");
                    listFiles.sortDate(false);
                    break;
                case R.string.sortSize:
                    Log.d(TAG, "setSortPreference: size");
                    listFiles.sortSize(false);
                    break;
                case R.string.sortLetter:
                    Log.d(TAG, "setSortPreference: alphabetical");
                    listFiles.sortName(false);
                    break;
            }
        } catch (ListFile.SortException e) {
            e.printStackTrace();
        }

        /*Permet le défilement des images*/
        CurrentUser currentUser = CurrentUser.getInstance();
        currentUser.listImage.clear();
        for (MyFile file : listFiles) {
            if (file.getTypeFile() == TypeFile.IMG)
                currentUser.listImage.add(file);
        }
        Log.d(TAG, "loadInBackground: Finished loading next files");

        return listFiles;
    }


}
