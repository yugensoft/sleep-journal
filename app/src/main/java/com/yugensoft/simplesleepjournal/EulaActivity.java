package com.yugensoft.simplesleepjournal;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class EulaActivity extends AppCompatActivity {

    private WebView webView;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eula);

        webView =(WebView)findViewById(R.id.webview);
        WebSettings.TextSize t = WebSettings.TextSize.NORMAL;
        webView.getSettings().setTextSize(t);
        webView.loadUrl("file:///android_asset/EULA.html");

        // Obtain the shared Tracker instance.
        mTracker = ((SimpleSleepJournalApplication)getApplication()).getDefaultTracker();

    }

    public void closeEula(View view) {
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
