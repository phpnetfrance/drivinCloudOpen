package org.phpnet.openDrivinCloudAndroid.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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

import org.phpnet.openDrivinCloudAndroid.Model.FileUpload;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.R;
import org.phpnet.openDrivinCloudAndroid.Util.FileUtils;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by clement on 01/08/16.
 */
public class ManualUploadsAdapter extends RecyclerView.Adapter<ManualUploadsAdapter.ViewHolder> {
    private static final String TAG = ManualUploadsAdapter.class.getSimpleName();
    private RealmResults<Upload> mDataSet;
    private Context mContext;


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileNameTextView;
        UploadServiceBroadcastReceiver uploadServiceBroadcastReceiver;
        public String uploadId;
        private ProgressBar progressView;
        private TextView progressText;

        public ViewHolder(View v){
            super(v);

            //Add click listener on the uploadlist item
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Element " + getAdapterPosition() + " clicked, canceling. (UID="+uploadId+")");
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
                                        UploadService.stopUpload(uploadId);
                                        realm.executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
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

    public ManualUploadsAdapter(Context context, RealmResults<Upload> dataSet){
        mDataSet = dataSet;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
        // Creating a new view
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_uploads_tab_manual_uploads_item, viewGroup, false);


        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        Log.d(TAG, "Element " + position + " set.");
        //Set the upload ID
        viewHolder.uploadId = mDataSet.get(position).uId;

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        RealmList<FileUpload> files = mDataSet.get(position).files;
        try {
            FileUpload file = files.first();
            Upload upload = mDataSet.get(position);

            viewHolder.fileNameTextView.setText(file.getLocalName());
            viewHolder.progressView.setProgress(upload.progress);

            if(file.upload.error == null) {

                if(upload.elapsedTime != null
                        && upload.sent != 0) {

                    viewHolder.progressText.setText(
                            mContext.getString(R.string.progress_details, upload.elapsedTime, FileUtils.byteCountToDisplaySize(upload.sent),
                                    upload.progress + "%")
                    );
                }else{
                    viewHolder.progressText.setText(
                            mContext.getString(R.string.progress_details_error, "Erreur réseau")
                    );
                }
            }else{
                viewHolder.progressText.setText(mContext.getString(R.string.progress_details_error, file.upload.error));
            }

            Log.d(TAG, "Setting name to " + file.getLocalName() + " for item "+position);
            viewHolder.getFileNameTextView().setText(file.getLocalName());
        }catch(IndexOutOfBoundsException e){
            Log.e(TAG, "onBindViewHolder: Error while retrieveing upload "+viewHolder.uploadId+" files, the list is empty", e);
        }



        //Add listener for events concerning this particular upload
        viewHolder.uploadServiceBroadcastReceiver = new UploadServiceBroadcastReceiver(){
            @Override
            public void onCancelled(Context context, UploadInfo uploadInfo) {
                if(uploadInfo.getUploadId().equals(viewHolder.uploadId))
                    Log.d(TAG, "Upload "+uploadInfo.getUploadId()+" cancel event received by corresponding view in manual uploads list");

                super.onCancelled(context, uploadInfo);
            }

            @Override
            public void onProgress(Context context, UploadInfo uploadInfo) {
                if (uploadInfo.getUploadId().equals(viewHolder.uploadId)) {
                    Log.d(TAG, "Upload " + uploadInfo.getUploadId() + " progress event received by corresponding view in manual uploads list. Progress : "+uploadInfo.getProgressPercent());
                    viewHolder.getProgressView().setProgress(uploadInfo.getProgressPercent());
                    viewHolder.getProgressTextView().setText(mContext.getString(R.string.progress_details, uploadInfo.getElapsedTimeString(), FileUtils.byteCountToDisplaySize(uploadInfo.getUploadedBytes())+"/"+FileUtils.byteCountToDisplaySize(uploadInfo.getTotalBytes()), uploadInfo.getProgressPercent()+"%"));
                }
                super.onProgress(context, uploadInfo);
            }

            @Override
            public void onError(Context context, UploadInfo uploadInfo, Exception exception) {
                if(uploadInfo.getUploadId().equals(viewHolder.uploadId))
                    Log.d(TAG, "Upload "+uploadInfo.getUploadId()+" error event received by corresponding view in manual uploads list");
                super.onError(context, uploadInfo, exception);
            }

            @Override
            public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {

                if(uploadInfo.getUploadId().equals(viewHolder.uploadId)) {
                    Log.d(TAG, "Upload " + uploadInfo.getUploadId() + " completed event received by corresponding view in manual uploads list, setting progress to 100%");
                    viewHolder.getProgressView().setProgress(100);
                    viewHolder.getProgressTextView().setText(mContext.getString(R.string.progress_details, uploadInfo.getElapsedTimeString(), FileUtils.byteCountToDisplaySize(uploadInfo.getUploadedBytes())+"/"+FileUtils.byteCountToDisplaySize(uploadInfo.getTotalBytes()), uploadInfo.getProgressPercent()+"%"));
                }

                super.onCompleted(context, uploadInfo, serverResponse);
            }
        };

        viewHolder.uploadServiceBroadcastReceiver.register(mContext);

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
