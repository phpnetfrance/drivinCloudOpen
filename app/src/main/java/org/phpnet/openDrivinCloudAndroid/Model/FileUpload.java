package org.phpnet.openDrivinCloudAndroid.Model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.io.FilenameUtils;

import io.realm.RealmObject;

/**
 * Created by clement on 11/08/16.
 */
public class FileUpload extends RealmObject implements Parcelable {
    public static final String FIELD_UPLOAD = "upload";
    public static final String FIELD_DONE = "done";
    public static final String FIELD_FILE_NAME = "fileName";
    public static final String FIELD_LOCAL_PATH = "localPath";
    public static final String FIELD_DISTANT_PATH = "distPath";

    public Upload upload; // The uploads operation corresponding to this file
    public Boolean done; // Used for synchronisation only
    public String fileName; // Used for sync only
    public String localPath; // The local path of this file
    public String distPath; // The distant path where the file got/gotta be uploaded

    public FileUpload(){
        this.done = false;
    }

    public FileUpload(Upload upload, String localPath, String distPath) {
        this.upload = upload;
        this.localPath = localPath;
        this.distPath = distPath;
        this.done = false;
        this.fileName = Uri.parse(localPath).getLastPathSegment();
    }

    public boolean isSync(){
        return this.upload.sync != null;
    }

    public Sync getSync(){
        return this.upload.sync;
    }

    public String getLocalName(){
        return FilenameUtils.getName(localPath);
    }

    public String getDistName(){
        return FilenameUtils.getName(distPath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.upload, flags);
        dest.writeByte((byte) (done ? 1 : 0));
        dest.writeString(this.fileName);
        dest.writeString(this.localPath);
        dest.writeString(this.distPath);
    }

    protected FileUpload(Parcel in) {
        this.upload = in.readParcelable(Upload.class.getClassLoader());
        this.done = in.readByte() != 0;
        this.fileName = in.readString();
        this.localPath = in.readString();
        this.distPath = in.readString();
    }

    public static final Parcelable.Creator<FileUpload> CREATOR = new Parcelable.Creator<FileUpload>() {
        @Override
        public FileUpload createFromParcel(Parcel source) {
            return new FileUpload(source);
        }

        @Override
        public FileUpload[] newArray(int size) {
            return new FileUpload[size];
        }
    };
}
