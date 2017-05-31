package org.phpnet.openDrivinCloudAndroid.Common;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.securepreferences.SecurePreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by clement on 07/07/16.
 */
public class Settings {
    private static final String TAG = "Settings";
    private static final String APP_SETTINGS = "DRIVINCLOUD_SETTINGS";
    private static final String APP_USERS_CREDENTIALS_FILE = "secret";
    private static final String APP_USERS_CREDENTIALS_SETTING_NAME = "users";
    private static SecurePreferences encodedSharedPrefs;

    private Settings() {
    }

    public static void debug(Context context) {
        System.out.println("___________BEGIN_____________\n" +
                "Debug users saved\n" +
                "____________________________");
        for (User user :
                getUsers(context, null)) {
            System.out.println("SSL : " + (user.isSSL() ? "Yes" : "No"));
            System.out.println("Host : " + user.getHost());
            System.out.println("Path : " + user.getPath());
            System.out.println("CurrentUser : " + user.getUsername());
            System.out.println("Password : " + user.getPassword());
        }
        System.out.println("_____________END____________");
    }

    /**
     * Retrieve the saved users
     * /!\ The first time this function is used, it can take up to 5sec.
     * It's better to load the encoded shared prefs with the initSecurePrefs() function
     * @param context
     * @param host
     * @return
     */
    public static ArrayList<User> getUsers(Context context, @Nullable String host) {
        if(encodedSharedPrefs == null) {
            Log.d(TAG, "getUsers: Secure preferences has not been loaded before calling getUsers");
            encodedSharedPrefs = new SecurePreferences(context, "", APP_USERS_CREDENTIALS_FILE);
        }

        Set<String> usersRaw = encodedSharedPrefs.getStringSet(APP_USERS_CREDENTIALS_SETTING_NAME, new HashSet<String>());
        Gson gson = new Gson();


        ArrayList<User> users = new ArrayList<>();

        for (String userRaw:
                usersRaw) {
            users.add(gson.fromJson(userRaw, User.class));
        }
        return users;
    }

    /**
     * This function is used to instanciate shared preferences asyncronously
     * @param context
     */
    public static void initSecurePrefs(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.d(TAG, "doInBackground: Loading Secure preferences...");
                encodedSharedPrefs = new SecurePreferences(context, "", APP_USERS_CREDENTIALS_FILE); //Retrieve encoded shared prefs, this takes up to 5sec
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Log.d(TAG, "onPostExecute: Done loading Secure preferences");
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    /**
     * Retrieve a user
     * @param context
     * @param host
     * @param username
     * @return null if no user found
     */
    public static User getUser(Context context, boolean useSSL, String host, String path, String username){
        ArrayList<User> tempUsers = getUsers(context, null); //Retrieve saved users from sharedpreferences
        //Search the list for a corresponding user
        for (User user:
             tempUsers) {
            if(user.getUsername().equals(username) && user.getHost().equals(host) && user.isSSL()==useSSL) return user;
        }
        return null; //No user corresponding to the request
    }

    /**
     * Retrieve a user using URI
     * @param context
     * @param serverURL The complete url Eg: https://test.com/path/to/dav
     * @param username
     * @return
     */
    public static User getUser(Context context, Uri serverURL, String username){
        return getUser(context, serverURL.getScheme().equals("https")?true:false, serverURL.getHost(), serverURL.getPath(), username);
    }



    /**
     * Permet d'ajouter un utilisateur dans les préférences à l'aide d'un URI
     * @param context
     * @param uri
     * @param username
     * @param password
     * @return
     */
    public static User addUser(Context context, Uri uri, String username, String password){
        return addUser(context, uri.getScheme().equals("https"), uri.getHost(), uri.getPath(), username, password);
    }

    /**
     * Permet d'ajouter un utilisateur dans les préférences
     * @param context
     * @param useSSL
     * @param host
     * @param path
     * @param username
     * @param password
     * @return
     */
    public static User addUser(Context context, boolean useSSL, String host, String path, String username, String password) {
        if(encodedSharedPrefs == null) {
            Log.d(TAG, "getUsers: Secure preferences has not been loaded before calling getUsers");
            encodedSharedPrefs = new SecurePreferences(context, "", APP_USERS_CREDENTIALS_FILE);
        }

        Gson gson = new Gson();
        Set<String> usersRaw = encodedSharedPrefs.getStringSet(APP_USERS_CREDENTIALS_SETTING_NAME, new HashSet<String>()); //Retrieve users (Json elements set)
        SharedPreferences.Editor prefEdit = encodedSharedPrefs.edit();

        User user = new Settings().new User(username, password, host, path, useSSL);

        usersRaw = deleteUserIfExists(gson, usersRaw, username, host);
        usersRaw.add(user.serialize());

        prefEdit.remove(APP_USERS_CREDENTIALS_SETTING_NAME).putStringSet(APP_USERS_CREDENTIALS_SETTING_NAME, usersRaw).commit();
        Log.d(TAG, "addUser: User "+user.getUsername()+" on "+user.getHost()+" added.");
        return user;
    }

    private static Set<String> deleteUserIfExists(Gson gson, Set<String> usersRaw, String username, String host) {
        Set<String> users = new HashSet<>();
        for (String userRaw:
                usersRaw) {
            User user = gson.fromJson(userRaw, User.class);
            if(!(user.getHost().equals(host) && user.getUsername().equals(username))){
                users.add(userRaw);
            }
        }
        return users;
    }

    public static void removeUser(Context context, User user) {
        if(encodedSharedPrefs == null) {
            Log.d(TAG, "getUsers: Secure preferences has not been loaded before calling getUsers");
            encodedSharedPrefs = new SecurePreferences(context, "", APP_USERS_CREDENTIALS_FILE);
        }
        Gson gson = new Gson();
        Set<String> usersRaw = encodedSharedPrefs.getStringSet(APP_USERS_CREDENTIALS_SETTING_NAME, new HashSet<String>()); //Retrieve users (Json elements set)
        SharedPreferences.Editor prefEdit = encodedSharedPrefs.edit();

        usersRaw = deleteUserIfExists(gson, usersRaw, user.getUsername(), user.getHost());

        prefEdit.remove(APP_USERS_CREDENTIALS_SETTING_NAME).putStringSet(APP_USERS_CREDENTIALS_SETTING_NAME, usersRaw).commit();
    }

    public class User {
        private boolean useSSL;
        private String host;
        private String username;
        private String path;
        private String password;

        public String getHost() {
            return host;
        }

        public String getPassword() {
            return password;
        }

        public String getUsername() {
            return username;
        }

        public User(String username, String password, String host, String path, boolean useSSL) {
            this.password = password;
            this.username = username;
            this.host = host;
            this.path = path;
            this.useSSL = useSSL;
        }

        public Uri getServerURL(){
            return Uri.parse((isSSL()?"https":"http")+"://"+getHost()+getPath());
        }

        @Override
        public String toString() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }

        public boolean isPasswordSaved(){
            return password != "";
        }

        public String getPath() {
            return path;
        }

        public boolean isSSL() {
            return useSSL;
        }

        public void setSSL(boolean useSSL){
            this.useSSL = useSSL;
        }
    }

