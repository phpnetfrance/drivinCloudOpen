package org.phpnet.openDrivinCloudAndroid;

import com.facebook.stetho.Stetho;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;


/**
 * Created by clement on 31/05/17.
 */

public class DrivinCloudOpenDebug extends DrivinCloudOpen {
    @Override
    public void onCreate(){
        super.onCreate();
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build());
    }
}
