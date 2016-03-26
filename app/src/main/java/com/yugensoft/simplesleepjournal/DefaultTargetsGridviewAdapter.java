package com.yugensoft.simplesleepjournal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import java.text.DateFormatSymbols;

/**
 *
 * Design notes:
 * With DEFAULT times, the Duration from the CenterOfDay and the wakeup/bedtime is stored in the time field.
 * Therefore, to construct the actual wakeup/bedtime for a given day, this value is simply added to the
 * CenterOfDay for that day.
 */

public class DefaultTargetsGridviewAdapter extends BaseAdapter {
    private final int ROW_COUNT = 8;
    private final int COL_COUNT = 3;

    private final String COLUMN_HEADER[] = {"Day", "Wakeup Time", "Bed Time"};

    private Context mContext;

    public DefaultTargetsGridviewAdapter(Context context) {
        this.mContext = context;
    }

    public int getCount() {
        return ROW_COUNT * COL_COUNT;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final String tagChangeable = mContext.getString(R.string.TAG_CHANGEABLE);

        final TextView tv;
        if (convertView == null) {
            tv = new TextView(mContext);

            tv.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.WRAP_CONTENT, GridView.LayoutParams.WRAP_CONTENT));
        }
        else {
            tv = (TextView) convertView;
        }

        // populate grid textviews
        final int row = (int)( (float) position / COL_COUNT);
        final int col = position % COL_COUNT;

        // Create a DateFormatSymbols instance
        DateFormatSymbols dfs = new DateFormatSymbols();

        // DateFormatSymbols instance has a method by name
        // getWeekdays() which returns back an array of
        // week days name
        final String[] arrayOfWeekDaysNames = dfs.getWeekdays();

        // Determine the data element corresponding to the TextView, and populate it accordingly
        if (row == 0) { // header column
            tv.setText(COLUMN_HEADER[col]);
        } else if (col == 0) { // day name column
            tv.setText(arrayOfWeekDaysNames[row].substring(0,3));
        } else {
            // Change color to indicate changeable value
            tv.setTextColor(mContext.getResources().getColor(R.color.changeable_textview_colour));
            // Query that element in a separate thread
            final SQLiteDatabase db = new TimeEntryDbHandler(mContext).getReadableDatabase();
            final Handler handler = new Handler();
            new Thread() {
                @Override
                public void run() {
                    final String output;
                    final String type;
                    final long id;
                    final String direction;
                    final boolean changeable = true;

                    if (col == 1) { // wakeup column
                        direction = TimeEntry.Direction.WAKE.name();
                    } else if (col == 2) { // bed time
                        direction = TimeEntry.Direction.BEDTIME.name();
                    } else {
                        // impossible column, something has gone wrong
                        throw new RuntimeException("Invalid column requested in defaults gridview");
                    }

                    String q = "SELECT " + TimeEntryDbHandler._ID + "," + TimeEntryDbHandler.COLUMN_TIME +
                            " FROM " + TimeEntryDbHandler.TABLE_TIME_ENTRIES +
                            " WHERE " + TimeEntryDbHandler.COLUMN_TYPE +
                            "= '" + arrayOfWeekDaysNames[row].toUpperCase() + "_DEFAULT'" +
                            " AND " + TimeEntryDbHandler.COLUMN_DIRECTION +
                            "= '" + direction + "'";
                    Cursor c = db.rawQuery(q, null);

                    final long time;
                    if(c != null && c.getCount() > 0) {
                        // get the time, convert to human-readable version
                        c.moveToNext();
                        time = c.getLong(c.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_TIME));
                        output = new HumanReadableConverter(mContext).DurationFromNoon(time);

                        type = arrayOfWeekDaysNames[row].toUpperCase() + "_DEFAULT";
                        id = c.getLong(c.getColumnIndexOrThrow(TimeEntryDbHandler._ID));
                    } else {
                        output = "(empty)";
                        type = arrayOfWeekDaysNames[row].toUpperCase() + "_DEFAULT";
                        time = -1;
                        id = -1;
                    }

                    // Free cursor
                    if (c != null) c.close();

                    handler.post(new Runnable() {
                        public void run() {
                            tv.setText(output);

                            Bundle bundle = new Bundle();
                            bundle.putLong(TimeEntryDbHandler._ID, id);
                            bundle.putString(TimeEntryDbHandler.COLUMN_DIRECTION, direction);
                            bundle.putString(TimeEntryDbHandler.COLUMN_TYPE, type);
                            bundle.putLong(TimeEntryDbHandler.COLUMN_TIME, time);
                            bundle.putBoolean(tagChangeable, changeable);
                            tv.setTag(R.id.VIEW_TAG_BUNDLE, bundle);

                        }
                    });
                }
            }.start();

        }

        return tv;
    }


}
