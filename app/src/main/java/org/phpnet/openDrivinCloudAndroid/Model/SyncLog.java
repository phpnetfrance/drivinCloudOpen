package org.phpnet.openDrivinCloudAndroid.Model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import io.realm.RealmObject;

/**
 * Created by clement on 02/02/17.
 * This class is used to log informations about the sync operations
 * This should bring more transparency to the user
 */

public class SyncLog extends RealmObject implements Parcelable {

    Sync sync;
    Date datetime;
    String type;
    String message;

    //A list of usable types, non exhaustive
    public enum LOG_TYPES{
        syncStateVerif,
        syncStart,
        syncEnd
    };


    public SyncLog(){
    }

    public SyncLog(Sync sync, Date datetime, String type, String message) {
        this.sync = sync;
        this.datetime = datetime;

        this.type = type;
        this.message = message;
    }

    protected SyncLog(Parcel in) {
        sync = in.readParcelable(Sync.class.getClassLoader());
        type = in.readString();
        message = in.readString();
    }

    /**
     * Returns the full log message
     * @param context
     * @return String log message
     */
    public String getMessage(Context context){
        return message;
    }

    /**
     * Returns the full type name
     * @param context
     * @return String log type
     */
    public String getType(Context context){
        return type;
    }

    public Sync getSync() {
        return sync;
    }

    public Date getDatetime() {
        return datetime;
    }

    public static final Creator<SyncLog> CREATOR = new Creator<SyncLog>() {
        @Override
        public SyncLog createFromParcel(Parcel in) {
            return new SyncLog(in);
        }

        @Override
        public SyncLog[] newArray(int size) {
            return new SyncLog[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(sync, i);
        parcel.writeString(type);
        parcel.writeString(message);
    }
}
