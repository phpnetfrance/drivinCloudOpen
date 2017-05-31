package org.phpnet.openDrivinCloudAndroid.Activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.phpnet.openDrivinCloudAndroid.Adapter.FileChooser;
import org.phpnet.openDrivinCloudAndroid.Adapter.FileChooserAdapter;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickFileChooser;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.File;
import java.util.ArrayList;


public class FileChooserActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, ActionMode.Callback, AdapterView.OnItemLongClickListener {

    private static Context myContext;
    private static FileChooserActivity activity;
    private Object mActionMode = null;
    private FileChooserAdapter adapter;
    private ListView listView;
    private Toolbar toolbar;

    public static Context getContext() {
        return myContext;
    }

    public static FileChooserActivity getActivity() {
        return activity;
    }

    private static final File EXT_STORAGE = Environment.getExternalStorageDirectory();

    public static final String ROOT = "/storage/";
    public static String currentDir = ROOT;

    private ArrayList<FileChooser> listFile = new ArrayList<>();


    private String getPrecDir() {
        String curr = FileChooserActivity.currentDir;
        if (curr.equals(ROOT)) {
            return ROOT;
        } else {
            curr = curr.substring(0, curr.length() - 1);
            int i = curr.lastIndexOf("/");
            if (i < 0) {
                return ROOT;
            } else {
                return curr.substring(0, i + 1);
            }
        }
    }

    private String getTitleActivity() {
        if (currentDir.equals(ROOT)) {
            return "";
        } else {
            String curr = FileChooserActivity.currentDir;
            curr = curr.substring(0, curr.length() - 1);
            int i = curr.lastIndexOf("/");
            return curr.substring(i + 1, curr.length());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                currentDir = getPrecDir();
                updateListFile();
                return true;
            default:
                return false;
        }
    }

    public void updateListFile() {
        listFile.clear();
        setTitle(getTitleActivity());
        File[] listFiles = new File(currentDir).listFiles();
        if (listFiles.length == 0) {
            findViewById(R.id.empty_folder).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.empty_folder).setVisibility(View.GONE);
        }

        for (File file : listFiles) {
            if (file.canRead() && !file.isHidden()) {
                listFile.add(new FileChooser(file));
            }
        }

        adapter = new FileChooserAdapter(this, listFile);
        listView = (ListView) findViewById(R.id.list_item);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_chooser_file);
        myContext = this;
        activity = this;

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        updateListFile();
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode != null) {
            return false;
        } else {
            FileChooser item = (FileChooser) parent.getItemAtPosition(position);
            adapter.addListFileSelected(item);
            mActionMode = startActionMode(this);
            return true;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileChooser item = (FileChooser) parent.getItemAtPosition(position);
        if (item.isDirectory) {
            currentDir += item.fileName + File.separator;
            updateListFile();
        } else {
            ClickFileChooser c = new ClickFileChooser();
            c.onClickFileChooser(item);
        }
    }


    private AdapterView.OnItemClickListener simpleClick(final ActionMode mode) {
        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int firstPosition = listView.getFirstVisiblePosition() - listView.getHeaderViewsCount();
                int wantedChild = position - firstPosition;

                FileChooser item = (FileChooser) listView.getChildAt(wantedChild).getTag();
                if (!adapter.containsListFileSelected(item)) {
                    if (!MyFile.interdit(item.fileName))
                        adapter.addListFileSelected(item);
                    else
                        CurrentUser.getInstance().showToast(FileChooserActivity.getContext(), "Le fichier contient un caractère interdit.\n" +
                                "Les caractères spéciaux autorisés sont - _ @ ] [ & ' . espace");
                } else {
                    adapter.removeListFileSelected(item);
                }

                if (adapter.sizeListFileSelected() == 0)
                    mode.finish();

                mode.setTitle(adapter.sizeListFileSelected() + " élement" + ((adapter.sizeListFileSelected() > 1) ? "s" : ""));
                adapter.notifyDataSetChanged();

            }
        };
        return clickListener;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        currentDir = ROOT;
        //finish();
        //startActivity(new Intent(this,AcceuilActivity.getContext().getClass()));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        listView.setOnItemClickListener(simpleClick(mode));
        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.menu_file_chooser, menu);
        adapter.notifyDataSetChanged();
        mode.setTitle(adapter.sizeListFileSelected() + " élement" + ((adapter.sizeListFileSelected() > 1) ? "s" : ""));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upload:
                //startActivity(new Intent(this,AcceuilActivity.getContext().getClass()));
                new ClickFileChooser().onClickFileChooser(adapter.getListFileSelected());
                mode.finish();
                return true;
            case R.id.select_all:
                for (FileChooser fileChooser : listFile) {
                    adapter.addListFileSelected(fileChooser);
                }
                mode.setTitle(adapter.sizeListFileSelected() + " élement" + ((adapter.sizeListFileSelected() > 1) ? "s" : ""));
                adapter.notifyDataSetChanged();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.clearListFileSelected();
        listView.setOnItemClickListener(this);
        adapter.notifyDataSetChanged();
        mActionMode = null;
    }
}
