package org.phpnet.openDrivinCloudAndroid.Adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.phpnet.openDrivinCloudAndroid.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by hazem on 28/06/15.
 */
public class FileAdapter extends BaseAdapter {
    private static final String TAG = FileAdapter.class.getSimpleName();
    private final Context context ;
    private List<MyFile> listFile;

    /*Un mécanisme pour gérer l'affichage graphique depuis un layout XML*/
    private LayoutInflater inflater;

    /*Liste des fichiers selectionnés*/
    public LinkedList<MyFile> selectedFiles;

    public List<MyFile> getSelectedFiles() {
        return selectedFiles;
    }


    public FileAdapter(Context context, List<MyFile> listFile) {
        this.context = context;
        this.listFile = listFile;
        this.inflater = LayoutInflater.from(context);
        this.selectedFiles = new LinkedList<>();

    }

    public int getCount() {
        return listFile.size();
    }

    @Override
    public Object getItem(int position) {
        return listFile.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout layoutItem;

        if (convertView == null) {
            layoutItem = (LinearLayout) inflater.inflate(R.layout.file_layout, parent, false);
        } else {
            layoutItem = (LinearLayout) convertView;
        }

        /*Récupération des TextView et Renseignement des valeurs*/
        TextView nameFileTV = (TextView) layoutItem.findViewById(R.id.nameFileTV);
        TextView infoFileTV = (TextView) layoutItem.findViewById(R.id.infoFileTV);
        ImageView imageView = (ImageView) layoutItem.findViewById(R.id.iconIV);

        /*Eciture des données*/
        MyFile item = listFile.get(position);

        // Delegated to the view
        //int M = CurrentUser.getInstance().getMaxLetter();
        //String shortName = (item.name.length() <= M) ? item.name : (item.name.substring(0, M) + "...");
        //nameFileTV.setText(shortName);

        nameFileTV.setText(item.name);
        infoFileTV.setText(item.date + (item.isDir() ? "" : ", " + item.size));

        if (listFile.get(position).isDir()) {
            imageView.setImageResource(R.drawable.dc_folder);
        } else {
            imageView.setImageResource(item.getIconResId());
        }


        /*Changement du background dans le cas des sélections*/
        if (selectedFiles.contains(item)) {
            layoutItem.setBackgroundColor(Color.parseColor("#73c2fb"));
        } else {
            layoutItem.setBackground(null);
        }

        layoutItem.setTag(item);
        return layoutItem;
    }

    public void setFiles(ArrayList<MyFile> files){
        this.listFile = files;
        notifyDataSetChanged();
    }
}
