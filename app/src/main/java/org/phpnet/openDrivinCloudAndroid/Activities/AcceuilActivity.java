package org.phpnet.openDrivinCloudAndroid.Activities;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadServiceBroadcastReceiver;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Navigate;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Sync.Syncs;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Uploads.Uploads;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Decode;
import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickCreateFile;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickCreateFolder;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickLogout;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickLong;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickMove;
import org.phpnet.openDrivinCloudAndroid.Model.Upload;
import org.phpnet.openDrivinCloudAndroid.Protocols.DrivinCloudDavUploadRequest;
import org.phpnet.openDrivinCloudAndroid.R;
import org.phpnet.openDrivinCloudAndroid.Util.ActionbarUtils;
import org.phpnet.openDrivinCloudAndroid.Util.Feedback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import io.realm.Realm;

public class AcceuilActivity extends AppCompatActivity
        implements Navigate.OnCallbackActivity, Uploads.OnUploadsFragmentInteractionListener, FolderChooserDialog.FolderCallback {
    private static final String TAG = AcceuilActivity.class.getSimpleName();

    private static final int FILE_UPLOAD_REQUEST_CODE = 42;
    public static final int PERMISSION_GRANT_REQUEST_CODE_FILECHOOSER = 6357;
    private static Context context;
    private static AcceuilActivity activity;
    private Menu menu;
    private SharedPreferences sharedPref;
    private File photoFile;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private String HELP_FRAGMENT_TAG = "NAVIGATEFRAGMENT";
    private String NAVIGATE_FRAGMENT_TAG = "HELPFRAGMENT";
    private String UPLOADS_FRAGMENT_TAG = "UPLOADSFRAGMENT";
    private String PREFERENCES_FRAGMENT_TAG = "PREFERENCESFRAGMENT";
    private String SYNCS_FRAGMENT_TAG = "SYNCSFRAGMENT";

    public static final String KEYSORT = "keySort";
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 53432;
    private Navigate navigateFragment;
    private Help helpFragment;
    private Uploads uploadsFragment;
    private Syncs syncsFragment;

    private Realm realm;

    private boolean isUserRegistered;
    //Listen for completed uploads and reload navigation view on upload completed
    //TODO better handling to prevent reloading the navigation view if the user is in another folder
    //TODO fix crash on file upload completion while in edit mode
    private UploadServiceBroadcastReceiver uploadServiceBroadcastReceiver = new UploadServiceBroadcastReceiver(){
        @Override
        public void onCompleted(Context context, final UploadInfo uploadInfo, final ServerResponse serverResponse) {
            super.onCompleted(context, uploadInfo, serverResponse);

            Log.d(TAG, "onCompleted: FROM "+uploadInfo.getUploadId()+" [CODE]headers: ["+serverResponse.getHttpCode()+"]"+serverResponse.getHeaders());

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Upload upload = realm.where(Upload.class).equalTo("uId", uploadInfo.getUploadId()).findFirst();
                    if(upload.startDate == null) upload.startDate = new Date(uploadInfo.getStartTime());
                    upload.endTime = new Date();
                    upload.averageSpeed = (long) uploadInfo.getUploadRate();
                    upload.sent = uploadInfo.getUploadedBytes();
                    upload.serverResponseCode = serverResponse.getHttpCode();
                    upload.canceled = false;
                    upload.elapsedTime = uploadInfo.getElapsedTimeString();
                    upload.progress = 100;
                }
            });
        }

        @Override
        public void onCancelled(Context context, final UploadInfo uploadInfo) {
            super.onCancelled(context, uploadInfo);
            Log.d(TAG, "execute: Received cancel event for upload "+uploadInfo.getUploadId());
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Upload upload = realm.where(Upload.class).equalTo("uId", uploadInfo.getUploadId()).findFirst();
                    upload.endTime = new Date();
                    upload.averageSpeed = (long) uploadInfo.getUploadRate();
                    upload.sent = uploadInfo.getUploadedBytes();
                    upload.canceled = true;
                    upload.elapsedTime = uploadInfo.getElapsedTimeString();
                }
            });
        }

        @Override
        public void onProgress(Context context, final UploadInfo uploadInfo) {
            super.onProgress(context, uploadInfo);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Upload upload = realm.where(Upload.class).equalTo("uId", uploadInfo.getUploadId()).findFirst();
                    if(upload.startDate == null) upload.startDate = new Date(uploadInfo.getStartTime());
                    upload.endTime = new Date();
                    upload.averageSpeed = (long) uploadInfo.getUploadRate();
                    upload.sent = uploadInfo.getUploadedBytes();
                    upload.progress = uploadInfo.getProgressPercent();
                    upload.elapsedTime = uploadInfo.getElapsedTimeString();
                }
            });
        }

        @Override
        public void onError(Context context, final UploadInfo uploadInfo, final Exception exception) {
            super.onError(context, uploadInfo, exception);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Upload upload = realm.where(Upload.class).equalTo("uId", uploadInfo.getUploadId()).findFirst();
                        if(upload.startDate == null) upload.startDate = new Date(uploadInfo.getStartTime());
                        upload.endTime = new Date();
                        upload.averageSpeed = (long) uploadInfo.getUploadRate();
                        upload.sent = uploadInfo.getUploadedBytes();
                        upload.error = exception.toString();
                        upload.elapsedTime = uploadInfo.getElapsedTimeString();
                    }
                });
        }
    };


    public static Context getContext() {
        return context;
    }

    public static AcceuilActivity getActivity() {
        return activity;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(CurrentUser.getInstance().serverURL == null){
            Log.d(TAG, "onCreate: Logout bc CurrentUser not instanciated");
            ClickLogout.getInstance().onClickLogout(true);
        }

        setContentView(R.layout.activity_acceuil);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        ActionBar ab = getSupportActionBar();

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);
        ab.setDisplayUseLogoEnabled(false);

        setTitle(CurrentUser.getInstance().getTitle());

        AcceuilActivity.context = this;
        AcceuilActivity.activity = this;

        //TODO
        // registerForContextMenu(listView);

        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        setupDrawerContent(nvDrawer);

        //Launch the drawer default fragment (Explore)
        showNavigateFragment();
        nvDrawer.getMenu().getItem(0).setChecked(true);

        realm = Realm.getDefaultInstance();
        try {
            isUserRegistered = Settings.getUser(context, CurrentUser.getInstance().serverURL, CurrentUser.getInstance().username) != null
                    && !Settings.getUser(context, CurrentUser.getInstance().serverURL, CurrentUser.getInstance().username).getPassword().equals("");
        }catch(NullPointerException e){
            Log.e(TAG, "onCreate: CurrentUser no more instanciated, logout.", e);
            ClickLogout.getInstance().onClickLogout(true);
        }
    }


    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Fragment fragment = null;
        Class fragmentClass = null;
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (menuItem.getItemId()) {
            case R.id.nav_explore:
                showNavigateFragment();
                closeDrawer();
                break;
            case R.id.nav_uploads:
                showUploadsFragment();
                closeDrawer();
                break;
            case R.id.nav_syncs:
                showSyncsFragment();
                closeDrawer();
                break;
            case R.id.nav_logout:
                logout();
                break;
            case R.id.nav_settings:
                Intent settingsIntent = new Intent(getApplicationContext(),
                        PrefsActivity.class);

                startActivity(settingsIntent);
                break;
            case R.id.nav_feedback:
                showFeedBackDialog();
                break;
            case R.id.nav_help:
                showHelp();
                break;
            default:
        }
    }

    private void logout() {
        ClickLogout.getInstance().onClickLogout();
    }

    // `onPostCreate` called when activity start-up is complete after `onStart()`
    // NOTE! Make sure to override the method with only a single `Bundle` argument
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    /*Coche la case correspondant au tri dans le menu*/
    public void checkSortPreference() {
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        int sort = sharedPref.getInt(KEYSORT, R.string.sortLetter);
        switch (sort) {
            case R.string.sortDate:
                menu.findItem(R.id.sort_date).setChecked(true);
                break;
            case R.string.sortSize:
                menu.findItem(R.id.sort_size).setChecked(true);
                break;
            case R.string.sortLetter:
                menu.findItem(R.id.sort_letter).setChecked(true);
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_acceuil, menu);
        this.menu = menu;
        checkSortPreference();
        return true;
    }

    /*
    * Handler des options du menu d'acceuil
    * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        CurrentUser currentUser = CurrentUser.getInstance();
        switch (item.getItemId()) {
            case android.R.id.home:
                //Checks if we are in the navigate fragment
                if(getSupportFragmentManager().findFragmentByTag(NAVIGATE_FRAGMENT_TAG) != null
                        && ((Navigate) getSupportFragmentManager().findFragmentByTag(NAVIGATE_FRAGMENT_TAG)).isVisible()) {

                    if (currentUser.listPrecDir.size() > 0) {
                        currentUser.moveBack();
                    } else {
                        openDrawer();
                        return true;
                    }
                    setTitle(currentUser.getTitle());
                    if (currentUser.move) {
                        ClickMove clickMove = new ClickMove(navigateFragment);
                        clickMove.onClickMove();
                    }
                    Navigate navigateFragment = (Navigate) getSupportFragmentManager().findFragmentByTag(NAVIGATE_FRAGMENT_TAG);
                    if (navigateFragment != null)
                        navigateFragment.updateListFile();
                }else{
                    openDrawer();
                }
                return true;
            case R.id.menu_upload:
                /*Intent getContentIntent = com.ipaulpro.afilechooser.utils.FileUtils.createGetContentIntent(); //TODO Check if it works with api 16
                Intent intent = Intent.createChooser(getContentIntent, getResources().getString(R.string.selectFileToUpload));
                startActivityForResult(intent, FILE_UPLOAD_REQUEST_CODE);
                */

                /*Intent i = new Intent(context, CustomNNFilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                startActivityForResult(i, FILE_UPLOAD_REQUEST_CODE);*/

                //Check permissions
                Log.d(TAG, "onOptionsItemSelected: Checking permissions for READ_EXTERNAL_STORAGE ("+PackageManager.PERMISSION_GRANTED+"|"+PackageManager.PERMISSION_DENIED+") : "+ ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE));
                if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Log.d(TAG, "onOptionsItemSelected: Showing information dialog");
                        new MaterialDialog.Builder(getContext())
                                .content(R.string.ask_read_ext_sto_perm)
                                .onAny(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        Log.d(TAG, "onOptionsItemSelected: Requestion permissions");
                                        ActivityCompat.requestPermissions(AcceuilActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_GRANT_REQUEST_CODE_FILECHOOSER);
                                    }
                                })
                                .positiveText(R.string.ok)
                                .build()
                                .show();
                    }else {
                        Log.d(TAG, "onOptionsItemSelected: Requestion permissions "+new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}.toString());
                        ActivityCompat.requestPermissions(AcceuilActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_GRANT_REQUEST_CODE_FILECHOOSER);
                    }
                }else {
                    Log.d(TAG, "onOptionsItemSelected: Permission already granted, preceeding to file chooser");
                    showFileChooser(FILE_UPLOAD_REQUEST_CODE);
                }
                return true;
            case R.id.sort_letter:
                sharedPref.edit().putInt(KEYSORT, R.string.sortLetter).apply();
                item.setChecked(true);
                updateListFile();
                return true;
            case R.id.sort_date:
                sharedPref.edit().putInt(KEYSORT, R.string.sortDate).apply();
                item.setChecked(true);
                updateListFile();
                return true;
            case R.id.sort_size:
                sharedPref.edit().putInt(KEYSORT, R.string.sortSize).apply();
                item.setChecked(true);
                updateListFile();
                return true;
            case R.id.create_dir:
                ClickCreateFolder.getInstance().onClickCreateFolder(Decode.getInstance().getListFile(currentUser.currentDirURL()));
                return true;
            case R.id.create_file:
                ClickCreateFile.getInstance().onClickCreateFile(Decode.getInstance().getListFile(currentUser.currentDirURL()));
                return true;
            case R.id.menu_camera:
                dispatchTakePictureIntent();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showFileChooser(int requestCode) {
        Intent chooseFile;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            chooseFile.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }else{
            chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        }
        chooseFile.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        chooseFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        chooseFile.setType("*/*");
        startActivityForResult(Intent.createChooser(chooseFile, getString(R.string.choose_file_to_upload)), requestCode);
    }


    /*Cree un fichier de nom unique, l'image prise sera stockée à l'intérieur*/
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir /* directory */
        );

        return image;
    }


    /*Appel l'activité de la caméra si possible*/
    private void dispatchTakePictureIntent() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                //File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                } else {
                    CurrentUser.getInstance().showToast(getContext(), getString(R.string.camera_unavailable));
                }
            }
        }


    }


    /*Prend le résultat de la caméra et l'upload au serveur*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); //Do not remove, important to catch results in underlaying fragments
        final CurrentUser currentUserInstance = CurrentUser.getInstance();
        if (currentUserInstance.wdr == null) {
            finish();
            startActivity(new Intent(this, LoginActivity.class));
            currentUserInstance.showToast(this, getString(R.string.session_expired));
        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Toast.makeText(AcceuilActivity.this, getString(R.string.fileAddedToUploadList, photoFile.getName()), Toast.LENGTH_LONG).show();
            try {
                Log.d(TAG, "Begining upload of file "+photoFile.getAbsolutePath()+" to "+ currentUserInstance.currentAbsoluteDirPath()+".");
                Logger.setLogLevel(Logger.LogLevel.DEBUG); //TODO Remove for release

                final String uploadId =
                        new DrivinCloudDavUploadRequest(getContext(), UUID.randomUUID(), currentUserInstance.serverURL, currentUserInstance.currentAbsoluteDirPath(), null)
                                //.setUsernameAndPassword(currentUserInstance.username, currentUserInstance.password)
                                .setFileToUpload(photoFile.getAbsolutePath(), photoFile.getName())
                                .startUpload();
                Log.d(TAG, "Launched upload of file "+photoFile.getAbsolutePath()+" to "+currentUserInstance.serverURL+". (UploadID : "+uploadId+")");


            } catch (FileNotFoundException e) {
                Toast.makeText(AcceuilActivity.this, getString(R.string.error_file_not_found, photoFile.getName()), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Unable to upload file "+photoFile.getName()+" ("+photoFile.getTotalSpace()+")", e);
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unable to upload file "+photoFile.getName()+" ("+photoFile.getTotalSpace()+")", e);
            } catch (Exception e){
                Log.e(TAG, "Unable to upload file "+photoFile.getName()+" ("+photoFile.getTotalSpace()+")", e);
            }
        } else if (requestCode == FILE_UPLOAD_REQUEST_CODE && resultCode == RESULT_OK) { //Received the result of the upload intent

            ArrayList<Uri> filesToUpload = new ArrayList<>();

            Log.d(TAG, "onActivityResult: "+data.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Multiple upload is api 19+ only
                ClipData filesClip = data.getClipData();
                if(filesClip != null) { //Case of multiple files
                    for (int i = 0; i < filesClip.getItemCount(); i++) {
                        filesToUpload.add(filesClip.getItemAt(i).getUri());
                    }
                }else{ //Case of a single file
                    filesToUpload.add(data.getData());
                }
            }else{
                filesToUpload.add(data.getData());
            }

            Log.d(TAG, "onActivityResult: Retrieved "+filesToUpload.size()+" files to upload.");
            Uri currentFileUri = filesToUpload.get(0);
            String fileName = "";
            try {
                Logger.setLogLevel(Logger.LogLevel.DEBUG);
                for (Uri fileUri :
                        filesToUpload) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        // Persist permission:
                        grantUriPermission(getPackageName(), fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Log.d(TAG, "onActivityResult: persist READURI permission for "+fileUri+" package: "+getPackageName());
                        final int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        try{
                            getContentResolver().takePersistableUriPermission(fileUri, flags);
                        }catch(SecurityException e){
                            Log.e(TAG, "onActivityResult: Erreur while taking permission", e);
                        }
                    }


                    currentFileUri = fileUri;
                    Cursor cursor = getActivity().getContentResolver().query(fileUri, null, null, null, null, null);
                    if(cursor != null && cursor.moveToFirst()){
                        fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        long size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                        Log.d(TAG, "onActivityResult: size"+ size);
                        Log.d(TAG, "Begining upload of file " + fileName + " to " + currentUserInstance.currentAbsoluteDirPath() + ".");
                        String uploadId =
                                new DrivinCloudDavUploadRequest(getContext(), UUID.randomUUID(), currentUserInstance.serverURL, currentUserInstance.currentAbsoluteDirPath(), null)
                                        //.setUsernameAndPassword(currentUserInstance.username, currentUserInstance.password)
                                        .setFileToUpload(fileUri, fileName)
                                        .startUpload();
                        Log.d(TAG, "Launched upload of file "+URLDecoder.decode(fileUri.toString(), "UTF-8")+" to "+ currentUserInstance.serverURL+currentUserInstance.currentAbsoluteDirPath()+". (UploadID : "+uploadId+")");

                    }
                }

            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unable to upload file "+currentFileUri.getLastPathSegment(), e);
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unable to upload file "+currentFileUri.getLastPathSegment(), e);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


            if(filesToUpload.size() == 1) {
                Toast.makeText(AcceuilActivity.this, getString(R.string.fileAddedToUploadList, fileName), Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(AcceuilActivity.this, getString(R.string.filesAddedToUploadList, String.valueOf(filesToUpload.size())), Toast.LENGTH_LONG).show();
            }
        } else {
        }
    }

    @Override
    public void onBackPressed() {
        CurrentUser currentUser = CurrentUser.getInstance();
        if (currentUser.listPrecDir.size() == 0) { //Si on est au bout de l'arborescence
            if(getSupportFragmentManager().getBackStackEntryCount() == 1){ //Si la backstack de la navigation est vide
                finish();
            }else {
                super.onBackPressed();
            }
        } else {
            currentUser.moveBack();
            updateListFile();
            onChangeDirectory();
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
    }



    public void updateListFile(){
        //Notify the fragment that the file list changed
        if(navigateFragment != null){
            navigateFragment.updateListFile();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Registering the uploadservice status broadcast listener.");
        uploadServiceBroadcastReceiver.register(getContext());
    }

    private void showFeedBackDialog() {

        MaterialDialog dialog = new MaterialDialog.Builder(this).title("Donnez votre avis sur drivinCloud")
                .autoDismiss(false)
                .cancelable(false)
                .customView(R.layout.dialog_feedback, false)
                .positiveText("Envoyer")
                .neutralText("Annuler")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        View view = dialog.getCustomView();

                        EditText mailView = (EditText) view.findViewById(R.id.feedbackMail);
                        EditText subjectView = (EditText) view.findViewById(R.id.feedbackSubject);
                        EditText messageBodyView = (EditText) view.findViewById(R.id.feedbackBody);

                        String mail = mailView.getText().toString();
                        String subject = subjectView.getText().toString();
                        String message = messageBodyView.getText().toString();

                        if(subject.length() > 5 && message.length() > 5){

                            if(Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {

                                dialog.dismiss();
                                Toast.makeText(getContext(), R.string.feedback_successfully_sent, Toast.LENGTH_LONG).show();
                                ErrorReporter acraReporter = ACRA.getErrorReporter();
                                acraReporter.setEnabled(true);
                                acraReporter.putCustomData("feedbackUserEmail", mail);
                                acraReporter.putCustomData("feedbackSubject", subject);
                                acraReporter.putCustomData("feedbackMessage", message);
                                acraReporter.putCustomData("currentDriveUser", CurrentUser.getInstance().username);
                                acraReporter.putCustomData("currentHostname", CurrentUser.getInstance().serverURL.toString());
                                acraReporter.putCustomData("currentDirectory", CurrentUser.getInstance().currentDirURL().toString());
                                acraReporter.handleException(new Feedback(), false);
                                acraReporter.clearCustomData();
                            }else{
                                Toast.makeText(getContext(), R.string.feedback_incomplete_form_mail, Toast.LENGTH_LONG).show();
                                mailView.setHighlightColor(Color.RED);
                            }
                        }else{
                            Toast.makeText(getContext(), R.string.feedback_incomplete_form, Toast.LENGTH_LONG).show();
                            subjectView.setHighlightColor(Color.RED);
                            messageBodyView.setHighlightColor(Color.RED);
                        }
                    }
                })
                .show();
    }

    private void showHelp() {
        Log.d("Drawer", "Show help fragment");
        FragmentManager fm = getActivity().getSupportFragmentManager();
        boolean fragmentPopped = !(helpFragment == null) && fm.popBackStackImmediate(helpFragment.getClass().getName(), 0);
        if (fragmentPopped) {
            //TODO Change title
        } else {
            Intent helpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://drivincloud.fr/documentation/android/v1/"));
            startActivity(helpIntent);
        }
    }

    /**
     * Initiates fragment if needed then shows it
     */
    public void showNavigateFragment() {
        Log.d("Drawer", "Show navigate fragment");

        //Reset the moveBack button
        updateActionBarHomeButton();

        //Reset the title moveBack to normal
        setTitle(CurrentUser.getInstance().getTitle());

        //Reset the menu
        if (menu != null) {
            onCreateOptionsMenu(menu);
        }

        FragmentManager fm = getActivity().getSupportFragmentManager();
        boolean fragmentPopped = !(navigateFragment == null) && fm.popBackStackImmediate(navigateFragment.getClass().getName(), 0);
        if (fragmentPopped) {
            //TODO Change title
        } else {
            FragmentTransaction fTransaction = fm.beginTransaction();
            navigateFragment = (Navigate) fm.findFragmentByTag(NAVIGATE_FRAGMENT_TAG);
            if (navigateFragment == null) {
                navigateFragment = new Navigate();
                fTransaction.replace(R.id.flContent, navigateFragment, NAVIGATE_FRAGMENT_TAG);
            } else {
                fTransaction.show(navigateFragment);
            }
            fTransaction.addToBackStack(navigateFragment.getClass().getName());
            fTransaction.commit();
        }

    }

    /**
     * Initiates fragment if needed then shows it
     */
    public void showUploadsFragment() {
        Log.d("Drawer", "Show uploads fragment");

        //Change title
        setTitle(R.string.uploads_fragment_title);

        //Hide the moveBack logo
        //AcceuilActivity.getActivity().getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_logo_nude);
        ActionbarUtils.setHomeAsUpIndicator(
                getActivity(),
                getActivity().getSupportActionBar(),
                false);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        boolean fragmentPopped = !(uploadsFragment == null) && fm.popBackStackImmediate(uploadsFragment.getClass().getName(), 0);
        if (fragmentPopped) {
            //TODO Change title
        } else {

            FragmentTransaction fTransaction = fm.beginTransaction();
            uploadsFragment = (Uploads) fm.findFragmentByTag(UPLOADS_FRAGMENT_TAG);
            if (uploadsFragment == null) {
                uploadsFragment = new Uploads();
                fTransaction.replace(R.id.flContent, uploadsFragment, UPLOADS_FRAGMENT_TAG);
            } else {
                fTransaction.show(uploadsFragment);
            }


            //Hide context menu items
            menu.clear();

            fTransaction.addToBackStack(uploadsFragment.getClass().getName());
            fTransaction.commit();


            //Hide context menu items
            //TODO Doesn't work when switching from sync fragments
            menu.clear();
        }
    }

    public void showSyncsFragment() {
        Log.d(TAG, "Show syncs fragment: Check that the account is registered (isUserRegistered: " + isUserRegistered + ")");
        final CurrentUser curr = CurrentUser.getInstance();
        if (isUserRegistered != true) {
            if (Settings.getUser(context, curr.serverURL, curr.username) == null || Settings.getUser(context, curr.serverURL, curr.username).getPassword().equals("")) {
                MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                        .icon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_error_outline_black_24dp, null))
                        .positiveColor(ResourcesCompat.getColor(getResources(), R.color.delete_message, null))
                        .title(context.getString(R.string.dialog_account_must_be_registered, curr.username))
                        .content(R.string.dialog_account_must_be_registered_content)
                        .positiveText(R.string.dialog_confirm_register)
                        .negativeText(R.string.dialog_cancel_register)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Log.d(TAG, "execute: Register account " + curr.username);

                                        Settings.addUser(context, curr.serverURL, curr.username, curr.password);
                                        isUserRegistered = true;

                                        //Change the title
                                        setTitle(R.string.syncs_fragment_title);

                                        //Hide context menu items
                                        menu.clear();

                                        FragmentManager fm = getActivity().getSupportFragmentManager();
                                        FragmentTransaction fTransaction = fm.beginTransaction();

                                        syncsFragment = (Syncs) fm.findFragmentByTag(SYNCS_FRAGMENT_TAG);

                                        if (syncsFragment == null) {
                                            syncsFragment = new Syncs();
                                            fTransaction.replace(R.id.flContent, syncsFragment, SYNCS_FRAGMENT_TAG);
                                        } else {
                                            fTransaction.show(syncsFragment);
                                        }
                                        fTransaction.addToBackStack(syncsFragment.getClass().getName());
                                        fTransaction.commit();
                                    }
                                });
                            }
                        }).onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                showNavigateFragment();
                                //TODO Better than this
                                nvDrawer.getMenu().getItem(0).setChecked(true);
                                nvDrawer.getMenu().getItem(2).setChecked(false);

                                isUserRegistered = false;
                            }
                        }).show();
            }
        } else {
            //Change the title
            setTitle(R.string.syncs_fragment_title);

            //Hide context menu items
            menu.clear();

            FragmentManager fm = getActivity().getSupportFragmentManager();

            boolean fragmentPopped = !(syncsFragment == null) && fm.popBackStackImmediate(syncsFragment.getClass().getName(), 0);
            if (fragmentPopped) {
                //TODO Change title
            } else {

                FragmentTransaction fTransaction = fm.beginTransaction();
                syncsFragment = (Syncs) fm.findFragmentByTag(SYNCS_FRAGMENT_TAG);

                if (syncsFragment == null) {
                    syncsFragment = new Syncs();
                    fTransaction.replace(R.id.flContent, syncsFragment, SYNCS_FRAGMENT_TAG);
                } else {
                    fTransaction.show(syncsFragment);
                }

                fTransaction.addToBackStack(syncsFragment.getClass().getName());
                fTransaction.commit();
            }
        }


    }

    @Override
    public ClickLong onItemLongClick(int position) {
        ClickLong callback = new ClickLong(navigateFragment);
        startSupportActionMode(callback);
        return callback;
    }

    /**
     * Called when the Navigate fragment changes cwd
     * Updates the title and moveBack button
     */
    @Override
    public void onChangeDirectory() {
        setTitle(CurrentUser.getInstance().getTitle());
        updateActionBarHomeButton();
    }


    /*
    * Removes the arrow from the actionbar if we are at the begining of the tree
    * */
    public void updateActionBarHomeButton(){
        CurrentUser curr = CurrentUser.getInstance();
        if (curr.listPrecDir.size() < 1) {
            Log.d("R.drawable.icon", String.valueOf(curr.listPrecDir.size()));
            //AcceuilActivity.getActivity().getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_logo_nude);
            ActionbarUtils.setHomeAsUpIndicator(
                    getActivity(),
                    getActivity().getSupportActionBar(),
                    false);

        } else {
            Log.d("R.drawable.back_logo", String.valueOf(curr.listPrecDir.size()));
            //AcceuilActivity.getActivity().getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_logo);
            ActionbarUtils.setHomeAsUpIndicator(
                    getActivity(),
                    getActivity().getSupportActionBar(),
                    true);
        }
    }

    public Navigate getNavigateFragment(){
        return navigateFragment;
    }

    public Syncs getSyncsFragment() {
        return syncsFragment;
    }

    @Override
    public void onFragmentInteraction(Uri uri) { //TODO Uploads fragment interaction

    }

    @Override
    protected void onDestroy() {
        //realm.close();
        uploadServiceBroadcastReceiver.unregister(getContext());
        super.onDestroy();
    }

    public void closeDrawer(){
        DrawerLayout mDrawerLayout;
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.closeDrawers();
    }

    public void openDrawer(){
        DrawerLayout mDrawerLayout;
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        getSyncsFragment().onFolderSelection(dialog, folder);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_GRANT_REQUEST_CODE_FILECHOOSER: {
                for(int i = 0; i<grantResults.length; i++){
                    Log.d(TAG, "onRequestPermissionsResult: grantResults["+permissions[i]+"]="+grantResults[i]);
                }
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //showFileChooser(FILE_UPLOAD_REQUEST_CODE);
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }else{
                    new MaterialDialog.Builder(getContext())
                            .content("Impossible d'accéder à vos fichiers.")
                            .build()
                            .show();
                }
            }

            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "La permission d'utiliser la camera est nécessaire", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
