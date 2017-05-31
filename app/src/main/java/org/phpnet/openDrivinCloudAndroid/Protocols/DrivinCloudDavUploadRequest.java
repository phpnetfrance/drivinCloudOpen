package org.phpnet.openDrivinCloudAndroid.Protocols;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import net.gotev.uploadservice.BinaryUploadRequest;
import net.gotev.uploadservice.Placeholders;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.okhttp.OkHttpStack;

import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.User;
import org.phpnet.openDrivinCloudAndroid.Model.Account;
import org.phpnet.openDrivinCloudAndroid.Model.FileUpload;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.UUID;

import io.realm.Realm;


/**
 * Created by clement on 11/08/16.
 *
 * This class adds persitence to the webdav upload requests
 *
 * Caution: OkHttp interceptors are not working, you need to specify credentials in order to do authenticated requests ( this.setBasicAuth() )
 */
public class DrivinCloudDavUploadRequest extends BinaryUploadRequest {
    private User user;
    private Upload upload;
    private final String uId;
    Realm realm;
    private Uri serverUrl;
    private String path;
    private String fileName;
    private static final String TAG = DrivinCloudDavUploadRequest.class.getSimpleName();

    /**
     * Create a new upload request
     * @param context
     * @param serverUrl
     * @param path
     * @param account The account object corresponding to the user asking for upload
     */
    public DrivinCloudDavUploadRequest(final Context context, final UUID uId, Uri serverUrl, String path, @Nullable final Account account) throws MalformedURLException {
        super(context, uId.toString(), serverUrl+path);
        this.serverUrl = serverUrl;
        UploadNotificationConfig notifConfig = new UploadNotificationConfig();
        this.path = path;
        notifConfig
                //.setIcon(R.drawable.ic_drivincloud_notificon) //TODO API16 compat
                .setAutoClearOnSuccess(false)
                .setInProgressMessage(String.format(context.getString(R.string.uploadfile_notif_mesg), Placeholders.UPLOAD_RATE, Placeholders.PROGRESS))
                .setCompletedMessage(String.format(context.getString(R.string.fileupload_notif_completed), Placeholders.ELAPSED_TIME))
                .setErrorMessage(context.getString(R.string.fileupload_notif_error))
                .setTitle(context.getString(R.string.fileupload_notif_title));
        super.setNotificationConfig(notifConfig);
        realm = Realm.getDefaultInstance();
        user = getCommonUser(account, context);
        UploadService.HTTP_STACK = new OkHttpStack(CurrentUser.getInstance().getOkHttpClient());
        this.setMethod("PUT");
        this.uId = uId.toString();

        Log.d(TAG, "Registering upload "+uId.toString()+" in DB for user "+user.username);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                upload = realm.createObject(Upload.class, uId.toString());
                upload.account = account;
                if(account == null) upload.account = CurrentUser.getInstance().getDBEntry();
                user.getDbEntry().uploads.add(upload);
                Log.d(TAG, "Upload "+uId.toString()+" registered in DB for user "+user.username);
            }
        });

    }


    private User getCommonUser(Account account, Context context) {
        User user;
        if(account == null){
            CurrentUser currentUser = CurrentUser.getInstance();
            user = new User(currentUser.serverURL, currentUser.username, currentUser.password);
        }else{
            user = account.getCommonUser(context);
        }
        return user;
    }

    @Override
    public BinaryUploadRequest setFileToUpload(String file){
        throw new UnsupportedOperationException("Use setFileToUpload(final Uri file, final String fileName) instead");
    }

    /**
     * Used to upload a file using its Uri
     * @param file
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public BinaryUploadRequest setFileToUpload(final Uri file, final String fileName) throws FileNotFoundException {
        this.fileName = fileName;

        /* ------- DEBUG -------
        Cursor cursor = context.getContentResolver().query(file, null, null, null, null);
        if (cursor == null) {
            Logger.error(getClass().getSimpleName(), "null cursor for " + file + ", returning size 0");
        }
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        long size = cursor.getLong(sizeIndex);
        cursor.close();
        Log.d(TAG, "setFileToUpload: Successfully retrieved file size: "+size);
        /* ----- END DEBUG ----- */

        String fullUrl = serverUrl+path+fileName;
        Log.d(TAG, "Updating URL to "+fullUrl);
        this.params.setServerUrl(fullUrl);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Log.d(TAG, "Registering file to upload for request "+uId+" in DB for user "+user.username+" (File path on device : "+ file +" Remote path : "+path+")");
                upload.files.add(new FileUpload(upload, fileName, path));
                Log.d(TAG, "Registered file to upload for request "+uId+") in DB for user "+user.username);
            }
        });
        return super.setFileToUpload(file.toString());
    }

    /**
     * Used to upload a file using absolute path
     * @param file
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public BinaryUploadRequest setFileToUpload(final String file, final String fileName) throws FileNotFoundException {
        this.fileName = fileName;

        /* ------- DEBUG -------
        Cursor cursor = context.getContentResolver().query(file, null, null, null, null);
        if (cursor == null) {
            Logger.error(getClass().getSimpleName(), "null cursor for " + file + ", returning size 0");
        }
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        long size = cursor.getLong(sizeIndex);
        cursor.close();
        Log.d(TAG, "setFileToUpload: Successfully retrieved file size: "+size);
        /* ----- END DEBUG ----- */

        String fullUrl = serverUrl+path+fileName;
        Log.d(TAG, "Updating URL to "+fullUrl);
        this.params.setServerUrl(fullUrl);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Log.d(TAG, "Registering file to upload for request "+uId+" in DB for user "+user.username+" (File path on device : "+ file +" Remote path : "+path+")");
                upload.files.add(new FileUpload(upload, fileName, path));
                Log.d(TAG, "Registered file to upload for request "+uId+") in DB for user "+user.username);
            }
        });
        return super.setFileToUpload(file);
    }
}