    private static SecurePreferences getSecurePreference(Context context) {
        return new SecurePreferences(context, "", "users.xml");
    }


    public static void rememberInternalMimeType(Context context, String mimeType) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);

        Set<String> internalMimeTypes = new HashSet<>(sharedPrefs.getStringSet("openInternalMimeTypes", new HashSet<String>()));
        Set<String> externalMimeTypes = new HashSet<>(sharedPrefs.getStringSet("openExternalMimeTypes", new HashSet<String>()));
        if (externalMimeTypes.contains(mimeType)) {
            externalMimeTypes.remove(mimeType);
        }

        if (!internalMimeTypes.contains(mimeType)) {
            internalMimeTypes.add(mimeType);
        }

        sharedPrefs.edit().remove("openExternalMimeTypes").remove("openInternalMimeTypes").putStringSet("openExternalMimeTypes", externalMimeTypes).putStringSet("openInternalMimeTypes", internalMimeTypes).commit();
    }

    public static void rememberExternalMimeType(Context context, String mimeType) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);

        Set<String> internalMimeTypes = new HashSet<>(sharedPrefs.getStringSet("openInternalMimeTypes", new HashSet<String>()));
        Set<String> externalMimeTypes = new HashSet<>(sharedPrefs.getStringSet("openExternalMimeTypes", new HashSet<String>()));
        if (internalMimeTypes.contains(mimeType)) {
            internalMimeTypes.remove(mimeType);
        }

        if (!externalMimeTypes.contains(mimeType)) {
            externalMimeTypes.add(mimeType);
        }

        sharedPrefs.edit().remove("openExternalMimeTypes").remove("openInternalMimeTypes").putStringSet("openInternalMimeTypes", internalMimeTypes).putStringSet("openExternalMimeTypes", externalMimeTypes).commit();
    }

    /**
     * get the current opening policy for a specific mimetype
     *
     * @param context
     * @param mimeType
     * @return internal, external or null if no policy
     */
    public static String getMimeTypeOpenPolicy(Context context, String mimeType) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
        Set<String> internalMimeTypes = sharedPrefs.getStringSet("openInternalMimeTypes", new HashSet<String>());
        Set<String> externalMimeTypes = sharedPrefs.getStringSet("openExternalMimeTypes", new HashSet<String>());
        if (internalMimeTypes.contains(mimeType)) {
            return "internal";
        }
        if (externalMimeTypes.contains(mimeType)) {
            return "external";
        }
        return null;
    }

    public static Set<String> getInternalMimePolicies(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
        Set<String> internalMimeTypes = sharedPrefs.getStringSet("openInternalMimeTypes", new HashSet<String>());
        return internalMimeTypes;
    }

    public static Set<String> getExternalMimePolicies(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
        Set<String> externalMimeTypes = sharedPrefs.getStringSet("openExternalMimeTypes", new HashSet<String>());
        return externalMimeTypes;
    }

    public static Set<String> getMimeTypesList(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
        Set<String> mimetypes = getExternalMimePolicies(context);
        mimetypes.addAll(getInternalMimePolicies(context));
        return mimetypes;
    }

}
