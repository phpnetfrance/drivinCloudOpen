package org.phpnet.openDrivinCloudAndroid.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.phpnet.openDrivinCloudAndroid.R;

/**
 * Created by clement on 03/02/17.
 */

public class Help extends AppCompatActivity {
    private WebView mWebView;
    private int back_count;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.help_webview);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl("file:///android_asset/doc/index.html");
        mWebView.setWebViewClient(new WebViewClient());
        back_count = 0;
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (back_count < 2 && mWebView.canGoBack()) {
                        mWebView.goBack();
                        back_count++;
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
}
