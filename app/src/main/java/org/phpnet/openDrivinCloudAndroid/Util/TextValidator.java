package org.phpnet.openDrivinCloudAndroid.Util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;


/**
 * Created by clement on 01/03/17.
 */

public abstract class TextValidator implements TextWatcher {
    private final TextView mTv;

    public TextValidator(TextView tv){
        this.mTv = tv;
    }

    public abstract void validate(TextView textView, String text);

    @Override
    public void afterTextChanged(Editable editable) {
        String text = mTv.getText().toString();
        validate(mTv, text);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }
}
