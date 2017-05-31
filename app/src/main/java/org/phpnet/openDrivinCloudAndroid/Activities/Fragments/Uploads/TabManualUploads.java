package org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Uploads;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.phpnet.openDrivinCloudAndroid.Adapter.ManualUploadsAdapter;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.R;

import io.realm.RealmResults;

/**
 * Created by clement on 21/07/16.
 */
public class TabManualUploads extends Fragment {

    private static final String TAG = TabManualUploads.class.getSimpleName();
    protected RecyclerView mRecyclerView;
    protected ManualUploadsAdapter mManualUploadsAdapter;
    protected RealmResults<Upload> dataSet;

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

        mManualUploadsAdapter = new ManualUploadsAdapter(getContext(), dataSet);
        mRecyclerView.setAdapter(mManualUploadsAdapter);

        return rootView;
    }

    public void notifyDataSetCleared(){
        mManualUploadsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Registering the uploadservice status broadcast listener.");
    }

    private void initUploadsList() {
        try {
            dataSet = CurrentUser.getInstance().getManualUploadList().sort("startDate");
            Log.d(TAG, "Retrieving uploads list..." +
                    "\nCurrent user uploads count : " + dataSet.size());
        }catch (NullPointerException e){
            Log.d(TAG, "initUploadsList: No uploads to show");
        }
    }
}
