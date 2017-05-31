package org.phpnet.openDrivinCloudAndroid.Model.Migration;

import org.phpnet.openDrivinCloudAndroid.Model.Account;
import org.phpnet.openDrivinCloudAndroid.Model.FileUpload;

import java.util.Date;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

/**
 * Created by clement on 02/02/17.
 */

public class Migration implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema realmSchema = realm.getSchema();

        /*
            Migrate from version 0 :
                - A Sync Log table has been added for more transparency
         */
        if(oldVersion == 0){
            //Add the log table
            RealmObjectSchema syncLogSchema = realmSchema.create("SyncLog")
                    .addRealmObjectField("sync", realmSchema.get("Sync"))
                    .addField("datetime", Date.class)
                    .addField("type", String.class)
                    .addField("message", String.class);

            //Add the external key on the sync table
            realmSchema.get("Sync")
                    .addRealmListField("log", syncLogSchema);
        }

        /*
            Migrate from version 2 :
                - A boolean field as been added to the use to persist ssl / notssl
         */
        if(oldVersion == 2){
            realmSchema.get("Account")
                    .addField(Account.FIELD_USE_SSL, Boolean.class);
        }

        if(oldVersion == 3){
            realmSchema.get("Account")
                    .addField(Account.FIELD_PATH, String.class)
                    .removeField("url");
        }

        if(oldVersion == 4){
            realmSchema.get("FileUpload")
                    .addField(FileUpload.FIELD_DONE, Boolean.class)
                    .addField(FileUpload.FIELD_FILE_NAME, String.class);
        }
    }
}
