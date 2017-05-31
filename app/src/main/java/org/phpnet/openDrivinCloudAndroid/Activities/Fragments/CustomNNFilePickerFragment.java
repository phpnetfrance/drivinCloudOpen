package org.phpnet.openDrivinCloudAndroid.Activities.Fragments;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.nononsenseapps.filepicker.FilePickerFragment;
import com.nononsenseapps.filepicker.LogicHandler;

import org.phpnet.openDrivinCloudAndroid.R;

import java.io.File;

/**
 * Created by clement on 16/12/16.
 */

public class CustomNNFilePickerFragment extends FilePickerFragment {
    private static final String TAG = "CustomNNFilePickerFragm";
    /**
     * @param parent Containing view
     * @param viewType which the ViewHolder will contain. Will be one of:
     * [VIEWTYPE_HEADER, VIEWTYPE_CHECKABLE, VIEWTYPE_DIR]. It is OK, and even expected, to use the same
     * layout for VIEWTYPE_HEADER and VIEWTYPE_DIR.
     * @return a view holder for a file or directory (the difference is presence of checkbox).
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case LogicHandler.VIEWTYPE_HEADER:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.filepicker_listitem_dir,
                        parent, false);
                return new HeaderViewHolder(v);
            case LogicHandler.VIEWTYPE_CHECKABLE:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.filepicker_photo_listitem_checkable,
                        parent, false);
                return new CheckableViewHolder(v);
            case LogicHandler.VIEWTYPE_DIR:
            default:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.filepicker_listitem_dir,
                        parent, false);
                return new DirViewHolder(v);
        }
    }


    /**
     * This method is rewritten to generate preview from image files
     * @param vh       to bind data from either a file or directory
     * @param position 0 - n, where the header has been subtracted
     * @param data     the file or directory which this item represents
     */
    @Override
    public void onBindViewHolder(@NonNull DirViewHolder vh, int position, @NonNull File data) {
        vh.file = data;
        vh.icon.setVisibility( isDir(data) ? View.VISIBLE : View.GONE);
        vh.text.setText(getName(data));

        if (isCheckable(data)) {
            if (mCheckedItems.contains(data)) {
                mCheckedVisibleViewHolders.add((CheckableViewHolder) vh);
                ((CheckableViewHolder) vh).checkbox.setChecked(true);
            } else {
                //noinspection SuspiciousMethodCalls
                mCheckedVisibleViewHolders.remove(vh);
                ((CheckableViewHolder) vh).checkbox.setChecked(false);
            }

            //Set image preview
            if(isPreviewable(data)){
                ImageView preview = (ImageView) vh.icon;
                Glide.with(this).load(data).into(preview);
                preview.setVisibility(View.VISIBLE);
            }
        }
    }

    public boolean isPreviewable(@NonNull final File path) {
        String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path.getAbsolutePath()));
        return mimetype!=null && path.exists() && (mimetype.contains("image") || mimetype.contains("video"));
    }
}
