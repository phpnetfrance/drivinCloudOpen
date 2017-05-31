package org.phpnet.openDrivinCloudAndroid.Activities.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.InputStream;

/**
 * Created by clement on 27/10/16.
 */

public class ImageActivityPlaceHolderFragment extends Fragment {
    private static final String TAG = "ImageActivityPlaceHolde";

    String name;
    String imageURL;
    int position;

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_IMG_TITLE = "image_title";
    private static final String ARG_IMG_LOC = "image_location";

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        this.position = args.getInt(ARG_SECTION_NUMBER);
        this.name = args.getString(ARG_IMG_TITLE);
        this.imageURL = args.getString(ARG_IMG_LOC);
    }

    public static ImageActivityPlaceHolderFragment newInstance(int sectionNumber, String name, String imageLocation) {
        ImageActivityPlaceHolderFragment fragment = new ImageActivityPlaceHolderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(ARG_IMG_TITLE, name);
        args.putString(ARG_IMG_LOC, imageLocation);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.page_item_image, container, false);

        ImageView imageView = (ImageView) rootView.findViewById(R.id.detail_image);
        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.imageLoading);

        Glide.get(getActivity()).register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(CurrentUser.getInstance().getOkHttpClient()));
        Log.d(TAG, "onCreateView: Image info: "+this);
        Glide.with(getActivity())
                .load(imageURL)
                .error(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_error_outline_black_24dp, null))
                .crossFade()
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "onException: ", e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                }).into(imageView);

        return rootView;
    }

    @Override
    public String toString() {
        return "ImageActivityPlaceHolderFragment{" +
                "name='" + name + '\'' +
                ", imageURL='" + imageURL + '\'' +
                ", position=" + position +
                '}';
    }
}
