package com.yugensoft.simplesleepjournal;


import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;


public class AboutFragment extends DialogFragment {

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        View tv = v.findViewById(R.id.about_text1);

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
