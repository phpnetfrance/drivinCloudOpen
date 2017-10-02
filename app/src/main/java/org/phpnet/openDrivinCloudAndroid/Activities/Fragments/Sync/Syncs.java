package org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Sync;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import org.phpnet.openDrivinCloudAndroid.Adapter.SyncsListAdapter;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Component.SyncsListDivider;
import org.phpnet.openDrivinCloudAndroid.Model.Sync;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.File;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by clement on 29/07/16.
 */
public class Syncs extends Fragment {
    private static final String TAG = Syncs.class.getSimpleName();

    //Sync stat vars ==> If changed, must be updated in xml too
    public static final String AUTHORITY = "org.phpnet.drivincloud.sync.provider";
    public static final String ACCOUNT_TYPE = "drivincloud.phpnet.org";
    public static final String ACCOUNT = "default_account";

    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 2L;
    public static final long DEFAULT_SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;
    public static final String KEYSYNCINTERVAL = "pref_sync_config_check_interval";
    Account mAccount;
    private SharedPreferences sharedPref;


    public static final int REQUEST_DIRECTORY = 48789;
    private static final String EXTRA_SYNC = "sync";
    RecyclerView mRecyclerView;
    RealmList<Sync> syncList;
    SyncsListAdapter mAdapter;

    Realm DB;
    private TextView mEmptyView;
    private Sync newSync;
    private MaterialDialog newSyncDialog;
    private boolean isReselectingFolder = false; //Used to check if we reopen the sync dialog or not
    private FloatingActionButton fabNewSync;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DB = Realm.getDefaultInstance();
        setHasOptionsMenu(true);
        initSyncList();
        mAccount = CreateSyncAccount(getContext());
        //sharedPref = getActivity().getSharedPreferences(getContext().getPackageName()+"_preferences", Context.MODE_PRIVATE);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        //Set the sync frequency
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
        long interval;
        try {
            interval = Long.parseLong(sharedPref.getString(Syncs.KEYSYNCINTERVAL, String.valueOf(Syncs.DEFAULT_SYNC_INTERVAL))) * 60;
        }catch(NumberFormatException e){
            //TODO Default interval is set in case of parse problem
            interval = 1800;
        }
        Log.d(TAG, "onCreate: Set sync frequency to "+interval+" sec");
        ContentResolver.addPeriodicSync(
                mAccount,
                AUTHORITY,
                Bundle.EMPTY,
                interval);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_GRANT_REQUEST_CODE_FILECHOOSER: {
                for(int i = 0; i<grantResults.length; i++){
                    Log.d(TAG, "onRequestPermissionsResult: grantResults["+permissions[i]+"]="+grantResults[i]);
                }
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Intent i = getActivity().getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage( getActivity().getBaseContext().getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }else{
                    new MaterialDialog.Builder(getContext())
                            .content("Impossible d'accéder à vos fichiers.")
                            .build()
                            .show();
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // Clear default menu entries as we don't need them
        inflater.inflate(R.menu.menu_sync_fragment, menu); //Add an entry / icon to check sync
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.check_syncs) {
            Log.d(TAG, "onOptionsItemSelected: Refresh sync button clicked, forcing sync verification");

            //Create settings bundle
            Bundle syncSettings = new Bundle();
            syncSettings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); //Force manual sync
            syncSettings.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); //Force sync to start instantly
            ContentResolver.requestSync(mAccount, AUTHORITY, syncSettings);
        }
        return super.onOptionsItemSelected(item);
    }

    private void initSyncList() {
        CurrentUser currentUser = CurrentUser.getInstance();
        syncList = currentUser.getSyncList();
        if(syncList == null){
            Log.d(TAG, "Retrieving syncs list..." +
                    "\n"+ currentUser.username+"'s syncs count : null");
        }else{
            Log.d(TAG, "Retrieving syncs list..." +
                    "\n"+ currentUser.username+"'s syncs count : " + syncList.size());
        }
    }





    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_syncs, container, false);
        rootView.setTag(TAG);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.syncsRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mEmptyView = (TextView) rootView.findViewById(R.id.empty_view);
        if(syncList == null || syncList.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }else{
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mAdapter = new SyncsListAdapter(syncList, DB);

        RecyclerView.ItemDecoration divider = new SyncsListDivider(ResourcesCompat.getDrawable(getResources(), R.drawable.syncs_list_divider, null), 10, 300);
        mRecyclerView.addItemDecoration(divider);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {}

            @Override
            public void onLongClick(View view, int position) {
                final Sync sync = ((SyncsListAdapter) mRecyclerView.getAdapter()).getSyncAt(position);
                MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                        .icon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_error_outline_black_24dp, null))
                        .positiveColor(ResourcesCompat.getColor(getResources(), R.color.delete_message, null))
                        .title(R.string.dialog_2check_before_delete_sync)
                        .positiveText(R.string.dialog_confirm_deletion)
                        .negativeText(R.string.dialog_cancel_deletion)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                DB.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Log.d(TAG, "execute: Delete sync "+sync.getName());
                                        RealmList<Upload> uploads = sync.getUploads();
                                        if(uploads != null) {
                                            for (Upload upload :
                                                    uploads) {
                                                upload.files.deleteAllFromRealm(); //1 Delete fileuploads
                                            }
                                            uploads.deleteAllFromRealm(); //2 delete uploads
                                        }
                                        sync.deleteFromRealm(); //3 delete sync
                                        //TODO Hurray! Got rid of the sync: Should implement cascade delete / Wait for realm to do it because this is kinda ugly
                                        mRecyclerView.setAdapter(new SyncsListAdapter(syncList, DB));
                                        if(syncList.size() == 0){
                                            mEmptyView.setVisibility(View.VISIBLE);
                                        }
                                    }
                                });
                            }
                        }).show();
            }


        }));


        //Handling new sync
        fabNewSync = (FloatingActionButton) rootView.findViewById(R.id.newSyncButton);
        fabNewSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newSync = new Sync();
                newSync.setAccount(CurrentUser.getInstance().getDBEntry());
                LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                LinearLayout form = (LinearLayout) layoutInflater.inflate(R.layout.fragment_syncs_add_new_sync_dialog1, null);

                EditText folderName = (EditText) form.findViewById(R.id.name);
                //Set an input filter on the edittext to prevent special chars
                final String usableChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
                InputFilter filter = new InputFilter() {
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (!usableChars.contains(""+source.charAt(i))) {
                                return "";
                            }
                        }
                        return null;
                    }
                };
                folderName.setFilters(new InputFilter[]{filter});

                final Intent chooserIntent = new Intent(getContext(), CustomDirectoryChooser.class);

                DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                        .newDirectoryName("Sync")
                        .allowReadOnlyDirectory(true)
                        .allowNewDirectoryNameModification(true)
                        .build();

                chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);


                //Check permissions
                Log.d(TAG, "onOptionsItemSelected: Checking permissions for READ_EXTERNAL_STORAGE ("+PackageManager.PERMISSION_GRANTED+"|"+PackageManager.PERMISSION_DENIED+") : "+ ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE));
                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(Syncs.this.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Log.d(TAG, "onOptionsItemSelected: Showing information dialog");
                        new MaterialDialog.Builder(getContext())
                                .content(R.string.ask_read_ext_sto_perm)
                                .onAny(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        Log.d(TAG, "onOptionsItemSelected: Requestion permissions");
                                        ActivityCompat.requestPermissions(Syncs.this.getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_GRANT_REQUEST_CODE_FILECHOOSER);
                                    }
                                })
                                .positiveText(R.string.ok)
                                .build()
                                .show();
                    }else {
                        Log.d(TAG, "onOptionsItemSelected: Requestion permissions "+new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}.toString());
                        ActivityCompat.requestPermissions(Syncs.this.getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_GRANT_REQUEST_CODE_FILECHOOSER);
                    }
                }else{
                    //Launch folder chooser
                    startActivityForResult(chooserIntent, REQUEST_DIRECTORY);

                    TextView folderNamePlaceholder = (TextView) form.findViewById(R.id.folderNamePlaceHolder);
                    folderNamePlaceholder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "onClick: Lets change the folder");
                            newSyncDialog.dismiss();
                            isReselectingFolder = true;
                            startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
                        }
                    });

                    //Create the dialog if it doesn't exist
                    if(newSyncDialog == null) {
                        newSyncDialog = new MaterialDialog.Builder(v.getContext())
                                .title(R.string.add_new_sync)
                                .icon(ContextCompat.getDrawable(getContext(), R.drawable.ic_sync_black_24dp))
                                .customView(form, true)
                                .positiveText(R.string.syncs_next_validate)
                                .negativeText(R.string.cancel)
                                .autoDismiss(false)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        EditText name = (EditText) dialog.findViewById(R.id.name);
                                        RadioGroup netType = (RadioGroup) dialog.findViewById(R.id.network_type);
                                        RadioGroup charging = (RadioGroup) dialog.findViewById(R.id.charging);
                                        RadioGroup freq = (RadioGroup) dialog.findViewById(R.id.frequency);
                                        RadioButton checkedNetType = (RadioButton) dialog.findViewById(netType.getCheckedRadioButtonId());
                                        RadioButton checkedCharging = (RadioButton) dialog.findViewById(netType.getCheckedRadioButtonId());
                                        RadioButton checkedFrequency = (RadioButton) dialog.findViewById(freq.getCheckedRadioButtonId());


                                        //Check if the name is available
                                        Log.d(TAG, "onClick: " + TextUtils.isEmpty(name.getText()));
                                        TextUtils.isEmpty(name.getText());

                                        boolean nameIsOk = true;
                                        if (name.getText().toString().isEmpty()) {
                                            dialog.show();
                                            nameIsOk = false;
                                        }

                                        Sync res = DB.where(Sync.class).equalTo("name", name.getText().toString()).findFirst();
                                        if (res != null) { //TODO Test this
                                            Toast.makeText(getContext(), R.string.name_already_taken, Toast.LENGTH_LONG).show();
                                            dialog.show();
                                            nameIsOk = false;
                                        }

                                        Log.d(TAG, "Results | Name: " + name.getText().toString() + ", NetType: " + checkedNetType.getText() + ", Freq: " + checkedFrequency.getText());

                                        newSync.setName(name.getText().toString());
                                        Log.d(TAG, "Radiobutton id : " + checkedFrequency.getId());

                                        switch (checkedNetType.getId()) {
                                            case R.id.radioButton_WiFi:
                                                newSync.setNetworkType(Sync.NET_TYPE.WIFI);
                                                break;
                                            case R.id.radioButton_WiFi_Data:
                                                newSync.setNetworkType(Sync.NET_TYPE.WIFIDATA);
                                                break;
                                        }

                                        switch (charging.getId()) {
                                            case R.id.radioButton_charging:
                                                newSync.setChargingOnly(true);
                                                break;
                                            case R.id.radioButton_battery:
                                                newSync.setChargingOnly(false);
                                                break;
                                        }

                                        switch (checkedFrequency.getId()) {
                                            case R.id.radioButton6h:
                                                newSync.setInterval(360); // Minutes
                                                break;
                                            case R.id.radioButton12h:
                                                newSync.setInterval(720);
                                                break;
                                            case R.id.radioButton24h:
                                                newSync.setInterval(1440);
                                                break;
                                            case R.id.radioButton48h:
                                                newSync.setInterval(2880);
                                                break;
                                            case R.id.radioButton7j:
                                                newSync.setInterval(10080);
                                                break;
                                            case R.id.radioButton15j:
                                                newSync.setInterval(21600);
                                                break;
                                            case R.id.radioButton30j:
                                                newSync.setInterval(43200);
                                                break;
                                        }

                                        newSync.setDateCreated(new Date());
                                        newSync.setActive(true);

                                        if(nameIsOk) {
                                            Log.d(TAG, "Registering new Sync : " + newSync.toString());
                                            DB.executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    newSync.getAccount().getSyncs().add(newSync);
                                                    mAdapter.notifyNewSync(newSync);
                                                    mEmptyView.setVisibility(View.GONE);
                                                    mRecyclerView.setVisibility(View.VISIBLE);
                                                    Bundle syncSettings = new Bundle();
                                                    syncSettings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true); //Force manual sync
                                                    syncSettings.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true); //Force sync to start instantly
                                                    ContentResolver.requestSync(mAccount, AUTHORITY, syncSettings);
                                                }
                                            });
                                            dialog.dismiss();
                                        }else{
                                            Toast.makeText(getContext(), R.string.invalid_sync_name, Toast.LENGTH_LONG).show();
                                        }

                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                }).build();
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onFolderSelection(FolderChooserDialog dialog, File folder) {
        Log.d(TAG, "Received folder "+folder.getName());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_DIRECTORY){
            if(resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                TextView folderNamePlaceHolder = (TextView) newSyncDialog.getCustomView().findViewById(R.id.folderNamePlaceHolder);
                Log.d(TAG, "onActivityResult selected folder : " + data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
                folderNamePlaceHolder.setText(data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
                newSyncDialog.show();
                newSync.setFolder(data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
                isReselectingFolder = false;
            }else if(resultCode == DirectoryChooserActivity.RESULT_CANCELED){
                Log.d(TAG, "onActivityResult: Canceled folder selection");
                if(isReselectingFolder) {
                    newSyncDialog.show();
                    isReselectingFolder = false;
                }
            }
        }
    }

    public static interface ClickListener{
        public void onClick(View view,int position);
        public void onLongClick(View view,int position);
    }

    /**
     * Extend RecyclerView Touch listener to recognize long click
     */
    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{
        private ClickListener clicklistener;
        private GestureDetector gestureDetector;

        public RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener){

            this.clicklistener=clicklistener;
            gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child=recycleView.findChildViewUnder(e.getX(),e.getY());
                    if(child!=null && clicklistener!=null){
                        clicklistener.onLongClick(child,recycleView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child=rv.findChildViewUnder(e.getX(),e.getY());
            if(child!=null && clicklistener!=null && gestureDetector.onTouchEvent(e)){
                clicklistener.onClick(child,rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        Context.ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            Log.i(TAG, "CreateSyncAccount: the default account has probably already been created");
        }
        return newAccount;
    }

}
