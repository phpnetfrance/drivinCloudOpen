package org.phpnet.openDrivinCloudAndroid;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phpnet.openDrivinCloudAndroid.Common.DavProvider;
import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.Model.Account;

import java.net.URISyntaxException;

import io.realm.Realm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest {
    private static final String TAG = "ApplicationTest";

    private static String PACKAGE_NAME;
    private static final String DAV_HOSTNAME = "drive5650.phpnetstorage.eu";
    private static final boolean DAV_USESSL = true;
    private static final String DAV_USERNAME = "drive5650";
    private static final String DAV_PASSWORD = "13563290";
    Account account;
    Settings.User user;
    Context context;

    @Before
    public void setup(){
        Log.d(TAG, "setup: BEGIN");
        Uri davUrl =  Uri.parse((DAV_USESSL?"https":"http")+"://"+ DAV_HOSTNAME);
        context = InstrumentationRegistry.getContext();
        PACKAGE_NAME = context.getPackageName();
        Settings.initSecurePrefs(context);
        user = Settings.addUser(context, davUrl, DAV_USERNAME, DAV_PASSWORD);
        Realm.init(context);
        final Realm db = Realm.getDefaultInstance();

        db.beginTransaction();
        Account user = db.where(Account.class).equalTo(Account.FIELD_HOSTNAME, "drive5650.phpnet.org").equalTo(Account.FIELD_USERNAME, "drive5650").findFirst();
        if(user != null){
            user.deleteFromRealm();
            Log.d(TAG, "execute: user exists");
        }
        account = new Account(DAV_USERNAME, davUrl);
        db.commitTransaction();

        Log.d(TAG, "setup: END");
    }

    @Test
    public void isAccountSet(){
        assertTrue("Account not set", account != null);
        assertEquals("hostname missmatch", account.getHostname(), DAV_HOSTNAME);
        assertEquals("username missmatch", account.getUsername(), DAV_USERNAME);
        assertEquals("password missmatch", account.getPassword(context), DAV_PASSWORD);
    }

    @Test
    public void test_userRegisteredInSecuredSharedPrefs() throws URISyntaxException {
        Log.d(TAG, "test_userRegisteredInSecuredSharedPrefs: Retrieving user "+account.getUsername()+ " on " + account.getHostname());
        Settings.User user = Settings.getUser(context, account.getUrl(), account.getUsername());
        assertEquals("Hostname missmatch", user.getHost(), account.getHostname());
        assertEquals("Username missmatch", user.getUsername(), account.getUsername());
    }

    @Test
    public void test_passwordCorrespondanceWorking(){
        Log.d(TAG, "test_passwordCorrespondanceWorking: User "+account.getUsername()+" on "+account.getHostname());
        assertEquals(account.getPassword(context), DAV_PASSWORD);
    }

    @Test
    public void test_getFiles() throws Exception {
        System.out.println("Hostname: "+account.getHostname());
        DavProvider prov = new DavProvider(account, context);
        prov.getItems("/");
    }
}