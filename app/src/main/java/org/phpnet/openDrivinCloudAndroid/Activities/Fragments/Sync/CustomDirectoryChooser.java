package org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Sync;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;

import org.phpnet.openDrivinCloudAndroid.R;

/**
 * Created by clement on 14/12/16.
 * Prevent the menu from printing as we don't need it
 */

public class CustomDirectoryChooser extends DirectoryChooserActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setSubtitle(R.string.sync_folder_chooser_subtitle);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return super.onPrepareOptionsMenu(menu);
    }
}
