package org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Uploads;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.phpnet.openDrivinCloudAndroid.Adapter.UploadsPagerAdapter;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.R;

import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnUploadsFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Uploads#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Uploads extends Fragment {
    private static final String TAG = Uploads.class.getSimpleName();
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_TAB_ID = "tabId";

    // Set the default tab
    private String tabId;

    private OnUploadsFragmentInteractionListener mListener;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private UploadsPagerAdapter adapter;

    public Uploads() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tabId The tab to show.
     * @return A new instance of fragment Uploads.
     */
    public static Uploads newInstance(@Nullable String tabId) {
        Uploads fragment = new Uploads();
        Bundle args = new Bundle();
        args.putString(ARG_TAB_ID, tabId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            if(getArguments().getString(ARG_TAB_ID) != null) {
                tabId = getArguments().getString(ARG_TAB_ID);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // Clear default menu entries as we don't need them
        inflater.inflate(R.menu.menu_uploads_fragment, menu); //Add an entry / icon to check sync
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.clear) {
            Log.d(TAG, "onOptionsItemSelected: Clear inactive uploads");
            new MaterialDialog.Builder(getContext())
                    .title(R.string.dialog_confirm_clear_uploads)
                    .content(R.string.dialog_confirm_clear_uploads_content)
                    .positiveText(R.string.dialog_confirm_clear_uploads_positive)
                    .negativeText(R.string.dialog_confirm_clear_uploads_negative)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Realm realm = Realm.getDefaultInstance();
                            if(viewPager.getCurrentItem() == UploadsPagerAdapter.ID_TAB_MANUALUPLOADS){
                                //Clear manual uploads
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.where(Upload.class)
                                                .isNull("sync")
                                                .equalTo("canceled", true)
                                                .or()
                                                .isNull("sync")
                                                .equalTo("progress", 100)
                                                .or()
                                                .isNull("sync")
                                                .isNotNull("error")
                                                .or()
                                                .isNull("sync")
                                                .isNull("startDate")
                                                .findAll()
                                                .deleteAllFromRealm();
                                        TabManualUploads fragment = (TabManualUploads) adapter.getItem(UploadsPagerAdapter.ID_TAB_MANUALUPLOADS);
                                        fragment.notifyDataSetCleared();
                                    }
                                });
                            }else{
                                //Clear sync uploads
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.where(Upload.class)
                                                .isNotNull("sync")
                                                .equalTo("canceled", true)
                                                .or()
                                                .isNotNull("sync")
                                                .equalTo("progress", 100)
                                                .or()
                                                .isNotNull("error")
                                                .findAll()
                                                .deleteAllFromRealm();
                                        TabSyncUploads fragment = (TabSyncUploads) adapter.getItem(UploadsPagerAdapter.ID_TAB_SYNCUPLOADS);
                                        fragment.notifyDataSetCleared();
                                    }
                                });
                            }
                        }
                    }).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View uploadsView = inflater.inflate(R.layout.fragment_uploads, container, false);

        tabLayout = (TabLayout) uploadsView.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.manual_uploads_tab));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.sync_uploads_tab));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        viewPager = (ViewPager) uploadsView.findViewById(R.id.pager);
        adapter = new UploadsPagerAdapter(getActivity().getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        return uploadsView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnUploadsFragmentInteractionListener) {
            mListener = (OnUploadsFragmentInteractionListener) context;
        } else {
/*            throw new RuntimeException(context.toString()
                    + " must implement OnUploadsFragmentInteractionListener");*/
            Log.e(context.toString(), " must implement interface of "+this.getClass().getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnUploadsFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

}