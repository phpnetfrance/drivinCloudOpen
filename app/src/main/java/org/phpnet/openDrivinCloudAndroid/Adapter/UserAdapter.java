package org.phpnet.openDrivinCloudAndroid.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.R;

import java.util.ArrayList;

/**
 * Created by clement on 19/07/16.
 */
public class UserAdapter extends ArrayAdapter<Settings.User> {

    Context context;
    ArrayList<Settings.User> values;

    public UserAdapter(Context context, ArrayList<Settings.User> items) {
        super(context, R.layout.list_item_user, items);
        this.values = items;
        this.context = context;
    }

    public void update(ArrayList<Settings.User> items){
        values.clear();
        values.addAll(items);
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout userView;
        Settings.User entry = getItem(position);
        userView = new LinearLayout(getContext());
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.list_item_user, parent, false);
        rowView.setTag(entry);

        //TextView host = (TextView) rowView.findViewById(R.id.host);
        TextView user = (TextView) rowView.findViewById(R.id.username);

        ImageView passwordSavedIndicator = (ImageView) rowView.findViewById(R.id.has_password);

        if(entry.isPasswordSaved()){
            passwordSavedIndicator.setVisibility(View.GONE);
        }

        //host.setText(entry.getHost());
        user.setText(entry.getUsername());

        return rowView;

    }
}
