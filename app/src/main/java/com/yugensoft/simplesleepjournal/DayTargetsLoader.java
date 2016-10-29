package com.yugensoft.simplesleepjournal;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import java.util.ArrayList;


public class DayTargetsLoader extends AsyncTaskLoader<ArrayList<TimeEntry>> {
    public DayTargetsLoader(Context context) {
        super(context);
    }

    @Override
    public ArrayList<TimeEntry> loadInBackground() {
        return new TimeEntryDbHandler(getContext()).getAllTimeEntries(false,false,true);
    }
}
