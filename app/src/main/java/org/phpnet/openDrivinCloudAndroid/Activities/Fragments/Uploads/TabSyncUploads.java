package org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Uploads;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.phpnet.openDrivinCloudAndroid.Adapter.SyncUploadsAdapter;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.R;

import io.realm.RealmResults;

/**
 * Created by clement on 21/07/16.
 */
public class TabSyncUploads extends Fragment {
    private static final String TAG = TabSyncUploads.class.getSimpleName();
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";

    protected RecyclerView mRecyclerView;
    protected SyncUploadsAdapter mSyncUploadsAdapter;
    protected RealmResults<Upload> dataSet;
    private FloatingActionButton mAddRandEntryView;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        initUploadsList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_uploads_tab_manual_uploads, container, false);
        rootView.setTag(TAG);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.listContainer);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(llm);


        mSyncUploadsAdapter = new SyncUploadsAdapter(getContext(), dataSet);
        mRecyclerView.setAdapter(mSyncUploadsAdapter);

        return rootView;
    }

    public void notifyDataSetCleared(){
        mSyncUploadsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Registering the uploadservice status broadcast listener.");
    }

    private void initUploadsList() {
        try {
            dataSet = CurrentUser.getInstance().getSyncUploadList().sort("startDate");
            Log.d(TAG, "Retrieving uploads list..." +
                    "\nCurrent user uploads count : " + dataSet.size());
        }catch (NullPointerException e){
            Log.d(TAG, "initUploadsList: No uploads to show");
        }
    }
}
