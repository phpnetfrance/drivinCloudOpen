package org.phpnet.openDrivinCloudAndroid.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.ImageActivityPlaceHolderFragment;

import java.util.List;

/**
 * Created by clement on 05/07/16.
 */
public class ImageViewerAdapter extends FragmentPagerAdapter{
    private static final String TAG = "ImageViewerAdapter";

    private List<MyFile> listImage;


    public ImageViewerAdapter(FragmentManager fm, List<MyFile> listImage) {
        super(fm);
        this.listImage = listImage;
    }


    @Override
    public Fragment getItem(int position) {
        MyFile image = listImage.get(position);
        Log.d(TAG, "getItem: "+image);
        ImageActivityPlaceHolderFragment tmp = ImageActivityPlaceHolderFragment.newInstance(
                position,
                image.getName(),
                image.getUrl().buildUpon().appendPath(image.getName()).build().toString()
        );
        Log.d(TAG, "getItem: "+tmp);
        return tmp;
    }

    @Override
    public int getCount() {
        return listImage.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return listImage.get(position).getName();
    }
}
