package com.yugensoft.simplesleepjournal;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import org.joda.time.DateTime;

public class RecordsActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // The loader's unique id. Loader ids are specific to the Activity
    private static final int LOADER_ID = 0;
    // The projection is the list of columns the Loader will return
    private static final String[] PROJECTION = new String []
    {
        TimeEntryDbHandler._ID,
        TimeEntryDbHandler.COLUMN_CENTER_OF_DAY,
        TimeEntryDbHandler.COLUMN_TIME,
        TimeEntryDbHandler.COLUMN_TYPE,
        TimeEntryDbHandler.COLUMN_DIRECTION
    };
    // The adapter that binds the data to the listview
    private TimeEntryListCursorAdapter mAdapter;

    private ListView listView;
    private Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize the CursorLoader
        getLoaderManager().initLoader(LOADER_ID, null, this);


        listView = (ListView) findViewById(R.id.records_list);

        String[] from =
        {
            TimeEntryDbHandler.COLUMN_CENTER_OF_DAY,
            TimeEntryDbHandler.COLUMN_DIRECTION,
            TimeEntryDbHandler.COLUMN_TIME,
        };
        int[] to =
        {
            R.id.TimeEntryListviewDay,
            R.id.TimeEntryListviewDirection,
            R.id.TimeEntryListviewTime,
        };

        // Initialize the adapter. A null cursor is used on initialization, it will be passed when
        // LoaderManager delivers the data on onLoadFinished
        mAdapter = new TimeEntryListCursorAdapter(this, R.layout.time_entry_records_listview_row, null, from, to, 0);
        listView.setAdapter(mAdapter);

        // Initialize the loader
        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, this);

        // Set onclick action of listview list items
        listView.setOnItemClickListener(ListItemClickListener);

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getApplication();
        mTracker = application.getDefaultTracker();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    AdapterView.OnItemClickListener ListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);

            //String msg = "ID: " + String.valueOf(id);
            //Toast.makeText(RecordsActivity.this, msg, Toast.LENGTH_SHORT).show();

            CustomEntryPickerFragment fragment = new CustomEntryPickerFragment();
            Bundle args = new Bundle();
            args.putLong(fragment.TAG_DEFAULT_DATE, cursor.getLong(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY)));
            args.putLong(fragment.TAG_DEFAULT_TIME, cursor.getLong(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_TIME)));
            args.putString(fragment.TAG_DEFAULT_DIRECTION, cursor.getString(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_DIRECTION)));
            args.putLong(fragment.TAG_ROW_ID, id);
            args.putBoolean(fragment.TAG_HAS_DELETE, true);
            args.putBoolean(fragment.TAG_IS_FIXED_DATE, true);
            args.putBoolean(fragment.TAG_IS_FIXED_DIRECTION, true);
            args.putString(fragment.TAG_TITLE, getString(R.string.change_sleep_record_dialog_title));
            fragment.setArguments(args);

            fragment.pickerCallback = CustomRecordPickerCallback;
            fragment.show(getSupportFragmentManager(), "customRecordPicker_with_defaults");

        }
    };

    CustomEntryPickerFragment.PickerCallback CustomRecordPickerCallback = new CustomEntryPickerFragment.PickerCallback() {
        @Override
        public void callbackSet(final int year, final int month, final int day, final int hour, final int minute, final String direction) {
            // Run in separate thread, as database work is done
            final Handler handler = new Handler();
            new Thread() {
                @Override
                public void run() {
                    // Put into the database
                    // Try update first. If the row doesn't exist, add it.
                    ContentValues mUpdateValues = new ContentValues();
                    DateTime time;
                    if (direction.equalsIgnoreCase(TimeEntry.Direction.BEDTIME.name()) && hour < 12) {
                        // Bedtime in morning of following day
                        time = new DateTime(year, month, day, hour, minute).plusDays(1);
                    } else {
                        time = new DateTime(year, month, day, hour, minute);
                    }
                    DateTime centerOfDay = new DateTime(year, month, day, 0, 0).plusHours(12);
                    mUpdateValues.put(TimeEntryDbHandler.COLUMN_TIME, time.getMillis());

                    String mSelectionClause = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + "=? AND " +
                            TimeEntryDbHandler.COLUMN_DIRECTION + "=? AND " +
                            TimeEntryDbHandler.COLUMN_TYPE + "=?";
                    String[] mSelectionArgs = {String.valueOf(centerOfDay.getMillis()), direction, TimeEntry.TimeEntryType.TIME_RECORD.name()};
                    int mRowsUpdated = 0;

                    mRowsUpdated = getContentResolver().update(
                            TimeEntryContentProvider.CONTENT_URI,   // the user dictionary content URI
                            mUpdateValues,                       // the columns to update
                            mSelectionClause,                    // the column to select on
                            mSelectionArgs                      // the value to compare to
                    );

                    final String toastText;
                    if (mRowsUpdated == 0) {
                        // doesn't exist, add it
                        Uri mNewUri;
                        ContentValues mNewValues = new ContentValues();
                        mNewValues.put(TimeEntryDbHandler.COLUMN_TIME, time.getMillis());
                        mNewValues.put(TimeEntryDbHandler.COLUMN_TYPE, TimeEntry.TimeEntryType.TIME_RECORD.name());
                        mNewValues.put(TimeEntryDbHandler.COLUMN_DIRECTION, direction);
                        mNewValues.put(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY, centerOfDay.getMillis());

                        mNewUri = getContentResolver().insert(
                                TimeEntryContentProvider.CONTENT_URI,   // the content URI
                                mNewValues                          // the values to insert
                        );

                        toastText = getString(R.string.custom_record_picker_notify_added);
                    } else {
                        toastText = getString(R.string.custom_record_picker_notify_changed);
                    }


                    // Display notification toast on completion
                    handler.post(new Runnable() {
                        public void run() {

                            Toast.makeText(
                                    RecordsActivity.this,
                                    toastText,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }
            }.start();

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("Add new record manually")
                    .build());
        }

        @Override
        public void callbackDelete(final int year, final int month, final int day, final int hour, final int minute, final String direction, final long rowId) {
            // Delete the time entry
            // Do in separate thread due to database operations
            final Handler handler = new Handler();
            new Thread() {
                @Override
                public void run() {
                    // Defines selection criteria for the rows you want to delete
                    DateTime centerOfDay = new DateTime(year, month, day, 0, 0).plusHours(12);
                    String mSelectionClause =
                            TimeEntryDbHandler._ID + "=?";
                    String[] mSelectionArgs = {String.valueOf(rowId)};

                    // Defines a variable to contain the number of rows deleted
                    int mRowsDeleted = 0;

                    // Deletes the words that match the selection criteria
                    mRowsDeleted = getContentResolver().delete(
                            TimeEntryContentProvider.CONTENT_URI,   // the user dictionary content URI
                            mSelectionClause,                    // the column to select on
                            mSelectionArgs                      // the value to compare to
                    );

                    assert (mRowsDeleted != 1) : "Only one row should have been deleted, total: " + String.valueOf(mRowsDeleted);

                    // Display notification toast on completion
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(
                                    RecordsActivity.this,
                                    getString(R.string.deleted_sleep_record),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                }
            }.start();
        }
    };

    // Method called by the Add New button
    public void addRecord(View v) {
        CustomEntryPickerFragment fragment = new CustomEntryPickerFragment();
        Bundle args = new Bundle();
        args.putString(fragment.TAG_TITLE, getString(R.string.add_sleep_record_dialog_title));
        fragment.setArguments(args);

        fragment.pickerCallback = CustomRecordPickerCallback;
        fragment.show(getSupportFragmentManager(), "customRecordPicker");
    }

    // Callback that is invoked when system has initialized Loader and is ready to start query.
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

        // Takes action based on the ID of the Loader that's being created

        switch (loaderID) {
            case LOADER_ID:
                // Returns a new CursorLoader
                String selection = TimeEntryDbHandler.COLUMN_TYPE + "=?";
                String selectionArgs[] = {TimeEntry.TimeEntryType.TIME_RECORD.name()};
                String sortOrder = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + " DESC, " +
                        TimeEntryDbHandler.COLUMN_TIME + " DESC";
                return new CursorLoader(
                        RecordsActivity.this,   // Parent activity context
                        TimeEntryContentProvider.CONTENT_URI,        // Table to query
                        PROJECTION,     // Projection to return
                        selection,            // selection clause
                        selectionArgs,        // selection arguments
                        sortOrder             // sort order
                );
            default:
                // An invalid id was passed in
                return null;
        }
    }

    @Override
    public void onLoadFinished (Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset (Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_records, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
