package org.phpnet.openDrivinCloudAndroid.Activities;

import android.accounts.Account;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import org.phpnet.openDrivinCloudAndroid.Activities.Fragments.Sync.Syncs;
import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.R;

import de.psdev.licensesdialog.LicensesDialog;


public class PrefsActivity extends PreferenceActivity {
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    private static final String TAG = "PrefsActivity";

    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "onSharedPreferenceChanged: "+key);
                switch (key){
                    case(Syncs.KEYSYNCINTERVAL):
                        long interval;
                        try {
                            interval = Long.parseLong(prefs.getString(Syncs.KEYSYNCINTERVAL, String.valueOf(Syncs.DEFAULT_SYNC_INTERVAL))) * 60;
                        }catch(NumberFormatException e){
                            //TODO Default interval is set is case of parse problem
                            interval = 1800;
                        }
                        Log.d(TAG, "onSharedPreferenceChanged: Set sync frequency to "+interval+" sec");
                        Account mAccount = Syncs.CreateSyncAccount(PrefsActivity.this);
                        ContentResolver.addPeriodicSync(
                                mAccount,
                                Syncs.AUTHORITY,
                                Bundle.EMPTY,
                                interval);
                        break;
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);

        super.onCreate(savedInstanceState);
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager pm = getPreferenceManager();
        try {
            pm.findPreference("pref_about_app_version").setSummary(this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
            final Preference thirdParties = pm.findPreference("pref_about_third_parties");
            thirdParties.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new LicensesDialog.Builder(thirdParties.getContext())
                            .setNotices(R.raw.notices)
                            .setTitle(getString(R.string.pref_licences))
                            .build()
                            .show();
                    return true;
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference != null) {
            if (preference instanceof PreferenceScreen) {
                if (((PreferenceScreen) preference).getDialog() != null) {
                    ((PreferenceScreen) preference).getDialog().getWindow().getDecorView().setBackgroundDrawable(this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
                    setUpNestedScreen((PreferenceScreen) preference);
                }
            }
        }

        return false;
    }

    public void setUpNestedScreen(final PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        if (preferenceScreen.getKey().equals("pref_screen_open_file_associations")) {
            if (Settings.getInternalMimePolicies(getApplicationContext()).size() + Settings.getExternalMimePolicies(getApplicationContext()).size() > 0) {
                preferenceScreen.removeAll();
            }

            for (final String mimetype :
                    Settings.getMimeTypesList(getApplicationContext())) {
                ListPreference lp = new ListPreference(preferenceScreen.getContext());
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimetype);
                if(extension != null) {
                    lp.setTitle(extension+ " (" +mimetype+ ")");
                }else{
                    lp.setTitle(mimetype);
                }
                lp.setEntries(R.array.dialog_choose_internal_external_choices);
                lp.setEntryValues(new String[]{"internal", "external"});
                String policy = Settings.getMimeTypeOpenPolicy(preferenceScreen.getContext(), mimetype);
                lp.setDefaultValue(policy);
                if (policy == "internal") {
                    lp.setSummary(R.string.dialog_choose_internal);
                } else {
                    lp.setSummary(R.string.dialog_choose_external);
                }


                lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        if (o == "internal") {
                            Settings.rememberInternalMimeType(preferenceScreen.getContext(), mimetype);
                            preference.setSummary(R.string.dialog_choose_internal);
                        } else {
                            Settings.rememberExternalMimeType(preferenceScreen.getContext(), mimetype);
                            preference.setSummary(R.string.dialog_choose_external);
                        }
                        return true;
                    }
                });

                preferenceScreen.addPreference(lp);
            }


        }




    }


}
