package com.yugensoft.simplesleepjournal;

import android.widget.DatePicker;
import android.widget.Toast;

/**
 */
public class CustomDatePickerFragment extends DatePickerFragment {

    public static abstract class PickerCallback {
        public abstract void callback(int year, int month, int day);
    }

    public PickerCallback pickerCallback;

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        int monthOneBased = month + 1; // The Calender/DatePickerDialog starts months at 0, but the TimeDate classes start at 1

        // Call the callbackSet function, to cause a notifySetDataChanged back in the activity
        if(pickerCallback !=null) {
            pickerCallback.callback(year, monthOneBased, day);
        } else {
            throw new RuntimeException("Callback must be set");
        }

        Toast.makeText(
            getActivity(),
            "Date Updated to " + new HumanReadableConverter(getActivity()).ConvertDate(year, monthOneBased, day),
            Toast.LENGTH_LONG
        ).show();
    }
}
