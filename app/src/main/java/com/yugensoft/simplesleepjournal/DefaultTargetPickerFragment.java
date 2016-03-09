package com.yugensoft.simplesleepjournal;

import android.content.ContentValues;
import android.net.Uri;
import android.widget.TimePicker;
import android.widget.Toast;

import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import org.joda.time.Period;

/**
 * Created by yugensoft on 25/07/2015.
 */
public class DefaultTargetPickerFragment extends TimePickerFragment {

    public static abstract class PickerCallback {
        public abstract void callback(int hourOfDay, int minute);
    }

    public PickerCallback pickerCallback;

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String direction = getArguments().getString(TimeEntryDbHandler.COLUMN_DIRECTION);
        if (direction == null) {
            throw new IllegalArgumentException("No DIRECTION passed in argument bundle");
        }
        String type = getArguments().getString(TimeEntryDbHandler.COLUMN_TYPE);
        if (type == null) {
            throw new IllegalArgumentException("No TYPE passed in argument bundle");
        }

        // Process the time into a Duration
        if (direction.equalsIgnoreCase(TimeEntry.Direction.BEDTIME.name())) {
            // Times between 0 midnight and 12 noon are considered to be on the next day for Bed Times
            // So, hourOfDay is shifted ahead 24 hours
            if (hourOfDay < 12) {
                hourOfDay += 24;
            }
        }

        // Get the Duration (long milliseconds) from noon to the chosen time)
        org.joda.time.Duration duration = new Period(hourOfDay, minute, 0, 0).minusHours(12).toStandardDuration();

        long row_id = getArguments().getLong(TimeEntryDbHandler._ID);
        if (row_id == 0L) {
            throw new IllegalArgumentException("Row ID is zero, indicating it has probably not been passed in argument bundle");
        }
        if (row_id == -1) { // new database entry
            Uri mNewUri;
            ContentValues mNewValues = new ContentValues();
            mNewValues.put(TimeEntryDbHandler.COLUMN_TIME, duration.getMillis());
            mNewValues.put(TimeEntryDbHandler.COLUMN_TYPE, type);
            mNewValues.put(TimeEntryDbHandler.COLUMN_DIRECTION, direction);
            mNewValues.put(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY, 0); // Field not used for defaults

            mNewUri = getActivity().getContentResolver().insert(
                    TimeEntryContentProvider.CONTENT_URI,   // the content URI
                    mNewValues                          // the values to insert
            );

        } else {
            // Update existing database entry
            ContentValues mUpdateValues = new ContentValues();
            mUpdateValues.put(TimeEntryDbHandler.COLUMN_TIME, duration.getMillis());

            String mSelectionClause = TimeEntryDbHandler._ID + "=?";
            String[] mSelectionArgs = {String.valueOf(row_id)};
            int mRowsUpdated = 0;

            mRowsUpdated = getActivity().getContentResolver().update(
                    TimeEntryContentProvider.CONTENT_URI,   // the user dictionary content URI
                    mUpdateValues,                       // the columns to update
                    mSelectionClause,                    // the column to select on
                    mSelectionArgs                      // the value to compare to
            );

            // Should always exist
            assert mRowsUpdated > 0 : "No row was updated";

        }

        // Call the callbackSet function, to cause a notifySetDataChanged back in the activity
        if(pickerCallback !=null)
            pickerCallback.callback(hourOfDay, minute);

            Toast.makeText(
                getActivity(),
                "Time Updated to " + new HumanReadableConverter(getActivity()).ConvertTime(hourOfDay % 24, minute),
                Toast.LENGTH_LONG
        ).show();
    }
}
