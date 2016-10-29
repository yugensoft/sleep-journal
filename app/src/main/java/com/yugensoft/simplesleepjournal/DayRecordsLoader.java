package com.yugensoft.simplesleepjournal;

import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import java.util.ArrayList;


public class DayRecordsLoader extends AsyncTaskLoader<ArrayList<TimeEntry>> {
    public DayRecordsLoader(Context context) {
        super(context);
    }

    @Override
    public ArrayList<TimeEntry> loadInBackground() {
        return new TimeEntryDbHandler(getContext()).getAllTimeEntries(true,false,false);
    }
}
