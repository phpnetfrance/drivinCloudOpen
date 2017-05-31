package org.phpnet.openDrivinCloudAndroid.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import org.mozilla.universalchardet.UniversalDetector;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Common.Decode;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.IOException;
import java.io.InputStream;

public class EditActivity extends AppCompatActivity {

    //Permet de savoir si le document a été modifié ou pas.
    private boolean modif = false;

    private static Context context;
    private Toolbar toolbar;
    private static final String TAG = "EditActivity";

    public static Context getContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        context = this;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.fileeditor_title);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));


        final String URL = getIntent().getExtras().getString("URL");
        final EditText editText = (EditText) findViewById(R.id.editText);

        /* Bi directional scroll START */
        final HorizontalScrollView hScroll = (HorizontalScrollView) findViewById(R.id.scrollHorizontal);
        final ScrollView vScroll = (ScrollView) findViewById(R.id.scrollVertical);

        vScroll.setOnTouchListener(new View.OnTouchListener() { //inner scroll listener
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        hScroll.setOnTouchListener(new View.OnTouchListener() { //outer scroll listener
            private float mx, my, curX, curY;
            private boolean started = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                curX = event.getX();
                curY = event.getY();
                int dx = (int) (mx - curX);
                int dy = (int) (my - curY);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        if (started) {
                            vScroll.scrollBy(0, dy);
                            hScroll.scrollBy(dx, 0);
                        } else {
                            started = true;
                        }
                        mx = curX;
                        my = curY;
                        break;
                    case MotionEvent.ACTION_UP:
                        vScroll.scrollBy(0, dy);
                        hScroll.scrollBy(dx, 0);
                        started = false;
                        break;
                }
                return true;
            }
        });
        /* Bi directional scroll END */

        /*Chargement du texte dans l'éditeur*/
        Thread getText = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Use the chardet encoding detector
                    UniversalDetector encodingDetector = new UniversalDetector(null);
                    InputStream stream = CurrentUser.getInstance().wdr.getMethodData(URL);
                    int nread;
                    byte[] buffer = new byte[4096];
                    //Feed octet stream to the detector until he's done
                    while ((nread = stream.read(buffer)) > 0 && !encodingDetector.isDone()) {
                        encodingDetector.handleData(buffer, 0, nread);
                    }
                    encodingDetector.dataEnd();
                    String detectedEncoding = encodingDetector.getDetectedCharset();
                    Log.d(TAG, "convertStreamToString detected encoding : "+detectedEncoding);

                    stream = CurrentUser.getInstance().wdr.getMethodData(URL);
                    String data = Decode.getInstance().convertStreamToString(stream, detectedEncoding);
                    editText.setText(data);
                } catch (IOException e) {
                    Log.e(TAG, "EditFile: Impossible d'ouvrir le fichier ("+URL+")", e);
                    e.printStackTrace();
                }
            }
        });
        try {
            getText.start();
            getText.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                modif = true;
            }

            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

    }

    /*Création du menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /*
    * Sauvegarde du texte modifié en UTF-8
    * */
    private Runnable runSave = new Runnable() {
        @Override
        public void run() {
            try {
                String URL = getIntent().getExtras().getString("URL");
                EditText editText = (EditText) findViewById(R.id.editText);
                byte[] data = editText.getText().toString().getBytes("UTF-8");
                CurrentUser.getInstance().wdr.putMethod(URL, data);
                modif = false;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };


    /*
    * Sauvegarde le document modifié
    * */
    private void threadSave() {
        try {
            Thread threadSave = new Thread(runSave);
            threadSave.start();
            threadSave.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /*
    * Handler des options du menu
    * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                threadSave();
                return true;
            case R.id.quit:
                if (!modif) {
                    finish();
                    startActivity(new Intent(this, AcceuilActivity.class));
                } else {
                    confirmQuit();
                }
                return true;
            default:
                return false;
        }
    }


    /*Si l'user modifie le document sans sauvegarder,
     on demande confirmation avant de quitter*/
    private void confirmQuit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Voulez-vous sauvegarder les modifications ?");
        builder.setTitle("Sauvegarde");

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getContext().startActivity(new Intent(getContext(), AcceuilActivity.getContext().getClass()));
                EditActivity.this.finish();
            }
        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                threadSave();
                getContext().startActivity(new Intent(getContext(), AcceuilActivity.getContext().getClass()));
                EditActivity.this.finish();
            }
        });
        builder.show();
    }


    /*Handler du boutton retour*/
    @Override
    public void onBackPressed() {
        if (!modif) {
            finish();
            startActivity(new Intent(this, AcceuilActivity.class));
        } else {
            confirmQuit();
        }
    }

}
