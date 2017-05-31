package org.phpnet.openDrivinCloudAndroid.Model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;

/**
 * Created by clement on 26/07/16.
 */
public class Sync extends RealmObject implements Parcelable {
    /* Relations */
    Account account; //The account linked to this Syncs
    RealmList<Upload> uploads; //All the uploads corresponding to a Syncs
    RealmList<SyncLog> log; //Log of the sync operations to provide more transparency to the user
    String name; //The name of the sync
    boolean active; //If active, the phone will retry on fail
    //int retryInterval; //The time in seconds between each retry for failed uploads TODO
    String folder; //The folder we have to sync
    //int maxNbSyncs; //How many syncs to keep on the server, use this to auto delete old sync folders TODO
    Date dateCreated;
    Date lastSuccess;
    int interval; //The duration of the interval between syncs in minutes
    @SerializedName("networkType")
    String rawNetworkType;
    @Ignore
    private transient NET_TYPE networkType;

    private boolean chargingOnly;
    private boolean force; //When set to true, will not verify battery state, connection type and last sync date
    private boolean reset; //When set to true, will re-upload the entire directory (This doesn't delete files, only overwrite)



    public Sync(){}

    public Sync(String name, Account account){
        this.name = name;
        this.account = account;
    }

    protected Sync(Parcel in) {
        account = in.readParcelable(Account.class.getClassLoader());
        name = in.readString();
        active = in.readByte() != 0;
        folder = in.readString();
        interval = in.readInt();
    }

    public static final Creator<Sync> CREATOR = new Creator<Sync>() {
        @Override
        public Sync createFromParcel(Parcel in) {
            return new Sync(in);
        }

        @Override
        public Sync[] newArray(int size) {
            return new Sync[size];
        }
    };

    public void addLog(String type, String message){
        this.log.add(new SyncLog(this, new Date(), type, message));
    }

    public Date getNextSyncDate(){
        Date previous = dateCreated;
        Calendar cal = Calendar.getInstance();
        if(lastSuccess != null){
            previous = lastSuccess;
        }
        cal.setTime(previous);
        cal.add(Calendar.MINUTE, interval);
        return cal.getTime();
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public RealmList<Upload> getUploads() {
        return uploads;
    }

    public void setUploads(RealmList<Upload> uploads) {
        this.uploads = uploads;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastSuccess() {
        return lastSuccess;
    }

    public String getLastSuccessPrintable(){
        if(lastSuccess == null) return null;
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("fr","FR"));
        return dateFormat.format(lastSuccess).toString();
    }

    public void setLastSuccess(Date lastSuccess) {
        this.lastSuccess = lastSuccess;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setNetworkType(NET_TYPE networkType) {
        this.networkType = networkType;
        setRawNetworkType(networkType.name());
    }

    public NET_TYPE getNetworkType(){
        return NET_TYPE.valueOf(rawNetworkType);
    }

    private void setRawNetworkType(String rawNetworkType) {
        this.rawNetworkType = rawNetworkType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(account, flags);
        dest.writeString(name);
        dest.writeByte((byte) (active ? 1 : 0));
        dest.writeString(folder);
        dest.writeInt(interval);
    }

    public void setChargingOnly(boolean chargingOnly) {
        this.chargingOnly = chargingOnly;
    }

    public boolean canSyncOnBattery() {
        return !chargingOnly;
    }

    public enum NET_TYPE{
        WIFI, WIFIDATA
    }

    @Override
    public String toString() {
        return "Sync{" +
                "account=" + account +
                ", uploads=" + uploads +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", folder='" + folder + '\'' +
                ", dateCreated=" + dateCreated +
                ", lastSuccess=" + lastSuccess +
                ", interval=" + interval +
                ", networkType=" + networkType +
                '}';
    }

    /**
     * Checks if this sync must be forced
     * A forced sync will be executed without connectivity, time since last and battery state checks
     * Calling this will reset the force flag to false.
     * @return true if the sync must be forced
     */
    public boolean isForce(){
        boolean tmpForce = force;
        Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                force = false;
            }
        });
        return tmpForce;
    }

    /**
     * When this is called, the next sync will be forced
     * (A forced sync will be executed without connectivity, time since last and battery state checks)
     */
    public void forceOnNextSync(){
        force = true;
    }

    /**
     * Checks if this sync must be reset
     * A reset sync will bypass files modif date checks and re-upload everything (Thus not deleting files, only overwriting existing ones)
     * Calling this will reset the reset flag to false.
     * @return true if reset asked
     */
    public boolean isReset(){
        boolean tmpReset = reset;
        Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                reset = false;
            }
        });
        return tmpReset;
    }


    /**
     * When this is calles, the next sync will be reset
     * A reset sync will bypass files modif date checks and re-upload everything (Thus not deleting files, only overwriting existing ones)
     */
    public void resetOnNextSync(){
        reset = true;
    }
}
