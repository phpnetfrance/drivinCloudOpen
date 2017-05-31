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

import org.phpnet.openDrivinCloudAndroid.Activities.FileChooserActivity;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by hazem on 28/06/15.
 */
public class FileChooserAdapter extends BaseAdapter {

    private List<FileChooser> listFile;
    private List<FileChooser> listFileSelected;

    public void addListFileSelected(FileChooser item) {
        if (!MyFile.interdit(item.fileName)) {
            if (!listFileSelected.contains(item))
                listFileSelected.add(item);
        } else {
            CurrentUser.getInstance().showToast(FileChooserActivity.getContext(), "Le fichier contient un caractère interdit.\n" +
                    "Les caractères spéciaux autorisés sont - _ @ ] [ & ' . espace");
        }
    }

    public boolean containsListFileSelected(Object item) {
        return listFileSelected.contains(item);
    }

    public boolean removeListFileSelected(Object item) {
        return listFileSelected.remove(item);
    }

    public void clearListFileSelected() {
        listFileSelected.clear();
    }

    public int sizeListFileSelected() {
        return listFileSelected.size();
    }

    public List<FileChooser> getListFileSelected() {
        return listFileSelected;
    }

    /*Un mécanisme pour gérer l'affichage graphique depuis un layout XML*/
    public LayoutInflater inflater;


    public FileChooserAdapter(Context context, List<FileChooser> listFile) {
        Context context1 = context;
        this.listFile = listFile;
        this.inflater = LayoutInflater.from(context);
        listFileSelected = new ArrayList<>();
    }

    @Override
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


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout layoutItem;
        FileChooser item = listFile.get(position);

        /*Réutilisation des layouts*/
        if (convertView == null) {
            /*Initialisation de notre item à partir du  layout XML "file_layout.xml"*/
            layoutItem = (LinearLayout) inflater.inflate(R.layout.file_chooser_layout, parent, false);
        } else {
            layoutItem = (LinearLayout) convertView;
        }

        /*Récupération des TextView et bouttons du  layout*/
        TextView fileName = (TextView) layoutItem.findViewById(R.id.name);
        TextView fileInfo = (TextView) layoutItem.findViewById(R.id.info);
        ImageView imageView = (ImageView) layoutItem.findViewById(R.id.image);
        String name = item.fileName;

        fileInfo.setText(item.info);

        if (item.isDirectory) {
            imageView.setImageResource(R.drawable.folder);
        } else {
            if (name.endsWith(".xls") || name.endsWith(".xlsx"))
                imageView.setImageResource(R.drawable.xls);
            else if (name.endsWith(".doc") || name.endsWith(".docx"))
                imageView.setImageResource(R.drawable.doc);
            else if (name.endsWith(".ppt") || name.endsWith(".pptx"))
                imageView.setImageResource(R.drawable.ppt);
            else if (name.endsWith(".pdf"))
                imageView.setImageResource(R.drawable.pdf);
            else if (name.endsWith(".apk"))
                imageView.setImageResource(R.drawable.android32);
            else if (name.endsWith(".txt"))
                imageView.setImageResource(R.drawable.txt32);
            else if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
                imageView.setImageResource(R.drawable.jpg32);
            else if (name.endsWith(".png"))
                imageView.setImageResource(R.drawable.png32);
            else if (name.endsWith(".zip"))
                imageView.setImageResource(R.drawable.zip32);
            else if (name.endsWith(".rtf"))
                imageView.setImageResource(R.drawable.rtf32);
            else if (name.endsWith(".gif"))
                imageView.setImageResource(R.drawable.gif32);
            else
                imageView.setImageResource(R.drawable.whitepage32);
        }
        fileName.setText(item.fileName);

        /*Changement du background dans le cas des sélections*/
        if (listFileSelected.contains(item)) {
            layoutItem.setBackgroundColor(Color.parseColor("#73c2fb"));
        } else {
            layoutItem.setBackground(null);
        }

        layoutItem.setTag(item);
        return layoutItem;
    }
}
