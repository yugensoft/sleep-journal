package com.yugensoft.simplesleepjournal;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.Calendar;

/**
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    public String TAG_DEFAULT_HOUR = "default_hour";
    public String TAG_DEFAULT_MINUTE = "default_minute";
    public String TAG_ARE_DEFAULTS = "are_defaults";

    private Tracker mTracker;

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
        int hour = -1;
        int minute = -1;

        // If defaults are supplied, use them, else use current time
        Bundle args = getArguments();
        if (args != null && args.getBoolean(TAG_ARE_DEFAULTS)) {
            hour = args.getInt(TAG_DEFAULT_HOUR);
            minute = args.getInt(TAG_DEFAULT_MINUTE);
        } else  {
            final Calendar c = Calendar.getInstance();
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Do something with the time chosen by the user

    }
}
