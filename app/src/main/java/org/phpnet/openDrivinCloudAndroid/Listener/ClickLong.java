package org.phpnet.openDrivinCloudAndroid.Listener;

import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Navigate;
import org.phpnet.openDrivinCloudAndroid.Adapter.FileAdapter;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.R;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by germaine on 17/07/15.
 */
public class ClickLong implements ActionMode.Callback {
    private static final String TAG = "ClickLong";

    private List<MyFile> listFile;
    private ListView listView;
    private FileAdapter adapter;
    private Object mActionMode = null;
    private Navigate navigate;

    public Object getmActionMode() {
        return mActionMode;
    }

    public void setmActionMode(Object mActionMode) {
        this.mActionMode = mActionMode;
    }

    public ClickLong(Navigate activity) {
        this.listFile = activity.getListFile();
        this.adapter = activity.getAdapter();
        this.listView = activity.getListView();
        this.navigate = activity;
    }


    private String getS(ActionMode mode) {
        String s;
        if (adapter.selectedFiles.size() > 1) {
            s = "s";
            mode.getMenu().findItem(R.id.rename_context_menu).setVisible(false);
        } else {
            mode.getMenu().findItem(R.id.rename_context_menu).setVisible(true);
            s = "";
        }
        return s;
    }

    private AdapterView.OnItemClickListener simpleClick(final ActionMode mode) {
        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // If its an header view do nothing
                if(PreferenceManager.getDefaultSharedPreferences(view.getContext())
                        .getBoolean("pref_nav_show_parent_dir", true)
                        && position == 0) {
                   return;
                }
                int firstPosition = listView.getFirstVisiblePosition();
                int wantedChild = position - firstPosition;

                View item = listView.getChildAt(wantedChild);

                if (item.getTag() != null && adapter.selectedFiles.contains(item.getTag())) {
                    adapter.selectedFiles.remove(item.getTag());
                } else {
                    adapter.selectedFiles.add((MyFile) item.getTag());
                }

                if (adapter.selectedFiles.size() == 0)
                    mode.finish();

                mode.setTitle(adapter.selectedFiles.size() + " élement" + getS(mode));
                adapter.notifyDataSetChanged();

            }
        };
        return clickListener;
    }

    private AdapterView.OnItemLongClickListener longClick(final ActionMode mode) {
        AdapterView.OnItemLongClickListener clickListener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int firstPosition = listView.getFirstVisiblePosition();
                int wantedChild = position - firstPosition;

                View item = listView.getChildAt(wantedChild);

                if (item.getTag() != null && adapter.selectedFiles.contains(item.getTag())) {
                    adapter.selectedFiles.remove(item.getTag());
                } else {
                    adapter.selectedFiles.add((MyFile) item.getTag());
                }

                if (adapter.selectedFiles.size() == 0)
                    mode.finish();

                mode.setTitle(adapter.selectedFiles.size() + " élement" + getS(mode));
                adapter.notifyDataSetChanged();

                return true;
            }
        };
        return clickListener;
    }


    private SwipeRefreshLayout.OnRefreshListener getRefreshListener() {
        SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                navigate.getSwipeRefreshLayout().setRefreshing(false);
            }
        };
        return refreshListener;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        listView.setOnItemClickListener(simpleClick(mode));
        listView.setOnItemLongClickListener(longClick(mode));
        navigate.getSwipeRefreshLayout().setOnRefreshListener(getRefreshListener());

        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.context_menu, menu);

        adapter.notifyDataSetChanged();

        mode.setTitle(adapter.selectedFiles.size() + " élement" + getS(mode));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_context_menu:
                ClickDelete.getInstance().onClickDelete(adapter.getSelectedFiles(), navigate, mode);
                return true;
            case R.id.download_context_menu:
                List<MyFile> listCopy = new LinkedList<>();
                listCopy.addAll(adapter.selectedFiles);
                ClickDownload.getInstance().onClickDownload(listCopy);
                mode.finish();
                return true;
            case R.id.rename_context_menu:
                ClickRename.getInstance().onClickRename(adapter.getSelectedFiles().get(0), navigate, mode);
                return true;
            case R.id.move_context_menu:
                CurrentUser currentUser = CurrentUser.getInstance();
                currentUser.listSelectedFiles.clear();
                currentUser.listSelectedFiles.addAll(adapter.selectedFiles);
                currentUser.setCurrentDirMoveURL(currentUser.currentDirURL().toString());
                currentUser.move = true;
                ClickMove clickMove = new ClickMove(navigate);
                clickMove.onClickMove();
                mode.finish();
                return true;
            case R.id.selectAll_context_menu:
                for (MyFile file : listFile) {
                    if (!adapter.selectedFiles.contains(file))
                        adapter.selectedFiles.add(file);
                }
                //nbSelectedFiles = listFile.size();
                mode.setTitle(adapter.selectedFiles.size() + " élement" + getS(mode));
                adapter.notifyDataSetChanged();
                return true;
            default:
                return false;
        }

    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        listView.setOnItemClickListener(AcceuilActivity.getActivity().getNavigateFragment());
        listView.setOnItemLongClickListener(AcceuilActivity.getActivity().getNavigateFragment());
        navigate.getSwipeRefreshLayout().setOnRefreshListener(navigate.getRefreshListener());
        adapter.selectedFiles.clear();
        adapter.notifyDataSetChanged();
    }
}
