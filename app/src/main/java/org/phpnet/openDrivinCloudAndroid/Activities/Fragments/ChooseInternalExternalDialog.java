package org.phpnet.openDrivinCloudAndroid.Activities.Fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.R;

public class ChooseInternalExternalDialog extends DialogFragment {
    private MyFile file;
    private ChooseInternalExternalDialogListener callback;

    public MyFile getFile(){
        return file;
    }

    public interface ChooseInternalExternalDialogListener {
        public void onOptionSelected(int index);
        public void onClickRemember(ChooseInternalExternalDialog dial);
        public void onClickOnce(ChooseInternalExternalDialog dial);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            callback = (ChooseInternalExternalDialogListener) getTargetFragment();
        } catch (Exception e) {
            throw new ClassCastException("Calling Fragment must implement ChooseInternalExternalDialogListener");
        }
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        Bundle args = this.getArguments();
        file = (MyFile) args.getSerializable("file");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_choose_internal_external);
        builder.setSingleChoiceItems(R.array.dialog_choose_internal_external_choices, 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callback.onOptionSelected(i);
                    }
                })
                .setNegativeButton(R.string.dialog_choose_internal_external_once, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callback.onClickOnce(ChooseInternalExternalDialog.this);
                    }
                }).setPositiveButton(R.string.dialog_choose_internal_external_remember, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callback.onClickRemember(ChooseInternalExternalDialog.this);
                    }
                });
        return builder.create();
    }
}