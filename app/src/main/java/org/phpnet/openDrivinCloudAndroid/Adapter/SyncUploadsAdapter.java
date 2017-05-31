package org.phpnet.openDrivinCloudAndroid.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadServiceBroadcastReceiver;

import org.phpnet.openDrivinCloudAndroid.Model.Sync;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.R;
import org.phpnet.openDrivinCloudAndroid.Util.FileUtils;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by clement on 01/08/16.
 */
public class SyncUploadsAdapter extends RecyclerView.Adapter<SyncUploadsAdapter.ViewHolder> {
    private static final String TAG = SyncUploadsAdapter.class.getSimpleName();
    private RealmResults<Upload> mDataSet;
    private Context mContext;


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileNameTextView;
        UploadServiceBroadcastReceiver uploadServiceBroadcastReceiver;
        public String uploadId;
        private ProgressBar progressView;
        private TextView progressText;
        private TextView nbFilesText;

        public ViewHolder(View v){
            super(v);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Realm realm = Realm.getDefaultInstance();
                    final Upload upload = realm.where(Upload.class)
                            .equalTo("uId",uploadId)
                            .findFirst();

                    if(upload.progress < 100 && upload.canceled == false){
                        new MaterialDialog.Builder(v.getContext())
                                .title(R.string.dialog_confirm_cancel_manual_upload)
                                .content(R.string.dialog_confirm_cancel_manual_upload_content)
                                .positiveText(R.string.dialog_confirm_cancel_manual_upload_positive)
                                .negativeText(R.string.continue_transfer)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        Log.d(TAG, "onClick: Stopping upload n°"+uploadId);
                                        UploadService.stopUpload(uploadId);
                                        realm.executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                upload.sync.setLastSuccess(null);
                                                upload.canceled = true;
                                            }
                                        });
                                    }
                                }).show();
                    }



                }
            });

            fileNameTextView = (TextView) v.findViewById(R.id.fileName);
            progressView = (ProgressBar) v.findViewById(R.id.progressBar);
            nbFilesText = (TextView) v.findViewById(R.id.nbFiles);
            progressText = (TextView) v.findViewById(R.id.progressText);

        }



        public TextView getFileNameTextView(){
            return fileNameTextView;
        }

        public ProgressBar getProgressView() {
            return progressView;
        }

        public TextView getProgressTextView() {
            return progressText;
        }
    }

    public SyncUploadsAdapter(Context context, RealmResults<Upload> dataSet){
        if(dataSet == null){
            Log.d(TAG, "SyncUploadsAdapter: Nb elements 0");
        }else{
            Log.d(TAG, "SyncUploadsAdapter: Nb elements "+dataSet.size());
        }
        mDataSet = dataSet;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
        // Creating a new view
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_uploads_tab_sync_uploads_item, viewGroup, false);


        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        Log.d(TAG, "Element " + position + " set.");
        //Set the upload ID
        viewHolder.uploadId = mDataSet.get(position).uId;

        try {
            Upload upload = mDataSet.get(position);
            Sync sync = mDataSet.get(position).sync;
            viewHolder.fileNameTextView.setText("Synchronisation de "+sync.getName()+" le "+
                    DateUtils.formatDateTime(mContext,
                            upload.startDate.getTime(),
                            DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE)
            );
            viewHolder.nbFilesText.setText(mContext.getString(R.string.sync_nb_files, String.valueOf(upload.files.size())));
            viewHolder.progressView.setProgress(upload.progress);
            String state = mContext.getString(R.string.sync_running);
            if(upload.progress == 100){
                state = mContext.getString(R.string.sync_finished)+" ("+
                        DateUtils.formatDateTime(mContext,
                                upload.endTime.getTime(),
                                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE)
                +")";
            }
            if(upload.error != null){
                state = upload.error;
            }
            if(upload.canceled == true){
                state = mContext.getString(R.string.sync_canceled);
            }
            viewHolder.progressText.setText(state);
        }catch(IndexOutOfBoundsException | NullPointerException e){
            Log.e(TAG, "onBindViewHolder: Error while retrieving upload "+viewHolder.uploadId+" files, the list is empty", e);
        }

        //Add listener for events concerning this particular upload
        viewHolder.uploadServiceBroadcastReceiver = new UploadServiceBroadcastReceiver(){
            @Override
            public void onCancelled(Context context, UploadInfo uploadInfo) {
                if(uploadInfo.getUploadId().equals(viewHolder.uploadId))
                    Log.d(TAG, "Upload "+uploadInfo.getUploadId()+" cancel event received by corresponding view in manual uploads list");
                viewHolder.getProgressTextView().setText(mContext.getString(
                        R.string.progress_sync_details,
                        FileUtils.byteCountToDisplaySize(uploadInfo.getUploadedBytes()),
                        uploadInfo.getProgressPercent()+"%", mContext.getString(R.string.sync_canceled))
                );
                super.onCancelled(context, uploadInfo);
            }

            @Override
            public void onProgress(Context context, UploadInfo uploadInfo) {
                if (uploadInfo.getUploadId().equals(viewHolder.uploadId)) {
                    Log.d(TAG, "Upload " + uploadInfo.getUploadId() + " progress event received by corresponding view in manual uploads list. Progress : "+uploadInfo.getProgressPercent());
                    viewHolder.getProgressView().setProgress(uploadInfo.getProgressPercent());
                    viewHolder.getProgressTextView().setText(mContext.getString(R.string.progress_sync_details, FileUtils.byteCountToDisplaySize(uploadInfo.getUploadedBytes()), uploadInfo.getProgressPercent()+"%", mContext.getString(R.string.sync_running)));
                }
                super.onProgress(context, uploadInfo);
            }

            @Override
            public void onError(Context context, UploadInfo uploadInfo, Exception exception) {
                if(uploadInfo.getUploadId().equals(viewHolder.uploadId)) {
                    Log.d(TAG, "Upload " + uploadInfo.getUploadId() + " error event received by corresponding view in manual uploads list");
                    viewHolder.getProgressTextView().setText(mContext.getString(R.string.progress_sync_details, FileUtils.byteCountToDisplaySize(uploadInfo.getUploadedBytes()), uploadInfo.getProgressPercent()+"%", mContext.getString(R.string.sync_error)));
                }
                super.onError(context, uploadInfo, exception);
            }

            @Override
            public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {

                if(uploadInfo.getUploadId().equals(viewHolder.uploadId)) {
                    Log.d(TAG, "Upload " + uploadInfo.getUploadId() + " completed event received by corresponding view in manual uploads list, setting progress to 100%");
                    viewHolder.getProgressView().setProgress(100);
                    viewHolder.getProgressTextView().setText(mContext.getString(R.string.progress_sync_details, FileUtils.byteCountToDisplaySize(uploadInfo.getUploadedBytes()), uploadInfo.getProgressPercent()+"%", mContext.getString(R.string.sync_finished)));
                }

                super.onCompleted(context, uploadInfo, serverResponse);
            }
        };

        viewHolder.uploadServiceBroadcastReceiver.register(mContext);

    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.uploadServiceBroadcastReceiver.unregister(mContext);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount(){
        try {
            return mDataSet.size();
        }catch (NullPointerException e){
            Log.d(TAG, "getItemCount: Provided dataset is null");
            return 0;
        }
    }

}
