package com.yugensoft.simplesleepjournal;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.webkit.WebView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class HowToActivity extends ActionBarActivity {
    private Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView view = (WebView) findViewById(R.id.howto_webview);
        view.setVerticalScrollBarEnabled(true);
        view.setHorizontalScrollBarEnabled(false);
        view.loadUrl("file:///android_asset/howto.html");
        //setContentView(view);

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

}
