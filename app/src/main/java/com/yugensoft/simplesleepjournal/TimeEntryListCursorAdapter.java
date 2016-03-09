package com.yugensoft.simplesleepjournal;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

/**
 * Created on 17/06/2015.
 * Class to adapt TimeEntry list into Listview
 */
public class TimeEntryListCursorAdapter extends SimpleCursorAdapter implements Filterable {

    private Context context;
    private int layout;
    private final LayoutInflater inflater;

    public TimeEntryListCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.context = context;
        this.layout = layout;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private class ViewHolder {
        TextView txtDay;
        TextView txtType;
        TextView txtDirection;
        TextView txtTime;

    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        ViewHolder holder;

        holder = new ViewHolder();
        holder.txtDay = (TextView) view.findViewById(R.id.TimeEntryListviewDay);
        holder.txtType = (TextView) view.findViewById(R.id.TimeEntryListviewType);
        holder.txtDirection = (TextView) view.findViewById(R.id.TimeEntryListviewDirection);
        holder.txtTime = (TextView) view.findViewById(R.id.TimeEntryListviewTime);

        // Get day from centerOfDay
        if (holder.txtDay != null) {
            long centerOfDay = cursor.getLong(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY));
            String strCenterOfDay = new HumanReadableConverter(context).ConvertDate(centerOfDay);
            holder.txtDay.setText(strCenterOfDay);
        }
        // Get type of time entry
        if (holder.txtType != null) {
            holder.txtType.setText(cursor.getString(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_TYPE)));
        }
        if (holder.txtDirection != null) {
            holder.txtDirection.setText(cursor.getString(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_DIRECTION)));
        }
        // Get time
        if (holder.txtTime != null) {
            long time = cursor.getLong(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_TIME));
            long centerOfDay = cursor.getLong(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY));
            String direction = cursor.getString(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_DIRECTION));
            String strTime = new HumanReadableConverter(context).RelativeTime(centerOfDay, time, direction);
            holder.txtTime.setText(strTime);
        }

    }

}
