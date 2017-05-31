package org.phpnet.openDrivinCloudAndroid.Protocols;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import net.gotev.uploadservice.BinaryUploadRequest;

import org.apache.commons.io.FileUtils;
import org.phpnet.openDrivinCloudAndroid.Model.Account;
import org.phpnet.openDrivinCloudAndroid.Model.FileUpload;
import org.phpnet.openDrivinCloudAndroid.Model.Sync;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.UUID;

import io.realm.Realm;


/**
 * Created by clement on 29/09/16.
 *
 * This class is used by the sync operation to perform uploads for a specific sync
 */
public class DrivinCloudSyncDavUploadRequest extends BinaryUploadRequest {
    private final Realm realm;
    private final Uri serverUrl;
    private final String path;
    private Upload upload;
    private Sync sync;
    private Account syncAccount;
    private final String uId;
    /**
     * True if the upload has been persisted in DB
     */
    private boolean persisted;
    private static final String TAG = DrivinCloudSyncDavUploadRequest.class.getSimpleName();


    /**
     * Create a new upload request
     * @param context
     * @param serverUrl
     * @param syncAccount The account object corresponding to the user asking for upload
     *                Set to null to use CurrentUser
     * @param sync The sync related to the upload request
     */
    public DrivinCloudSyncDavUploadRequest(final Context context, final UUID uId, Uri serverUrl, String path, final Account syncAccount, final Sync sync) throws MalformedURLException {
        super(context, uId.toString(), serverUrl.toString());
        Log.d(TAG, "DrivinCloudSyncDavUploadRequest: super(context, uId.toString():" + uId + ", serverUrl.toString():" + serverUrl + ");");
        this.uId = uId.toString();
        this.serverUrl = serverUrl;
        this.path = path;
        this.persisted = false;
        this.sync = sync;
        this.syncAccount = syncAccount;
        this.realm = Realm.getDefaultInstance();
        this.setMethod("PUT");
    }

    public BinaryUploadRequest setFileToUpload(final String file, final String fileName) throws FileNotFoundException {
        String fullUrl = serverUrl + path;
        Log.d(TAG, "setFileToUpload: Updating URL to "+fullUrl);
        params.setServerUrl(fullUrl);
        if(!persisted){ // We persist on the first file added to prevent registering empty uploads
            Log.d(TAG, "DrivinCloudSyncDavUploadRequest: Registering upload "+uId+" in DB for sync "+sync.getName()+" (User: "+syncAccount.getUsername()+")");

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    upload = realm.createObject(Upload.class, uId.toString());
                    upload.account = syncAccount;
                    upload.sync = sync;
                    syncAccount.uploads.add(upload);
                    sync.getUploads().add(upload);
                    Log.d(TAG, "Upload "+uId.toString()+" registered in DB for user "+syncAccount.getUsername()+" and for sync "+sync.getName());
                }
            });
            persisted = true;
        }

        final String distPath = serverUrl.getPath();
        Log.d(TAG, "Registering file "+ file +" for upload request "+uId+" in DB for user "+syncAccount.getUsername()+" (Dist path: "+distPath+")");
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                upload.files.add(new FileUpload(upload, file, distPath));
                upload.size += new File(file).length();
                Log.d(TAG, "Registered file to upload for request "+uId+" (Total: "+ FileUtils.byteCountToDisplaySize(upload.size)+") in DB for user "+syncAccount.getUsername());
            }
        });
        return super.setFileToUpload(file);
    }
}
