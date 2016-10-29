package com.yugensoft.simplesleepjournal;

import android.widget.TimePicker;
import android.widget.Toast;

/**
 */
public class CustomTimePickerFragment extends TimePickerFragment {

    public static abstract class PickerCallback {
        public abstract void callback(int hour, int minute);
    }

    public PickerCallback pickerCallback;

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Call the callbackSet function, to cause a notifySetDataChanged back in the activity
        if(pickerCallback !=null) {
            pickerCallback.callback(hourOfDay, minute);
        } else {
            throw new RuntimeException("Callback must be set");
        }

        Toast.makeText(
            getActivity(),
            "Time Updated to " + new HumanReadableConverter(getActivity()).ConvertTime(hourOfDay,minute),
            Toast.LENGTH_LONG
        ).show();
    }
}
