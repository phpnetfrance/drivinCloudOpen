package org.phpnet.openDrivinCloudAndroid.Activities;

import android.os.Environment;
import android.support.annotation.Nullable;

import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.CustomNNFilePickerFragment;

import java.io.File;

/**
 * Created by clement on 16/12/16.
 */

public class CustomNNFilePickerActivity extends AbstractFilePickerActivity {
    public CustomNNFilePickerActivity() {
        super();
    }

    @Override
    protected AbstractFilePickerFragment getFragment(@Nullable String startPath, int mode, boolean allowMultiple, boolean allowCreateDir, boolean allowExistingFile, boolean singleClick) {
        // Load our custom fragment here
        AbstractFilePickerFragment<File> fragment = new CustomNNFilePickerFragment();
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
        fragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
        return fragment;
    }
}
