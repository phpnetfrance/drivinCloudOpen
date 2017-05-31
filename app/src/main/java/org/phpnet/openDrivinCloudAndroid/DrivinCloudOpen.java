package org.phpnet.openDrivinCloudAndroid;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.phpnet.openDrivinCloudAndroid.Model.Migration.Migration;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by clement on 26/07/16.
 */
@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "https://trace.phpnet.org/acra-drivincloud/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "drivincloud-report",
        formUriBasicAuthPassword = "ze6goo7"
)
public class DrivinCloudOpen extends MultiDexApplication {
    private static final String TAG = "DrivinCloudOpen";
    private static DrivinCloudOpen app;
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        app = this;

        Realm.init(this);

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .schemaVersion(BuildConfig.REALM_SCHEMA_VERSION)
                .migration(new Migration())
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);

        ACRA.init(this);
    }

    public static DrivinCloudOpen getInstance(){
        return app;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
