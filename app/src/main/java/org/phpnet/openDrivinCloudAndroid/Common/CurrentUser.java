package org.phpnet.openDrivinCloudAndroid.Common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.apache.webdav.lib.WebdavResource;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Model.Account;
import org.phpnet.openDrivinCloudAndroid.Model.Sync;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;

/**
 * Permet le transfert d'informations entre les activités.
 */
public class CurrentUser {

    public WebdavResource wdr = null;
    public OkHttpClient okHttpClient;
    public String username = "";
    public String password = "";
    public Uri serverURL;
    private String currentDir = "";
    public LinkedList<String> listPrecDir = new LinkedList<>();
    private static final String TAG = CurrentUser.class.getSimpleName();

    //Répertoire de l'application
    public String appDir;

    //Répertoire de déplacement
    private String currentDirMoveURL = "";

    //Liste des fichiers selectionnées
    public List<MyFile> listSelectedFiles = new LinkedList<>();

    //Liste des images dans un dossier donnée
    public List<MyFile> listImage = new LinkedList<>();

    public int indexCurrentImage;

    //Déplacement en cours ou pas
    public boolean move = false;

    private Toast toast = null;

    public String getCurrentDirMoveURL() {
        return currentDirMoveURL;
    }

    public void setCurrentDirMoveURL(String currentDirMoveURL) {
        this.currentDirMoveURL = currentDirMoveURL;
    }

    public int getMaxLetter() {
        int maxLetter = 16;
        return maxLetter;
    }

    private static CurrentUser instance = new CurrentUser();

    public static CurrentUser getInstance() {
        Log.d(TAG, "getInstance: Returning currently connected user "+instance.username+" on "+instance.serverURL);
        return instance;
    }

    private CurrentUser() {}

    /**
     * Get all the uploads for a user
     * This methods returns the whole upload list
     * @return All the uploads for a user
     */
    public RealmList<Upload> getUploadList(){
        RealmList<Upload> uploads = getDBEntry().getUploads();
        Log.d(TAG, "retrived uploads for user " + this.username +", count : " + uploads.size());
        return uploads;
    }

    /**
     * Get all the uploads for a user
     * This methods returns the whole upload list
     * @return All the uploads for a user
     */
    public RealmResults<Upload> getManualUploadList(){
        try {
            RealmResults<Upload> uploads = getDBEntry().getManualUploads();
            Log.d(TAG, "retrived manual uploads for user " + this.username +", count : " + uploads.size());
            return uploads;
        }catch(NullPointerException e){
            Log.d(TAG, "retrived manual uploads for user " + this.username +", count : 0");
            return null;
        }
    }

    /**
     * Get all the uploads for a user
     * This methods returns the whole upload list
     * @return All the uploads for a user
     */
    public RealmResults<Upload> getSyncUploadList(){
        try {
            RealmResults<Upload> uploads = getDBEntry().getSyncUploads();
            Log.d(TAG, "retrived sync uploads for user " + this.username +", count : " + uploads.size());
            return uploads;
        }catch(NullPointerException e){
            Log.d(TAG, "retrived manual uploads for user " + this.username +", count : 0");
            return null;
        }
    }


    /**
     * Get all the syncs for a user
     * This methods returns the whole sync list
     * @return All the syncs for a user
     */
    public RealmList<Sync> getSyncList() {
        RealmList<Sync> syncs = getDBEntry().getSyncs();
        if(syncs != null) {
            Log.d(TAG, "Retrieved syncs for user " + this.username + ", count : " + syncs.size());
        }else{
            Log.d(TAG, "Retrieved syncs for user " + this.username + ", count : null");
        }
        return syncs;
    }

    /**
     * Get the account model corresponding to the user
     * @return The user account entry in the database
     */
    public Account getDBEntry(){
        Log.d(TAG, "Retrieving Account entry in DB for username " + username + " on "+ serverURL);
        Realm DB = Realm.getDefaultInstance();
        RealmQuery<Account> queryAccount = DB.where(Account.class);
        queryAccount.equalTo(Account.FIELD_USERNAME, username).equalTo(Account.FIELD_HOSTNAME, serverURL.getHost()).equalTo(Account.FIELD_PATH, serverURL.getPath());

        // Lets create the entry if it doesn't exist
        if(queryAccount.count() < 1){
            Log.d(TAG, "No entry corresponding to user " + username + "\n" +
                    "Creating new entry");
            DB.beginTransaction();
            Account account = new Account(username, serverURL);
            DB.copyToRealm(account);
            DB.commitTransaction();

            Log.d(TAG, "Account "+account.getUsername()+" created");
            return account;
        }
        Account entry = queryAccount.findFirst();
        return entry;
    }

