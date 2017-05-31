package org.phpnet.openDrivinCloudAndroid.Model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.Common.Settings.User;

import java.net.URISyntaxException;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

/**
 * Created by clement on 26/07/16.
 *
 * Account persistence
 * /!\ The database is not used to store the webdav connection info.
 * /!\ The webdav connection info is stored in SharedPreferences
 * /!\ We use username+hostname as the key to retrieve the connection
 * /!\ info from SharedPreferences
 *
 */
public class Account extends RealmObject implements Parcelable{
    private static final String TAG = "Account";
    public static final String FIELD_PK = "key";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_USE_SSL = "useSSL";

    @PrimaryKey int key; // Compound key, composed from hostname+username hashcode
    String username; //Used to retrieved the corresponding connection info from SharedPreferences
    String hostname; //Idem
    String path;
    boolean useSSL;

    RealmList<Sync> syncs; //Store all the syncs for the account
    public RealmList<Upload> uploads; //Store all the uploads

    public Account(String username, Uri url) {
        this.username = username;
        this.hostname = url.getHost();
        this.path = url.getPath();
        this.useSSL = url.getScheme().equals("https");
        this.key = (url.toString()+username).toLowerCase().hashCode();
    }

    public Account(){}

    protected Account(Parcel in) {
        int tempKey = in.readInt(); //Retrieve the primary key of our entry
        Realm DB = Realm.getDefaultInstance();
        Account tempAccount = DB.where(Account.class).equalTo("key", tempKey).findFirst(); //retrieve the entry from the unique key
        //Copy the field values
        key = tempAccount.key;
        hostname = tempAccount.getHostname();
        username = tempAccount.getUsername();
        uploads = tempAccount.getUploads();
        syncs = tempAccount.getSyncs();
        useSSL = tempAccount.getUseSSL();
        path = tempAccount.getPath();
    }

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.key = (hostname+username).toLowerCase().hashCode();
        this.username = username;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.key = (hostname+username).toLowerCase().hashCode();
        this.hostname = hostname;
    }

    public RealmList<Sync> getSyncs() {
        return syncs;
    }

    public void setSyncs(RealmList<Sync> syncs) {
        this.syncs = syncs;
    }

    public RealmList<Upload> getUploads() {
        return uploads;
    }

    public RealmResults<Upload> getManualUploads(){
        try {
            return uploads.where().isNull("sync").findAll();
        }catch(NullPointerException e){
            Log.e(TAG, "getManualUploads: uploads is null", e);
            return null;
        }
    }

    public RealmResults<Upload> getSyncUploads(){
        try {
            return uploads.where().isNotNull("sync").findAll();
        }catch(NullPointerException e){
            Log.e(TAG, "getManualUploads: uploads is null", e);
            return null;
        }
    }

    public void setUploads(RealmList<Upload> uploads) {
        this.uploads = uploads;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(key);
    }

    public User getUser(Context context){
        Log.d(TAG, "getUser: hostname "+hostname+path+", Username: "+username);
        User usr = null;
        try {
            usr = Settings.getUser(context, this.getUrl(), username);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if(usr == null) Log.d(TAG, "getUser: Can't retrieve user "+username+", " +
                "the user is probably not registered");
        return usr;
    }

    public org.phpnet.openDrivinCloudAndroid.Common.User getCommonUser(Context context){
        CurrentUser cu = CurrentUser.getInstance();
        return  new org.phpnet.openDrivinCloudAndroid.Common.User(cu.serverURL, cu.username, cu.password);

    }

    public String getPassword(Context context){
        return getUser(context).getPassword();
    }

    public void setUseSSL(boolean useSSL){
        this.useSSL = useSSL;
    }

    public boolean getUseSSL() {
        return useSSL;
    }

    public String getPath() {
        return path;
    }

    public Uri getUrl() throws URISyntaxException {
        return Uri.parse((getUseSSL()?"https://":"http://")+getHostname()+getPath());
    }
}
