package com.yugensoft.simplesleepjournal;


import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


public class AboutFragment extends DialogFragment {
    private Tracker mTracker;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);
        TextView tv = (TextView)v.findViewById(R.id.about_text1);

        // Fill in text view
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            tv.setText(getString(R.string.app_name) + " v" + version + "\n" + getString(R.string.about));
        } catch (PackageManager.NameNotFoundException e) {

        }

        getDialog().setCanceledOnTouchOutside(true);
        //setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme);

        // Close dialog when clicked
        v.setOnClickListener(DialogCloseListener);
        tv.setOnClickListener(DialogCloseListener);

        return v;
    }

    View.OnClickListener DialogCloseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getDialog().dismiss();

        }
    };

}