    /*
     * Renvoie le répertoire de l'application
     * */
    private String getAppDir(Context context) {
        PackageManager m = context.getPackageManager();
        PackageInfo p = null;
        try {
            p = m.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        appDir = p.applicationInfo.dataDir;
        return p.applicationInfo.dataDir;
    }

    /**
     * Used to get the full URL of the current directory (http(s)://hostname/basepath/currentdir)
     * @return
     */
    public Uri currentDirURL() {
        Uri.Builder uriBuilder = serverURL.buildUpon();
        for (String pathSegment :
                this.listPrecDir) {
            pathSegment.replace("/", "");
            if(!pathSegment.equals("")){
                uriBuilder.appendPath(pathSegment);
            }
        }
        for (String pathSegment :
                currentDir.split("/")) {
            pathSegment.replace("/", "");
            if(!pathSegment.equals("")){
                uriBuilder.appendPath(pathSegment);
            }
        }
        Log.d(TAG, "currentDirURL: "+uriBuilder.build());
        return uriBuilder.build();
    }

    public String currentDirPath(){
        if(this.currentDir.equals("")) return "/";
        return currentDir;
    }

    public String currentAbsoluteDirPath(){
        String path = TextUtils.join("/", this.listPrecDir);
        if(this.currentDir.equals("")) return "/";
        String tmpCurrDir = currentDir;
        if(!tmpCurrDir.endsWith("/")) tmpCurrDir+="/";
        if(!tmpCurrDir.startsWith("/")) tmpCurrDir = "/"+tmpCurrDir;
        return path+tmpCurrDir;
    }

    /**
     * Use this to get the title of the current page
     * @return current folder name or username if empty
     */
    public String getTitle() {
        /*Renvoi le titre du dossier courant*/
        if (this.currentDir.equals("")) {
            return CurrentUser.getInstance().username;
        }
        return this.currentDir;
    }

    /*Renvoi le précedent dossier*/
    public String getPrecDir() {
        String curr = this.currentDir;
        if (curr.equals(""))
            return "";
        curr = curr.substring(0, curr.length() - 1);
        int i = curr.lastIndexOf("/");
        if (i < 0) {
            return "";
        } else {
            return curr.substring(0, i + 1);
        }
    }

    //Affichage d'un message dans un context
    public void showToast(Context context, String msg) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public List<MyFile> getPreviewableImages(Context context) {
        LinkedList<MyFile> previewableImages = new LinkedList<>();
        for (MyFile img :
                listImage) {
            if (img.isPreviewable(context)) previewableImages.add(img);
        }
        return previewableImages;
    }

    public boolean isPasswordSaved(){
        return password != "";
    }

    public void logout() {
        Log.d(TAG, "logout: Disconnecting user "+username+" from "+this.serverURL);
        try {
            if (this.wdr != null) {
                //Fermeture de la session
                this.wdr.close();
            }
            this.wdr = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.currentDir = "";
        this.move = false;
    }

    /**
     * Recule d'un dossier dans l'arborescence
     */
    public void moveBack() {
        Log.d(TAG, "moveBack: Moving back from folder "+this.currentDir+" to "+this.listPrecDir.getLast());
        this.currentDir = this.listPrecDir.removeLast();
    }

    /**
     * Avance d'un dossier dans l'arborescence
     * @param folderName
     */
    public void moveForward(String folderName) {
        folderName = folderName.replace("/", "");
        Log.d(TAG, "moveForward: currentDir="+this.currentDir);
        Log.d(TAG, "moveForward: Moving from DIR "+this.currentDirURL()+" to DIR "+folderName);
        this.listPrecDir.addLast(this.currentDir);
        this.currentDir = folderName;
    }
}
