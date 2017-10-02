package org.phpnet.openDrivinCloudAndroid.Activities.Fragments;


import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.EditActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.ImageActivity;
import org.phpnet.openDrivinCloudAndroid.Adapter.FileAdapter;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickDownload;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickFile;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickLogout;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickLong;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickMove;
import org.phpnet.openDrivinCloudAndroid.Loader.RemoteFileLoader;
import org.phpnet.openDrivinCloudAndroid.Protocols.DrivinCloudDownload;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class Navigate extends Fragment implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, ChooseInternalExternalDialog.ChooseInternalExternalDialogListener, LoaderManager.LoaderCallbacks<List<MyFile>> {

    private static final String TAG = "Navigate";
    private static final int DIALOG_REQUEST_CODE = 65421868;
    public static final String LOADER_CONFIG_SORT = "sort";
    private static final int REQEST_PERMISSION_WRITE_EXT_STORAGE = 2719;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<MyFile> listFile;
    private SharedPreferences sharedPref;
    private FileAdapter adapter;
    private ListView listView;
    public static final String KEYSORT = "keySort";
    private ClickLong mActionModeCallback;
    private View viewMove;

    public Navigate() {}

    public FileAdapter getAdapter() {
        return adapter;
    }

    public ListView getListView() {
        return listView;
    }

    public ArrayList<MyFile> getListFile() {
        return listFile;
    }

    public void setListFile(ArrayList<MyFile> listFile) {
        this.listFile = listFile;
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return swipeRefreshLayout;
    }

    OnCallbackActivity mCallback;

    public void onRestart() {
        if (CurrentUser.getInstance().move) {
            viewMove.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public Loader<List<MyFile>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "updateListFile: putint "+args.getInt(LOADER_CONFIG_SORT));
        return new RemoteFileLoader(getContext(), args.getInt(LOADER_CONFIG_SORT, R.string.sortDate));
    }

    @Override
    public void onLoadFinished(Loader<List<MyFile>> loader, List<MyFile> data) {
        ArrayList<MyFile> files = (ArrayList<MyFile>) data;
        this.listFile = files;
        adapter.setFiles(new ArrayList<>(files));

        listView.setEnabled(true);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<List<MyFile>> loader) {
        this.listFile = new ArrayList<>();
        adapter.setFiles(new ArrayList<MyFile>());
    }

    public interface OnCallbackActivity {
        /**
         * The user just selected an item into the list
         * @param position
         * @return The actionmode callback
         */
        public ClickLong onItemLongClick(int position);

        /**
         * The list just got updated
         */
        public void onChangeDirectory();
    }

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof Activity){
            try {
                mCallback = (OnCallbackActivity) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString()
                        + " must implement OnCallbackActivity");
            }

        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View navigateFragment = inflater.inflate(R.layout.fragment_navigate, container, false);

        sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        swipeRefreshLayout = (SwipeRefreshLayout) navigateFragment.findViewById(R.id.swipe_refresh_layout);

        viewMove = navigateFragment.findViewById(R.id.move_bar);
        listView = (ListView) navigateFragment.findViewById(R.id.list);
        listView.setEmptyView(getActivity().findViewById(android.R.id.empty));

        //Adds an header view ("../") if activated in preferences
        if(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_nav_show_parent_dir", true)) {
            ViewGroup backItem = (ViewGroup) inflater.inflate(R.layout.back_item_layout, listView, false);
            listView.addHeaderView(backItem, null, true);
        }

        updateListFile();

        swipeRefreshLayout.setOnRefreshListener(getRefreshListener());


        listView.setOnItemLongClickListener(this);
        listView.setOnItemClickListener(this);

        return navigateFragment;
    }

    /*Traitement de l'actualisation*/
    public SwipeRefreshLayout.OnRefreshListener getRefreshListener() {
        SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateListFile();
                swipeRefreshLayout.setRefreshing(false);

            }
        };
        return refreshListener;
    }

    /*Mise à jour de la liste, et du tri*/
    public void updateListFile(){
        Log.d(TAG, "updateListFile: Called");
        listFile = new ArrayList<>();
        adapter = new FileAdapter(getContext(), listFile);
        listView.setAdapter(adapter);
        Bundle loaderConfig = new Bundle();
        loaderConfig.putInt(LOADER_CONFIG_SORT, sharedPref.getInt(KEYSORT, R.string.sortLetter));
        getActivity().getSupportLoaderManager().destroyLoader(1); //Workaround, onCreateLoader not called otherwize...
        if(CurrentUser.getInstance().username.equals("")){
            ClickLogout.getInstance().onClickLogout(true);
        }
        RemoteFileLoader loader = (RemoteFileLoader) getActivity().getSupportLoaderManager().initLoader(1, loaderConfig, this);
        loader.forceLoad();
        listView.setEnabled(false); //Prevent click while loading file list
        swipeRefreshLayout.setRefreshing(true); //Set refreshing animation
        //Will be reverted in the callback (onLoadFinished)


        mActionModeCallback = new ClickLong(this);
        mCallback.onChangeDirectory(); //Notify the parent activity
    }

    /*Mise à jour de la liste, et du tri*/

    /**
     * Deprecated
     * @param newListFile
     */
    public void updateListFile(ArrayList<MyFile> newListFile) {
        updateListFile();
    }

    /*Tri la liste selon les préferences de l'user*/
    public void setSortPreference() {
        int sort = sharedPref.getInt(KEYSORT, R.string.sortLetter);
        switch (sort) {
            case R.string.sortDate:
                Log.d(TAG, "setSortPreference: date");
                Collections.sort(listFile, MyFile.getDateComparator(true));
                break;
            case R.string.sortSize:
                Log.d(TAG, "setSortPreference: size");
                Collections.sort(listFile, MyFile.getSizeComparator(true));
                break;
            case R.string.sortLetter:
                Log.d(TAG, "setSortPreference: alphabetical");
                Collections.sort(listFile, MyFile.getNameComparator(true));
                break;
        }
    }

    /**/
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionModeCallback.getmActionMode() != null) {//On est déjà dans le callback
            return false;
        }else{
            //Prevent selection for header
            if(PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean("pref_nav_show_parent_dir", true)
                    && position == 0) {
                return false;
            }

            //Pour eviter plusieurs selections, on vide la liste des selections ...
            CurrentUser currentUser = CurrentUser.getInstance();
            if (currentUser.move) {
                currentUser.move = false;
                viewMove.setVisibility(View.GONE);
                currentUser.listSelectedFiles.clear();
            }

            adapter.selectedFiles.add((MyFile) view.getTag());
            //mActionModeCallback.setmActionMode(startSupportActionMode(mActionModeCallback));
            mCallback.onItemLongClick(position); //Notify the parent activity to start the actionmode
            return true;

        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MyFile item = (MyFile) parent.getItemAtPosition(position);
        onClickFile(item);
    }

    /*
    * Traitement du click sur un fichier selon son type
    * */
    public void onClickFile(MyFile item) {
        CurrentUser currentUser = CurrentUser.getInstance();
        if(item == null){
            if (currentUser.listPrecDir.size() > 0) { //Si il y a des dossiers à remonter
                currentUser.moveBack();
                updateListFile();
            }
        }else{
            Intent intent;
            switch (item.getTypeFile()) {
                case DIR:
                    currentUser.moveForward(item.getName());
                    updateListFile();
                    mCallback.onChangeDirectory(); //Notify the activity we just changed the cwd
                    if (currentUser.move) {
                        ClickMove clickMove = new ClickMove(this);
                        clickMove.onClickMove();
                    }
                    break;
                case TXT:
                    choseInternalExternal(item, true);
                    break;
                case IMG:
                    currentUser.indexCurrentImage = currentUser.listImage.indexOf(item);
                    choseInternalExternal(item, item.isPreviewable(getContext()));
                    break;
                default:
                    //new ClickFile(item.pathURL + item.name, item.name, item.getMimeType()).onClickFile();
                    boolean canWriteExtStorage = (ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED;
                    if(canWriteExtStorage) {
                        ClickDownload.getInstance().onClickDownload(item);
                    }else{
                        ActivityCompat.requestPermissions(this.getActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQEST_PERMISSION_WRITE_EXT_STORAGE);
                    }
            }
        }
    }

    private enum selectOption {INTERNAL, EXTERNAL};
    private selectOption tempSelectedOption = selectOption.EXTERNAL; //Default to external

    /**
     * Checks Internal? External? Then opens the file
     *
     * @param file
     * @param canOpenInDC
     */
    public void choseInternalExternal(final MyFile file, boolean canOpenInDC) {
        if(canOpenInDC) {
            String policy = Settings.getMimeTypeOpenPolicy(getContext(), file.getMimeType());
            if (policy == null) { //No policy for this mime type
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ChooseInternalExternalDialog dialogFragment = new ChooseInternalExternalDialog();
                Bundle args = new Bundle();
                args.putSerializable("file", file);
                dialogFragment.setArguments(args);
                dialogFragment.setTargetFragment(this, DIALOG_REQUEST_CODE);
                dialogFragment.show(getActivity().getSupportFragmentManager(), "ChooseInternalExternalDialog");
            } else if (policy == "internal") {
                doOpen(file, selectOption.INTERNAL);
            } else if (policy == "external") {
                doOpen(file, selectOption.EXTERNAL);
            }
        }else{
            MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                    .icon(AppCompatResources.getDrawable(getContext(), R.drawable.ic_error_outline_black_24dp))
                    .title(R.string.alert_big_file)
                    .content(R.string.alert_big_file_content)
                    .positiveText(R.string.alert_big_file_open)
                    .negativeText(R.string.alert_big_file_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            //new ClickFile(item.pathURL + item.name, item.name, item.getMimeType()).onClickFile();
                            boolean canWriteExtStorage = (ContextCompat.checkSelfPermission(Navigate.this.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED;
                            if(canWriteExtStorage) {
                                ClickDownload.getInstance().onClickDownload(file);
                            }else{
                                ActivityCompat.requestPermissions(Navigate.this.getActivity(),
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQEST_PERMISSION_WRITE_EXT_STORAGE);
                            }
                        }
                    }).show();
        }
    }

    @Override
    public void onClickRemember(ChooseInternalExternalDialog dialog) {
        doOpen(dialog.getFile(), tempSelectedOption);
        if (tempSelectedOption == tempSelectedOption.INTERNAL) {
            Settings.rememberInternalMimeType(getContext(), dialog.getFile().getMimeType());
        } else if (tempSelectedOption == tempSelectedOption.EXTERNAL) {
            Settings.rememberExternalMimeType(getContext(), dialog.getFile().getMimeType());
        }
    }

    @Override
    public void onClickOnce(ChooseInternalExternalDialog dialog) {
        doOpen(dialog.getFile(), tempSelectedOption);
    }

    /**
     * @param file
     * @param intExt
     */
    private void doOpen(final MyFile file, final selectOption intExt) {
        //new ClickFile(item.pathURL + item.name, item.name, item.getMimeType()).onClickFile();
        boolean canWriteExtStorage = (ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED;
        if(canWriteExtStorage) {
            final File target = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/"+file.name);
            DrivinCloudDownload dl = new DrivinCloudDownload(file.pathURL.buildUpon().appendPath(file.name).toString(), target);
            dl.addDownloadListener(new DrivinCloudDownload.DownloadListener() {
                @Override
                public void progress(long downloadedBytes, long totalBytes, String url) {
                    Log.d(TAG, "progress: "+downloadedBytes+"/"+totalBytes+" ("+((float)downloadedBytes/totalBytes)*100+"%"+")");
                }

                @Override
                public void cancel() {
                    Log.d(TAG, "cancel: Received cancel event");
                }

                @Override
                public void complete() {
                    Log.d(TAG, "complete: Received complete event, opening file");
                    switch (file.getTypeFile()) {
                        case IMG:
                            if (intExt == selectOption.INTERNAL) {
                                getContext().startActivity(new Intent(getContext(), ImageActivity.class));
                            } else if (intExt == selectOption.EXTERNAL) {
                                //new ClickFile(file.pathURL + "/" + file.name, file.name, file.getMimeType()).onClickFile();
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(target), file.getMimeType());

                                try {
                                    AcceuilActivity.getContext().startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    CurrentUser.getInstance().showToast(AcceuilActivity.getContext(), "Aucune application disponible pour visionner le fichier.");
                                }
                            }
                            break;
                        case TXT:
                            if (intExt == selectOption.INTERNAL) {
                                Intent intent = new Intent(getContext(), EditActivity.class);
                                intent.putExtra("URL", file.pathURL.buildUpon().appendPath(file.name).build().toString());
                                getActivity().finish();
                                getContext().startActivity(intent);
                            } else if (intExt == selectOption.EXTERNAL) {
                                new ClickFile(file.pathURL + "/" + file.name, file.name, file.getMimeType()).onClickFile();
                            }
                    }
                }


            });
            Log.d(TAG, "downloadFile: Starting download of file "+file.pathURL);
            dl.createNotification(getContext(), file.name, getContext().getString(R.string.downloading), getContext().getString(R.string.download_finished), getContext().getString(R.string.download_canceled), R.drawable.ic_drivincloud_notificon);
            dl.execute();
        }else{
            Toast.makeText(getContext(), "Vous devez autoriser l'application à enregistrer ce fichier", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQEST_PERMISSION_WRITE_EXT_STORAGE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (CurrentUser.getInstance().move) {
            ClickMove clickMove = new ClickMove(this);
            clickMove.onClickMove();
            viewMove.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOptionSelected(int index) {
        if (index == 0) {
            tempSelectedOption = selectOption.INTERNAL;
        } else {
            tempSelectedOption = selectOption.EXTERNAL;
        }
    }
}