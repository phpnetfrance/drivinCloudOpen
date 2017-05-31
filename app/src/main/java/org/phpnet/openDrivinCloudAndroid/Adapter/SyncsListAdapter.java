package org.phpnet.openDrivinCloudAndroid.Adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.phpnet.openDrivinCloudAndroid.Model.Sync;
import org.phpnet.openDrivinCloudAndroid.R;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by clement on 18/08/16.
 */
public class SyncsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private static final String TAG = SyncsListAdapter.class.getSimpleName();
    public static final int HEADER = 0;
    public static final int BODY = 1;
    private Realm DB;

    List<Item> data;

    public SyncsListAdapter(RealmList<Sync> data, Realm DB){
        this.DB = DB;
        this.data = new ArrayList<>();
        if(data != null){
            for (Sync sync:
                    data) {
                this.data.add(new SyncsListAdapter.Item(SyncsListAdapter.HEADER, sync));
                this.data.add(new SyncsListAdapter.Item(SyncsListAdapter.BODY, sync));
            }
        }else{
            Log.d(TAG, "SyncsListAdapter: Data is null");
        }

    }



    public Sync getSyncAt(int position){
        return data.get(position).sync;
    }

    /**
     * Used to notify a new sync object so that it gets added to the list
     * Caution: Sync object is read only if passed from another thread.
     * @param sync
     */
    public void notifyNewSync(Sync sync){
        Log.d(TAG, "notifyNewSync: Received notification of a new sync, updating view");
        this.data.add(new SyncsListAdapter.Item(SyncsListAdapter.HEADER, sync));
        this.notifyItemInserted(this.data.size());
        this.data.add(new SyncsListAdapter.Item(SyncsListAdapter.BODY, sync));
        this.notifyItemInserted(this.data.size());
        this.notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        final Context context = parent.getContext();
        LayoutInflater layInf = LayoutInflater.from(context);



        switch (viewType){
            case HEADER:
                view = layInf.inflate(R.layout.fragment_syncs_item_header, parent, false);
                final SyncsHeaderViewHolder hvh = new SyncsHeaderViewHolder(view);
                hvh.expandIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { //Expand mecanism
                        data.get(hvh.getAdapterPosition()+1).opened = !data.get(hvh.getAdapterPosition()+1).opened;
                        notifyDataSetChanged();
                    }
                });

                return hvh;
            case BODY:
                view = layInf.inflate(R.layout.fragment_syncs_item_body, parent, false);
                SyncsBodyViewHolder bvh = new SyncsBodyViewHolder(view);

                return bvh;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final Item item = data.get(position);
        holder.itemView.setLongClickable(true);
        final Context context = holder.itemView.getContext();
        switch (item.type){
            case HEADER:
                SyncsHeaderViewHolder hvh = (SyncsHeaderViewHolder) holder;


                //Fill view
                hvh.label.setText(item.sync.getName());
                hvh.syncSwitch.setChecked(item.sync.isActive());
                if(item.sync.getLastSuccessPrintable() == null) {
                    hvh.lastSync.setText(R.string.never);
                }else{
                    hvh.lastSync.setText(item.sync.getLastSuccessPrintable());
                }

                //Change listener
                hvh.syncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                        DB.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                item.sync.setActive(isChecked);
                            }
                        });
                    }
                });
                break;
            case BODY:
                final SyncsBodyViewHolder bvh = (SyncsBodyViewHolder) holder;

                //Fill view
                bvh.sourceFolder.setText(item.sync.getFolder());

                switch(item.sync.getNetworkType()){
                    case WIFI:
                        bvh.networkType.check(R.id.radioButton_WiFi);
                        break;
                    case WIFIDATA:
                        bvh.networkType.check(R.id.radioButton_WiFi_Data);
                        break;
                }

                if(item.sync.canSyncOnBattery()){
                    bvh.charging.check(R.id.radioButton_battery);
                }else{
                    bvh.charging.check(R.id.radioButton_charging);
                }

                switch(item.sync.getInterval()) {
                    case 360:
                        bvh.frequency.check(R.id.radioButton6h);
                        break;
                    case 720:
                        bvh.frequency.check(R.id.radioButton12h);
                        break;
                    case 1440:
                        bvh.frequency.check(R.id.radioButton24h);
                        break;
                    case 2880:
                        bvh.frequency.check(R.id.radioButton48h);
                        break;
                    case 10080:
                        bvh.frequency.check(R.id.radioButton7j);
                        break;
                    case 21600:
                        bvh.frequency.check(R.id.radioButton15j);
                        break;
                    case 43200:
                        bvh.frequency.check(R.id.radioButton30j);
                        break;
                }

                if(bvh.getAdapterPosition() >= 0 && data.get(bvh.getAdapterPosition()).opened){
                    bvh.open();
                }else if(bvh.getAdapterPosition() >= 0){
                    bvh.close();
                }

                //Change listeners
                bvh.networkType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, final int checkedId) {
                        DB.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                switch (checkedId) {
                                    case R.id.radioButton_WiFi:
                                        item.sync.setNetworkType(Sync.NET_TYPE.WIFI);
                                        break;
                                    case R.id.radioButton_WiFi_Data:
                                        item.sync.setNetworkType(Sync.NET_TYPE.WIFIDATA);
                                        break;
                                }
                            }
                        });

                    }
                });

                bvh.charging.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, final int checkedId) {
                        DB.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                switch (checkedId){
                                    case R.id.radioButton_charging :
                                        item.sync.setChargingOnly(true); // Minutes
                                        break;
                                    case R.id.radioButton_battery :
                                        item.sync.setChargingOnly(false);
                                        break;
                                }
                            }
                        });

                    }
                });

                bvh.frequency.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, final int checkedId) {
                        DB.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                switch (checkedId){
                                    case R.id.radioButton6h :
                                        item.sync.setInterval(360); // Minutes
                                        break;
                                    case R.id.radioButton12h :
                                        item.sync.setInterval(720);
                                        break;
                                    case R.id.radioButton24h :
                                        item.sync.setInterval(1440); // Minutes
                                        break;
                                    case R.id.radioButton48h :
                                        item.sync.setInterval(2880);
                                        break;
                                    case R.id.radioButton7j:
                                        item.sync.setInterval(10080);
                                        break;
                                    case R.id.radioButton15j:
                                        item.sync.setInterval(21600);
                                        break;
                                    case R.id.radioButton30j :
                                        item.sync.setInterval(43200);
                                        break;
                                }
                            }
                        });

                    }
                });

                bvh.forceSyncBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        final Realm realm = Realm.getDefaultInstance();
                        new MaterialDialog.Builder(context)
                                .icon(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_error_outline_black_24dp, null))
                                .positiveColor(ResourcesCompat.getColor(context.getResources(), R.color.delete_message, null))
                                .title(R.string.dialog_2check_before_force_sync)
                                .content(R.string.force_sync_desc)
                                .positiveText(R.string.dialog_confirm_force_sync)
                                .negativeText(R.string.dialog_cancel_force_sync)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        Log.d(TAG, "onClick: Forcing " + item.sync.getName() + " sync on next verification");
                                        realm.executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                item.sync.forceOnNextSync();
                                            }
                                        });
                                    }
                                }).show();
                    }
                });

                bvh.resetSyncBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new MaterialDialog.Builder(context)
                                .icon(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_error_outline_black_24dp, null))
                                .positiveColor(ResourcesCompat.getColor(context.getResources(), R.color.delete_message, null))
                                .title(R.string.dialog_2check_before_reset_sync)
                                .content(R.string.reset_sync_desc)
                                .positiveText(R.string.dialog_confirm_reset_sync)
                                .negativeText(R.string.dialog_cancel_reset_sync)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        Log.d(TAG, "onClick: Reset "+item.sync.getName()+" sync on next verification");
                                        Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                item.sync.resetOnNextSync();
                                            }
                                        });
                                    }
                                }).show();
                    }
                });

        }
    }

    @Override
    public int getItemViewType(int postition){
        return data.get(postition).type;
    }

    @Override
    public int getItemCount(){ return data.size();}

    public static class Item {
        public Sync sync;
        public int type;
        public boolean opened = false;

        public Item(int type, Sync sync) {
            this.sync = sync;
            this.type = type;
        }
    }

    public static class SyncsHeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView label;
        public ImageButton expandIcon;
        public Switch syncSwitch;
        public TextView lastSync;
        public View body;

        public SyncsHeaderViewHolder(View itemView) {
            super(itemView);
            body = itemView;
            label = (TextView) itemView.findViewById(R.id.syncLabel);
            expandIcon = (ImageButton) itemView.findViewById(R.id.expand);
            syncSwitch = (Switch) itemView.findViewById(R.id.toggleSyncSwitchView);
            lastSync = (TextView) itemView.findViewById(R.id.lastSyncTextView);
        }


    }

    public static class SyncsBodyViewHolder extends RecyclerView.ViewHolder {
        public TextView sourceFolder;
        public RadioGroup networkType;
        public RadioGroup charging;
        public RadioGroup frequency;
        public Button forceSyncBtn;
        public Button resetSyncBtn;

        public int fullHeight;

        public SyncsBodyViewHolder(final View itemView) {
            super(itemView);
            sourceFolder = (TextView) itemView.findViewById(R.id.source_folder);
            networkType = (RadioGroup) itemView.findViewById(R.id.network_type);
            charging = (RadioGroup) itemView.findViewById(R.id.charging);
            frequency = (RadioGroup) itemView.findViewById(R.id.frequency);
            forceSyncBtn = (Button) itemView.findViewById(R.id.forceSyncButton);
            resetSyncBtn = (Button) itemView.findViewById(R.id.resetSyncButton);


            this.itemView.measure(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT); //Note: this is not really precise...
            fullHeight = this.itemView.getMeasuredHeight(); //We will need the height to animate open the view
        }

        public void open() {

            ValueAnimator va = ValueAnimator.ofInt(0, fullHeight);
            va.setDuration(200);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Integer value = (Integer) animation.getAnimatedValue();
                    itemView.getLayoutParams().height = value.intValue();
                    itemView.requestLayout();
                }
            });
            va.start();
        }

        public void close() {

            ValueAnimator va = ValueAnimator.ofInt(itemView.getHeight(), 0);
            va.setDuration(200);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Integer value = (Integer) animation.getAnimatedValue();
                    itemView.getLayoutParams().height = value.intValue();
                    itemView.requestLayout();
                }
            });
            va.start();
        }
    }

}
