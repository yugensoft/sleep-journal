package com.yugensoft.simplesleepjournal.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 23/05/2015.
 */
public class TimeEntryDbHandler extends SQLiteOpenHelper implements BaseColumns {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "SleepJournal.db";
    public static final String TABLE_TIME_ENTRIES = "time_entries";

    public static final String COLUMN_CENTER_OF_DAY = "center_of_day";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_TYPE = "time_entry_type";
    public static final String COLUMN_DIRECTION = "wakeup_or_bedtime";

    public static final String[] FULL_PROJECTION = new String []
    {
            TimeEntryDbHandler._ID,
            TimeEntryDbHandler.COLUMN_CENTER_OF_DAY,
            TimeEntryDbHandler.COLUMN_TIME,
            TimeEntryDbHandler.COLUMN_TYPE,
            TimeEntryDbHandler.COLUMN_DIRECTION
    };

    public TimeEntryDbHandler(Context context) {
        super (context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TIME_ENTRIES_TABLE = "CREATE TABLE " + TABLE_TIME_ENTRIES + "("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_CENTER_OF_DAY + " INTEGER,"
                + COLUMN_TIME + " INTEGER,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_DIRECTION + " TEXT"
                + ")";
        db.execSQL(CREATE_TIME_ENTRIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle changes in the database between versions
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion)
        {
            switch (upgradeTo)
            {
                case 2:
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME_ENTRIES);
                    onCreate(db);
                    break;

            }
            upgradeTo++;
        }

    }

    // Method to add new time entry row to time entries table
    public long addTimeEntry(TimeEntry timeEntry) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CENTER_OF_DAY, timeEntry.get_centerOfDay());
        values.put(COLUMN_TIME, timeEntry.get_time());
        values.put(COLUMN_TYPE, timeEntry.get_timeEntryType().name());
        values.put(COLUMN_DIRECTION, timeEntry.get_direction().name());

        SQLiteDatabase db = this.getWritableDatabase();

        long newRowId;
        newRowId = db.insert(TABLE_TIME_ENTRIES, null, values);
        db.close();

        return newRowId;
    }

    // Method to return a list of time entries of specified time entry types
    public ArrayList<TimeEntry> getAllTimeEntries(boolean returnRecords, boolean returnTargets, boolean returnDefaults) {
        ArrayList<TimeEntry> allTimeEntries = new ArrayList<TimeEntry>();

        // If nothing to be returned, return empty list
        if (!returnRecords && !returnTargets && !returnDefaults) {
            return allTimeEntries;
        }

        // Construct Select query based on arguments
        String selectQuery = "SELECT * FROM " + TABLE_TIME_ENTRIES + " WHERE 0";
        if (returnRecords) {
            selectQuery += " OR " + COLUMN_TYPE + "='" + TimeEntry.TimeEntryType.TIME_RECORD.name() + "'";
        }
        if (returnTargets) {
            selectQuery += " OR " + COLUMN_TYPE + "='" + TimeEntry.TimeEntryType.TIME_TARGET.name() + "'";
        }
        if (returnDefaults) {
            selectQuery += " OR " + COLUMN_TYPE + " LIKE '%_DEFAULT'";
        }

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                TimeEntry timeEntry = new TimeEntry();
                // extract from row
                timeEntry.set_id(cursor.getInt(cursor.getColumnIndex(_ID)));
                timeEntry.set_centerOfDay(cursor.getLong(cursor.getColumnIndex(COLUMN_CENTER_OF_DAY)));
                timeEntry.set_direction(TimeEntry.Direction.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_DIRECTION))));
                timeEntry.set_timeEntryType(TimeEntry.TimeEntryType.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_TYPE))));
                timeEntry.set_time(cursor.getLong(cursor.getColumnIndex(COLUMN_TIME)));
                // add to the list
                allTimeEntries.add(timeEntry);
            } while (cursor.moveToNext());
        }

        // cleanup
        cursor.close();

        return allTimeEntries;
    }


    // Method to delete a time entry from the time entries table
    public boolean deleteTimeEntry(int id) {

        SQLiteDatabase db = this.getWritableDatabase();
        int affectedRows = db.delete(TABLE_TIME_ENTRIES, _ID + " = ?", new String[] { String.valueOf(id) });
        return (affectedRows > 0);
    }

}
