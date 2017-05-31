package org.phpnet.openDrivinCloudAndroid.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Uploads.TabManualUploads;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Uploads.TabSyncUploads;

/**
 * Created by clement on 21/07/16.
 */
public class UploadsPagerAdapter extends FragmentStatePagerAdapter{
    private static final String TAG = UploadsPagerAdapter.class.getSimpleName();
    public static final int ID_TAB_MANUALUPLOADS = 0;
    public static final int ID_TAB_SYNCUPLOADS = 1;
    int nbTabs;
    private TabSyncUploads tabSyncUpl;
    private TabManualUploads tabManuUpl;

    public UploadsPagerAdapter(FragmentManager fm, int nbTabs) {
        super(fm);
        this.nbTabs = nbTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                if(tabManuUpl == null)
                    tabManuUpl = new TabManualUploads();
                return tabManuUpl;
            case 1:
                if(tabSyncUpl==null)
                    tabSyncUpl = new TabSyncUploads();
                return tabSyncUpl;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return nbTabs;
    }
}
