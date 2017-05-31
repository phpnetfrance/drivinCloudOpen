package org.phpnet.openDrivinCloudAndroid.Common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.webdav.lib.WebdavResource;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Model.Account;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.Util.Net.NetworkUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * CurrentUser non-singleton and jackrabbit version
 * Will remplace the other version when it will be deprecated
 */
public class User extends HttpClient{

    private static final String USER_AGENT = "Mozilla/5.0 (Android) drivinCloud-android/%1$s";
    private static final String TAG = User.class.getSimpleName();
    private boolean useSSL;
    public WebdavResource wdr = null;
    public String username = "";
    public String password = "";
    public Uri serverURL;
    public String currentDir = "";
    public LinkedList<String> listPrecDir = new LinkedList<>();

    //Répertoire de l'application
    public String appDir;

    //Liste des images dans un dossier donnée
    public List<MyFile> listImage = new LinkedList<>();

    public int indexCurrentImage;

    //Déplacement en cours ou pas
    public boolean move = false;

    private Toast toast = null;
    private String UID;

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxLetter() {
        int maxLetter = 16;
        return maxLetter;
    }

    /**
     * Instanciates user and loggin
     * @param serverURL
     * @param username
     * @param password
     * @param context
     */
    public User(Uri serverURL, String username, String password, Context context) {
        this.useSSL = serverURL.getScheme().equals("https");
        this.username = username;
        this.serverURL = serverURL;
        this.password = password;
        this.doLogin(context);
    }

    /**
     * Instanciate user, doesn't connect
     * @param serverURL
     * @param username
     * @param password
     */
    public User(Uri serverURL, String username, String password){
        this.useSSL = serverURL.getScheme().equals("https");
        this.serverURL = serverURL;
        this.username = username;
        this.password = password;
    }

    public void doLogin(Context context){
        if(wdr == null) {
            try {
                Thread thread = login(context);
                thread.start();
                thread.join();
                if (wdr != null) {
                    //Success
                    Log.d(TAG, "doLogin: User "+ username +" successfully logged in");
                } else {
                    //Fail
                    Log.d(TAG, "doLogin: Can't connect to webdav server");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            Log.d(TAG, "doLogin: User is already logged in");
        }
    }

    /**
     * Get all the uploads for a user
     * This methods returns the whole upload list
     * @return All the uploads for a user
     */
    public RealmList<Upload> getUploadList(){
        Realm DB = Realm.getDefaultInstance();
        RealmQuery<Account> queryAccount = DB.where(Account.class);
        queryAccount.equalTo(Account.FIELD_USERNAME, username)
                .equalTo(Account.FIELD_HOSTNAME, serverURL.getHost())
                .equalTo(Account.FIELD_PATH, serverURL.getPath());
        return queryAccount.findFirst().getUploads();
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

    /*
    * Authentification sur le serveur de backup
    * Exception en cas d'echec
    * */
    private Thread login(final Context context) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    //We register an advanced SSL context with better SSL Certificate checking
                    NetworkUtils.registerAdvancedSslContext(true, context);

                    HttpURL hrl;
                    if(useSSL){
                        hrl = new HttpsURL(serverURL.toString());
                    }else{
                        hrl = new HttpURL(serverURL.toString());
                    }
                    hrl.setUserinfo(username, password);

                    wdr = new WebdavResource(hrl);

                    CurrentUser currentUser = CurrentUser.getInstance();
                    currentUser.wdr = wdr;
                    currentUser.setOkHttpClient(new OkHttpClient.Builder()
                        .authenticator(new Authenticator() {
                            @Override
                            public Request authenticate(Route route, Response response) throws IOException {
                                Log.d(TAG, "okHttpAuthenticate: auth for response "+response);
                                String creds = Credentials.basic(username, password);
                                return response.request().newBuilder()
                                        .header("Authorization", creds)
                                        .build();
                            }
                        })
                        .build());
                    currentUser.username = username;
                    currentUser.password = password;
                    currentUser.serverURL = serverURL;
                    currentUser.appDir = getAppDir(context) + "/";
                } catch (Exception e) {
                    wdr = null;
                    e.printStackTrace();
                }
            }
        });
        return thread;
    }

    public String currentDirURL() {
        return serverURL + currentDir;
    }

    public String currentDirName() {
        /*Renvoi le titre du dossier courant*/
        if (this.currentDir.equals("")) {
            return username;
        } else {
            String curr = this.currentDir;
            curr = curr.substring(0, curr.length() - 1);
            int i = curr.lastIndexOf("/");
            if (i < 0)
                return curr + "/";
            else
                return curr.substring(i + 1, curr.length()) + "/";
        }
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

    /**
     * Executes a method using the jackrabbit webdav lib
     *
     * @param method method request
     * @return http response code
     */
    public int executeMethod(HttpMethod method) throws IOException{
        HttpParams params = method.getParams();
        String userAgent = USER_AGENT;
        params.setParameter(HttpMethodParams.USER_AGENT, USER_AGENT);

        Log.d(TAG, "REQUEST " + method.getName() + " " + method.getPath());

        int status = super.executeMethod(method);
        method.releaseConnection();

        return status;
    }

    /**
     * Get the account model corresponding to the user
     * @return The user account entry in the database
     */
    public Account getDbEntry(){
        Log.d(TAG, "Trying to retrieve Account entry in DB for user " + username);
        final Realm DB = Realm.getDefaultInstance();
        RealmQuery<Account> queryAccount = DB.where(Account.class);
        Log.d(TAG, "getDbEntry: Query username:url "+username+":"+serverURL);
        queryAccount.equalTo(Account.FIELD_USERNAME, username)
                .equalTo(Account.FIELD_HOSTNAME, serverURL.getHost())
                .equalTo(Account.FIELD_PATH, serverURL.getPath());

        // Lets create the entry if it doesn't exist
        if(queryAccount.count() < 1){
            Log.d(TAG, "No entry corresponding to user " + username + "\n" +
                    "Creating new entry");
            DB.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Account account = new Account(username, serverURL);
                    DB.copyToRealm(account);
                }
            });
        }
        return queryAccount.findFirst();
    }

    public boolean isPasswordSaved(){
        return password != "";
    }

    public String getUsername() {
        return username;
    }

    /**
     * Gets a string with this form:
     * USER@HOSTNAME
     * @return unique id
     */
    public String getUID() {
        if(UID == null){
            UID = username+"@"+serverURL;
        }
        return UID;
    }

    public Uri getUrl() {
        return serverURL;
    }

    public boolean useSSL() {
        return useSSL;
    }

    public String getHostname() {
        return serverURL.getHost();
    }
}
