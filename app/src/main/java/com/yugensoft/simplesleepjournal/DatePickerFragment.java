package com.yugensoft.simplesleepjournal;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Created by yugensoft on 25/07/2015.
 */

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    public String TAG_DEFAULT_YEAR = "default_year";
    public String TAG_DEFAULT_MONTH = "default_month";
    public String TAG_DEFAULT_DAY = "default_day";
    public String TAG_ARE_DEFAULTS = "are_defaults";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use supplied arguments as the default date in the picker, or if not supplied, the current date
        int year = 0;
        int month = 0;
        int day = 0;

        Bundle args = getArguments();
        if (args != null && args.getBoolean(TAG_ARE_DEFAULTS)) {
            year = args.getInt(TAG_DEFAULT_YEAR);
            month = args.getInt(TAG_DEFAULT_MONTH) -1; // The Calender/DatePickerDialog starts months at 0
            day = args.getInt(TAG_DEFAULT_DAY);
        } else {
            // defaults  not supplied, use current date
            final Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);

    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Do something with the date chosen by the user
    }

}
