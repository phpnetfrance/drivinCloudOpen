package org.phpnet.openDrivinCloudAndroid.Sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.okhttp.OkHttpStack;

import org.phpnet.openDrivinCloudAndroid.Model.Sync;
import org.phpnet.openDrivinCloudAndroid.Model.SyncLog;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.Protocols.DrivinCloudDavUploadRequest;
import org.phpnet.openDrivinCloudAndroid.Protocols.DrivinCloudSyncDavUploadRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;


/**
 * Created by clement on 04/07/16.
 */
public class DrivincloudAbstractSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "DrivincloudAbstractSync";
    public static final String SYNC_FOLDER = "/syncs";


    ConnectivityManager connectivityManager;
    NetworkInfo netInfo;
    IntentFilter ifilter;
    Intent batteryStatus;
    ArrayList<DrivinCloudDavUploadRequest> uploadRequests;
    ArrayList<String> createdFolders = new ArrayList<>();
    private OkHttpClient okHttpClient;
    private org.phpnet.openDrivinCloudAndroid.Model.Account syncAccount;

    public DrivincloudAbstractSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        netInfo = connectivityManager.getActiveNetworkInfo();
        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = context.registerReceiver(null, ifilter);
        uploadRequests = new ArrayList<>();
    }

    @Override
    public void onPerformSync(Account account,
                              Bundle bundle,
                              String s,
                              ContentProviderClient contentProviderClient,
                              SyncResult syncResult) {

        /**
         * 1) Get syncs from DB
         * 2) Verify last sync
         * 3) If [last sync date] > [sync interval + last sync date] do Upload
         * 4) Back to 2)
         */
        Log.d(TAG, "onPerformSync: [SyncOp] operation begin");

        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Sync> activeSyncsQuery = realm.where(Sync.class);
        RealmResults<Sync> activeSyncs = activeSyncsQuery.equalTo("active", true).findAll();

        Log.d(TAG, "onPerformSync: [SyncOp] Found "+activeSyncs.size()+" active syncs in DB");

        //Loop through the different syncs configured by the user
        for (final Sync sync : activeSyncs) {
            Log.d(TAG, "onPerformSync: [SyncOp] Processing sync "+sync.toString());
            boolean isForce = sync.isForce();
            final boolean isReset = sync.isReset();
            if(isForce || (isConnectionTypeValid(sync) && isBatteryStateValid(sync)) ) {
                if (isForce || isAwaitingSync(sync)) {
                    if(isForce){
                        Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() + " has force flag.");
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                sync.addLog(SyncLog.LOG_TYPES.syncStateVerif.toString(), "L'utilisateur a choisi de forcer cette synchronisation");
                            }
                        });
                    }else{
                        DateFormat localeDateformat = android.text.format.DateFormat.getDateFormat(getContext());
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                sync.addLog(SyncLog.LOG_TYPES.syncStateVerif.toString(), "Dernière synchronisation :"+ sync.getLastSuccessPrintable()+". Cette synchronisation doit être effectuée.");
                            }
                        });
                    }
                    Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() + " is awaiting " +
                            "synchronisation (Last sync: " + sync.getLastSuccessPrintable() + ")");

                    String localFolder = sync.getFolder();

                    syncAccount = sync.getAccount();
                    if(syncAccount != null) {
                        final List<File> filesToSave = getListFiles(new File(localFolder));
                        Log.d(TAG, "onPerformSync: [SyncOp] Checking files for sync " + sync.getName() +
                                ". (Fol: " + sync.getFolder() + "). Found " + filesToSave.size() +
                                " files awaiting sync");
                        if(sync.getLastSuccess() == null){
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    sync.addLog("fileToSync", "Première synchronisation, " + filesToSave.size() + " à envoyer vers votre drivinCloud");
                                }
                            });
                        }

                        File baseSyncPath = new File(sync.getFolder());
                        int nbFilesToUpload = 0;
                        for (final File file : filesToSave) {
                            try {
                                Date lastModified = new Date(file.lastModified());
                                long localSize = file.getTotalSpace();

                                DrivinCloudSyncDavUploadRequest uReq = null;
                                String relativePath = baseSyncPath.toURI().relativize(new File(file.getAbsolutePath()).toURI()).getPath();
                                try {
                                    uReq = new DrivinCloudSyncDavUploadRequest(
                                            getContext(),
                                            UUID.randomUUID(),
                                            syncAccount.getUrl(),
                                            SYNC_FOLDER + "/" + sync.getName() + "/" + relativePath,
                                            syncAccount,
                                            sync
                                    );

                                    final String creds = Credentials.basic(syncAccount.getUsername(), syncAccount.getPassword(getContext()));

                                    okHttpClient = new OkHttpClient.Builder()
                                            .authenticator(new Authenticator() {
                                                @Override
                                                public Request authenticate(Route route, Response response) throws IOException {
                                                    Log.d(TAG, "okHttpAuthenticate: auth for response " + response);
                                                    return response.request().newBuilder()
                                                            .header("Authorization", creds)
                                                            .build();
                                                }
                                            })
                                            .addInterceptor(new HttpLoggingInterceptor()
                                                    .setLevel(HttpLoggingInterceptor.Level.BASIC))
                                            .build();
                                    UploadService.HTTP_STACK = new OkHttpStack(okHttpClient);
                                    uReq.setBasicAuth(syncAccount.getUsername(), syncAccount.getPassword(getContext()));

                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (sync.getLastSuccess() == null) { //In case of the first sync
                                    Log.d(TAG, "onPerformSync: [SyncOp] File " + file.getAbsolutePath() +
                                            " has never been synchronized, add it to uploads list (Sync: " + sync.getName() + ", uploadpath: " + relativePath + ")");
                                    nbFilesToUpload++;

                                    createFolders(SYNC_FOLDER + "/" + sync.getName() + "/" + relativePath.substring(0, relativePath.length()-file.getName().length()));
                                    uReq.setFileToUpload(file.getAbsolutePath(), file.getName());
                                    uReq.startUpload();
                                } else {
                                    if (!file.isDirectory()) { //If its not a folder
                                        if (isReset || lastModified.after(sync.getLastSuccess())) {
                                            Log.d(TAG, "onPerformSync: [SyncOp] (isReset: "+isReset+") File " + file.getAbsolutePath() +
                                                    " has been added/modified since last sync, add it to uploads list (Sync: " + sync.getName() + ", uploadpath: " + relativePath + ")");

                                            realm.executeTransaction(new Realm.Transaction() {
                                                                         @Override
                                                                         public void execute(Realm realm) {
                                                                             if (isReset) {
                                                                                 sync.addLog("fileToSync", "Le fichier " + file.getName() + " va être envoyé à nouveau sur le serveur (Remise à zero). Envoi de la dernière version du fichier vers votre drivinCloud.");
                                                                             } else {
                                                                                 sync.addLog("fileToSync", "Le fichier " + file.getName() + " a changé depuis la dernière syncronisation. Envoi de la dernière version du fichier vers votre drivinCloud.");
                                                                             }
                                                                         }
                                                                     });
                                            nbFilesToUpload++;
                                            //uReq.addFileToUpload(file.getAbsolutePath(), SYNC_FOLDER + "/" + sync.getName() + "/" + relativePath);

                                            createFolders(SYNC_FOLDER + "/" + sync.getName() + "/" + relativePath.substring(0, relativePath.length()-file.getName().length()));
                                            uReq.setFileToUpload(file.getAbsolutePath(), file.getName());
                                            uReq.startUpload();
                                        } else {
                                            Log.d(TAG, "onPerformSync: [SyncOp] File " + file.getAbsolutePath() +
                                                    " has NOT been modified since last sync, ignore it (Sync: " + sync.getName() + ")");
                                        }
                                    }
                                }
                            } catch (FileNotFoundException e) {
                                //TODO File has been deleted during sync prepare
                                e.printStackTrace();
                            }
                        }

                        try {
                            if(nbFilesToUpload > 0) {
                                Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() + " upload is now being processed");
                                //uReq.startUpload();
                            }else{
                                Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() + " has not modified files, skipping.");
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG,
                                    "onPerformSync: [SyncOp] Error while performing upload for sync " + sync.getName(),
                                    e);
                            e.printStackTrace();

                            //In case there is no file to sync, set success for better user feedback.
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    // Set sync launch success
                                    sync.setLastSuccess(new Date());
                                }
                            });

                            return;
                        } catch (IllegalArgumentException e) {
                            Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() + " has no file to sync, abort. (The folder is probably empty or nothing has changed since the last sync)", e);
                        }

                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                // Set sync launch success
                                sync.setLastSuccess(new Date());
                            }
                        });

                        Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() + " upload complete. Last sync date set to " + sync.getLastSuccess() + " in DB.");
                    }else{
                        //Clean broken sync
                        Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() + " has no account, deleting.");
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                sync.deleteFromRealm();
                            }
                        });
                    }



                } else {
                    Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() +
                            " is not awaiting synchronisation.");
                    final int hourDiff = (int) (sync.getNextSyncDate().getTime() - sync.getLastSuccess().getTime()) / 4600;

                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            sync.addLog(SyncLog.LOG_TYPES.syncStateVerif.toString(), "La synchronisation n'est pas en attente, la prochaine synchronisation est programmée dans "+hourDiff+" heure(s)");
                        }
                    });
                }
            }else{
                if(!isBatteryStateValid(sync)){
                    Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() +
                            " will not get synced because battery state restriction type is canSyncOnBattery:"
                            +sync.canSyncOnBattery()+" and current battery state is "
                            +batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
                    if(isAwaitingSync(sync)){
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                sync.addLog(SyncLog.LOG_TYPES.syncStateVerif.toString(), "La synchronisation est en attente mais elle ne peut pas se faire sur batterie, branchez votre appareil ou changez la configuration.");
                            }
                        });
                    }
                }
                if(!isConnectionTypeValid(sync)){
                    Log.d(TAG, "onPerformSync: [SyncOp] Sync " + sync.getName() +
                            " will not get synced because connection type restriction type is "
                            +sync.getNetworkType()+" and current connection is "
                            +netInfo.getTypeName());

                    if(isAwaitingSync(sync)){
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                sync.addLog(SyncLog.LOG_TYPES.syncStateVerif.toString(), "La synchronisation est en attente mais elle ne peut se faire qu'en wifi, connecteaz votre appareil ou changez la configuration.");
                            }
                        });
                    }
                }
            }
        }
    }

    private void createFolders(String path) {
        if(!createdFolders.contains(path)) {
            String lastFolderCreated = "";
            for (String segment:path.split("/")) {
                lastFolderCreated+=segment+"/";
                if(!createdFolders.contains(lastFolderCreated)){
                    Log.d(TAG, "createFolders: MKCOL "+lastFolderCreated);
                    try {
                        Request request = new Request.Builder().url(syncAccount.getUrl().toString()+lastFolderCreated).method("MKCOL", null).build();
                        Response response = okHttpClient.newCall(request).execute();
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        Log.d(TAG, "createFolders: [DONE] MKCOL "+lastFolderCreated);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    createdFolders.add(lastFolderCreated);
                }
            }
        }
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
        Log.d(TAG, "onSyncCanceled: [SyncOp] cancel sync ops");

        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Sync> activeSyncsQuery = realm.where(Sync.class);
        RealmResults<Sync> runningSyncs = activeSyncsQuery.equalTo("active", true).findAll();
        for (Sync sync:
             runningSyncs) {
            RealmResults<Upload> runningUploads = sync.getUploads().where().equalTo("serverResponseCode", 0).findAll();
            for (Upload upload: runningUploads) {
                UploadService.stopUpload(upload.uId);
            }
        }
    }

    /**
     * Compares sync connection type restriction with current device connection
     * @param sync
     * @return true if sync is allowed
     */
    private boolean isConnectionTypeValid(Sync sync) {
        switch (sync.getNetworkType()){
            case WIFI:
                return isDeviceWifi();
            case WIFIDATA:
                return true;
        }
        return false;
    }

    /**
     * Compares the battery status sync restriction with current device battery status
     * @param sync
     * @return true if the sync is allowed
     */
    private boolean isBatteryStateValid(Sync sync){
        if(sync.canSyncOnBattery()){
            return true;
        }else{
            return isDeviceCharging();
        }
    }

    private boolean isAwaitingSync(Sync sync) {

        Date current = new Date();
        Date nextSync = new Date();
        if(sync.getLastSuccess() != null){
            Calendar cal = Calendar.getInstance();
            cal.setTime(sync.getLastSuccess());
            cal.add(Calendar.MINUTE, sync.getInterval());
            nextSync = new Date(cal.getTimeInMillis());
            Log.d(TAG, "onPerformSync: [SyncOp] Sync "+sync.toString()+" has already been " +
                    "executed on "+sync.getLastSuccess().toString()+" next sync should " +
                    "append on "+nextSync.toString());
        }
        return sync.getLastSuccess() == null || nextSync.before(current);
    }

    private List<File> getListFiles(File parentDir) {
        Log.d(TAG, "getListFiles: Checking folder "+parentDir);
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }

    /**
     * Checks if the device is currently connected via wifi (or ethernet)
     * @return true id wifi/ethernet, false if 3G,4G,etc.
     */
    public boolean isDeviceWifi() {
        return  netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;
    }

    /**
     * Checks if the device is currently in charge
     * @return true if the device is currently charging
     */
    public boolean isDeviceCharging() {
        int batstat = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        Log.d(TAG, "isDeviceCharging: "+batstat);
        return batstat == BatteryManager.BATTERY_STATUS_CHARGING || batstat == BatteryManager.BATTERY_STATUS_FULL;
    }
}
