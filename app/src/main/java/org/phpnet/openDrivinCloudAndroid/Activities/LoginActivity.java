package org.phpnet.openDrivinCloudAndroid.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.webdav.lib.WebdavResource;
import org.phpnet.openDrivinCloudAndroid.Adapter.UserAdapter;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.Model.Account;
import org.phpnet.openDrivinCloudAndroid.R;
import org.phpnet.openDrivinCloudAndroid.Util.Net.NetworkUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import io.realm.Realm;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;

public class LoginActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;


    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private FloatingActionButton fab;
    protected static Realm DB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DB = Realm.getDefaultInstance();
        //If user is already instanciated, skip login
        if (CurrentUser.getInstance().wdr != null) {
            finish();
            startActivity(new Intent(this, AcceuilActivity.class));
        }




        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int pos) {
                //Hide virtual keyboard
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                //Change the fab image
                if (pos == 0) {
                    fab.setImageResource(R.drawable.ic_person_add_white_24dp);
                } else {
                    fab.setImageResource(R.drawable.ic_people_white_24dp);
                }

                //Remove actionmode
                if (LoginFragment.mActionMode != null) {
                    LoginFragment.mActionMode.finish();
                }
            }
        });


        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FloatingActionButton fab = (FloatingActionButton) view;
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() ^ 1);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static class LoginFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String TAG = LoginFragment.class.getSimpleName();
        private WebdavResource wdr = null;
        private HttpsURL hrl = null;
        private Thread thread = null;
        private Uri serverURLObj;
        private String username = "";
        private String password = "";
        private ListView listView;
        private UserAdapter adapter;
        public static ActionMode mActionMode;
        private Settings.User user;
        private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

            // Called when the action mode is created; startActionMode() was called
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate a menu resource providing context menu items
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.account_list_context_menu, menu);
                return true;
            }

            // Called each time the action mode is shown. Always called after onCreateActionMode, but
            // may be called multiple times if the mode is invalidated.
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false; // Return false if nothing is done
            }

            // Called when the user selects a contextual menu item
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete_context_menu:
                        final Account account = DB.where(Account.class)
                                .equalTo(Account.FIELD_HOSTNAME, user.getHost())
                                .equalTo(Account.FIELD_USERNAME, user.getUsername())
                                .findFirst();
                        if(account != null){
                            Log.d(TAG, "onActionItemClicked: Delete account | "+account.getUsername());
                            DB.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    account.deleteFromRealm();
                                    Settings.removeUser(getContext(), user);
                                    adapter.update(Settings.getUsers(getContext(), null));
                                    mActionMode.finish();
                                }
                            });
                        }else{
                            Log.d(TAG, "onActionItemClicked: Delete account (with no password) | "+user.getUsername());
                            Settings.removeUser(getContext(), user);
                            adapter.update(Settings.getUsers(getContext(), null));
                            mActionMode.finish();
                        }
                        break;
                    case R.id.edit_context_menu:
                        LinearLayout form = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.edit_user, null);
                        final EditText password = (EditText) form.findViewById(R.id.input_password);
                        final TextInputLayout passwordFieldWrapper = (TextInputLayout) form.findViewById(R.id.input_password_wrapper);

                        final MaterialDialog credentialsChangeDialog = new MaterialDialog.Builder(getContext())
                                .autoDismiss(false)
                                .title(R.string.edit_user_info)
                                .customView(R.layout.edit_user, true)
                                .customView(form, true)
                                .positiveText(R.string.confirm_edit_user)
                                .negativeText(R.string.cancel_edit_user)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        if(checkLogin(user.getUsername(), password.getText().toString())){
                                            final String usernameString = user.getUsername();
                                            Settings.addUser(getContext(), user.getServerURL(), usernameString, password.getText().toString());
                                            user = Settings.getUser(getContext(), user.getServerURL(), usernameString);
                                            adapter.update(Settings.getUsers(getContext(), null));
                                            dialog.dismiss();
                                        }else{
                                            Toast.makeText(getContext(), R.string.authEditError, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }).onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                }).build();

                        credentialsChangeDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);

                        password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                Log.d(TAG, "onFocusChange: 2");
                                if (TextUtils.isEmpty(password.getText())) {
                                    passwordFieldWrapper.setError(getText(R.string.error_field_required));
                                    credentialsChangeDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                                } else {
                                    passwordFieldWrapper.setErrorEnabled(false);
                                    credentialsChangeDialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                }
                            }
                        });

                        password.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                Log.d(TAG, "onFocusChange: 1");
                                if (TextUtils.isEmpty(password.getText())) {
                                    passwordFieldWrapper.setError(getText(R.string.error_field_required));
                                    credentialsChangeDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                                } else {
                                    passwordFieldWrapper.setErrorEnabled(false);
                                    credentialsChangeDialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                }
                            }
                        });

                        password.setText(user.getPassword());
                        credentialsChangeDialog.show();
                        break;
                    default:
                        return false;
                }
                return true;
            }

            // Called when the user exits the action mode
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
                listView.setSelector(R.color.colorPrimary);

                //Dirty workaround AOSP bug...
                listView.clearChoices();
                for (int i = 0; i < listView.getCount(); i++)
                    listView.setItemChecked(i, false);
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
                    }
                });

            }
        };

        private boolean checkLogin(String username, String password) {
            this.username = username;
            this.password = password;
            thread = login();
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (wdr != null) {
                return true;
            }
            return false;
        }


        public LoginFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static LoginFragment newInstance(int sectionNumber) {
            LoginFragment fragment = new LoginFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = null;
            if (this.getArguments().getInt(ARG_SECTION_NUMBER, 2) == 2) { //Show login page
                rootView = inflater.inflate(R.layout.fragment_main_auth_new, container, false);
                Button connectButton = (Button) rootView.findViewById(R.id.btn_login);
                TextView createAccount = (TextView) rootView.findViewById(R.id.create_account_link);
                createAccount.setMovementMethod(LinkMovementMethod.getInstance());
                Spinner useSSLSpinner = (Spinner) rootView.findViewById(R.id.input_login_ssl);
                ArrayAdapter<CharSequence> sslAdapter = ArrayAdapter.createFromResource(this.getContext(), R.array.login_ssl_choices, android.R.layout.simple_spinner_item);
                sslAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                useSSLSpinner.setAdapter(sslAdapter);
                final Switch rememberUsernameField = (Switch) rootView.findViewById(R.id.switch_save_username);
                final Switch rememberPasswordField = (Switch) rootView.findViewById(R.id.switch_save_password);
                rememberPasswordField.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        rememberUsernameField.setChecked((isChecked) ? true : rememberUsernameField.isChecked());
                    }
                });
                rememberUsernameField.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        rememberPasswordField.setChecked((!isChecked) ? false : rememberPasswordField.isChecked());
                    }
                });

                connectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Do Login
                        ViewGroup loginForm = (ViewGroup) view.getParent();
                        Spinner useSSLSpinner = (Spinner) loginForm.findViewById(R.id.input_login_ssl);
                        TextInputLayout hostnameFieldWrapper = (TextInputLayout) loginForm.findViewById(R.id.input_login_hostname);
                        TextInputLayout loginFieldWrapper = (TextInputLayout) loginForm.findViewById(R.id.input_login_wrapper);
                        TextInputLayout passwordFieldWrapper = (TextInputLayout) loginForm.findViewById(R.id.input_password_wrapper);

                        boolean useSSL = useSSLSpinner.getSelectedItem().toString().equals("https://");
                        String serverHostname = hostnameFieldWrapper.getEditText().getText().toString();
                        boolean hostnameIsValid = false;
                        serverURLObj = Uri.parse(useSSLSpinner.getSelectedItem().toString()+serverHostname);
                        hostnameIsValid = true;
                        username = loginFieldWrapper.getEditText().getText().toString();
                        password = passwordFieldWrapper.getEditText().getText().toString();
                        boolean rememberUsername = rememberUsernameField.isChecked();
                        boolean rememberPassword = rememberPasswordField.isChecked();

                        if (!hostnameIsValid || TextUtils.isEmpty(hostnameFieldWrapper.getEditText().getText()) || TextUtils.isEmpty(loginFieldWrapper.getEditText().getText()) || TextUtils.isEmpty(passwordFieldWrapper.getEditText().getText())) {
                            if(!hostnameIsValid){
                                hostnameFieldWrapper.setError("Nom d'hôte invalide");
                            }else{
                                hostnameFieldWrapper.setErrorEnabled(false);
                            }

                            //Do validate fields
                            if (TextUtils.isEmpty(hostnameFieldWrapper.getEditText().getText())) {
                                hostnameFieldWrapper.setError(getText(R.string.error_field_required));
                            } else if(hostnameIsValid) {
                                hostnameFieldWrapper.setErrorEnabled(false);
                            }

                            if (TextUtils.isEmpty(loginFieldWrapper.getEditText().getText())) {
                                loginFieldWrapper.setError(getText(R.string.error_field_required));
                            } else {
                                loginFieldWrapper.setErrorEnabled(false);
                            }

                            if (TextUtils.isEmpty(passwordFieldWrapper.getEditText().getText())) {
                                passwordFieldWrapper.setError(getText(R.string.error_field_required));
                            } else {
                                passwordFieldWrapper.setErrorEnabled(false);
                            }
                        } else {
                            //Do try login and change activity if ok
                            doTryLogin(rememberUsername, rememberPassword);
                        }

                    }
                });
            } else { //Show saved user list
                rootView = inflater.inflate(R.layout.fragment_main_auth_list, container, false);
                listView = (ListView) rootView.findViewById(R.id.account_list);
                ArrayList<Settings.User> savedCredentials = Settings.getUsers(getContext(), null);
                adapter = new UserAdapter(getContext(), savedCredentials);
                listView.setAdapter(adapter);
                listView.setEmptyView(rootView.findViewById(android.R.id.empty));

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        user = (Settings.User) listView.getAdapter().getItem(i);
                        if (mActionMode == null) {
                            username = user.getUsername();
                            password = user.getPassword();
                            serverURLObj = user.getServerURL();
                            Log.d(TAG, "onItemClick: "+serverURLObj);
                            if(password.equals("")){
                                new MaterialDialog.Builder(getContext())
                                        .title(R.string.dialog_ask_password)
                                        .customView(R.layout.dialog_ask_password, true)
                                        .positiveText(R.string.dialog_ask_password_positive)
                                        .negativeText(R.string.dialog_ask_password_negative)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                Switch savePasswordSwitch = (Switch) dialog.findViewById(R.id.switch_save_password);
                                                EditText passwordInput = (EditText) dialog.findViewById(R.id.input_password);
                                                boolean savePass = savePasswordSwitch.isChecked();
                                                password = passwordInput.getText().toString();
                                                doTryLogin(true, savePass);

                                            }
                                        })
                                        .show();
                            }else {
                                doTryLogin(false);
                            }
                        }
                    }
                });

                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {


                        user = (Settings.User) listView.getAdapter().getItem(i);
                        if (mActionMode != null) {
                            return false;
                        }

                        mActionMode = getActivity().startActionMode(mActionModeCallback);
                        listView.setSelector(R.color.colorPrimaryDark);
                        listView.post(new Runnable() {
                            @Override
                            public void run() {
                                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                            }
                        });
                        view.setSelected(true);
                        return true;


                    }
                });
            }

            return rootView;
        }

        private void doTryLogin(boolean rememberUsername, boolean rememberPassword) {
            if(rememberPassword){
                doTryLogin(true);
            }else if(rememberUsername){
                thread = login();
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (wdr != null) {
                    saveUser(false);
                    getActivity().finish();
                    startActivity(new Intent(getContext(), AcceuilActivity.class));
                } else {
                    Toast.makeText(getContext(), R.string.authError, Toast.LENGTH_SHORT).show();
                }
            }else{
                thread = login();
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (wdr != null) {
                    getActivity().finish();
                    startActivity(new Intent(getContext(), AcceuilActivity.class));
                } else {
                    Toast.makeText(getContext(), R.string.authError, Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void doTryLogin(boolean remember) {
            try {
                thread = login();
                thread.start();
                thread.join();
                if (wdr != null) {
                    if(remember) saveUser();
                    getActivity().finish();
                    startActivity(new Intent(getContext(), AcceuilActivity.class));
                } else {
                    Toast.makeText(getContext(), R.string.authError, Toast.LENGTH_SHORT).show();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void saveUser() {
            DB.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Log.d(TAG, "Searching for user: "+serverURLObj.getHost()+"|"+username+"|"+serverURLObj.getScheme().equals("https"));
                    Account user = DB.where(Account.class)
                            .equalTo(Account.FIELD_HOSTNAME, serverURLObj.getHost())
                            .equalTo(Account.FIELD_USERNAME, username)
                            .equalTo(Account.FIELD_PATH, serverURLObj.getPath())
                            .equalTo(Account.FIELD_USE_SSL, serverURLObj.getScheme().equals("https"))
                            .findFirst();
                    Log.d(TAG, "saveUser: found user?"+ (user==null ? " No" : " Yes->"+user.getUsername()));
                    if(user != null) user.deleteFromRealm();
                    user = new Account(username, serverURLObj);
                    realm.copyToRealm(user);
                }
            });
            Settings.addUser(getContext(), serverURLObj, username, password);
        }

        private void saveUser(boolean rememberPassword) {
            if(rememberPassword) {
                saveUser();
            }else{
                Settings.addUser(getContext(), serverURLObj, username, "");
            }
        }

        /*
        * Authentification sur le serveur de backup
        * Exception en cas d'echec
        * */
        private Thread login() {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        NetworkUtils.registerAdvancedSslContext(true, getContext());
                        HttpURL hrl;
                        Log.d(TAG, "Login thread: Logging to server "+serverURLObj.toString());
                        if(serverURLObj.getScheme().equals("https")) {
                            hrl = new HttpsURL(serverURLObj.toString());
                        }else{
                            hrl = new HttpURL(serverURLObj.toString());
                        }
                        hrl.setUserinfo(username, password);

                        wdr = new WebdavResource(hrl);

                        CurrentUser currentUser = CurrentUser.getInstance();
                        currentUser.wdr = wdr;
                        OkHttpClient httpClient = new OkHttpClient.Builder()
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
                                .addInterceptor(new HttpLoggingInterceptor()
                                        .setLevel(HttpLoggingInterceptor.Level.BASIC))
                                .build();
                        currentUser.setOkHttpClient(httpClient);
                        currentUser.username = username;
                        currentUser.password = password;
                        currentUser.serverURL = serverURLObj;
                        currentUser.appDir = getAppDir() + "/";
                    } catch (HttpException e) {
                        e.printStackTrace();
                        wdr = null;
                    } catch (IOException e){
                        e.printStackTrace();
                        wdr = null;
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        wdr = null;
                    }
                }
            });
            return thread;
        }

        /*
        * Renvoie le répertoire de l'application
        * */
        private String getAppDir() {
            PackageManager m = getActivity().getPackageManager();
            PackageInfo p = null;
            try {
                p = m.getPackageInfo(getActivity().getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return p.applicationInfo.dataDir;
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return LoginFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Vos comptes";
                case 1:
                    return "Ajouter un compte";
            }
            return null;
        }
    }
}

