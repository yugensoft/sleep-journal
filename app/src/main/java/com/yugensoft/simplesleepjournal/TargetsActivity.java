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
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;


public class TargetsActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_CODE_TIME_PICKER = 0;
    private static final String CUSTOM_TARGET_PICKER_TAG = "customTargetPicker";

    private Tracker mTracker;

    // The loader's unique id. Loader ids are specific to the Activity
    private static final int LOADER_ID = 1;
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

    private ListView lvTargets;
    private GridView gridview;

    // Previous choice variables
    private int previousHourChoice = -1;
    private int previousMinuteChoice = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_targets);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize the GridView for Default Targets
        gridview = (GridView) findViewById(R.id.defaults_gridview);
        final DefaultTargetsGridviewAdapter gva = new DefaultTargetsGridviewAdapter(this);
        gridview.setAdapter(gva);

        // When items on the grid are pressed:
        // -- Show a time picker, which then updates the gridview values after a change
        gridview.setOnItemClickListener(GridClickListener);

        // Custom Targets
        // Initialize the CursorLoader
        getLoaderManager().initLoader(LOADER_ID, null, this);

        lvTargets = (ListView) findViewById(R.id.custom_targets_list);

        String[] from = {
            TimeEntryDbHandler.COLUMN_CENTER_OF_DAY,
            TimeEntryDbHandler.COLUMN_DIRECTION,
            TimeEntryDbHandler.COLUMN_TIME,
        };
        int[] to = {
            R.id.TimeEntryListviewDay,
            R.id.TimeEntryListviewDirection,
            R.id.TimeEntryListviewTime,
        };

        // Initialize the adapter. A null cursor is used on initialization, it will be passed when
        // LoaderManager delivers the data on onLoadFinished
        mAdapter = new TimeEntryListCursorAdapter(this, R.layout.time_entry_custom_targets_listview_row, null, from, to, 0);
        lvTargets.setAdapter(mAdapter);

        // Initialize the loader
        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, this);

        // Set onclick action of listview list items
        lvTargets.setOnItemClickListener(ListItemClickListener);

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
            //Toast.makeText(TargetsActivity.this, msg, Toast.LENGTH_SHORT).show();

            // Open CustomTargetPicker with defaults set, and the delete button available, and the date frozen
            CustomEntryPickerFragment fragment = new CustomEntryPickerFragment();
            Bundle args = new Bundle();
            args.putLong(CustomEntryPickerFragment.TAG_DEFAULT_DATE, cursor.getLong(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY)));
            args.putLong(CustomEntryPickerFragment.TAG_DEFAULT_TIME, cursor.getLong(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_TIME)));
            args.putString(CustomEntryPickerFragment.TAG_DEFAULT_DIRECTION, cursor.getString(cursor.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_DIRECTION)));
            args.putBoolean(CustomEntryPickerFragment.TAG_HAS_DELETE, true);
            args.putBoolean(CustomEntryPickerFragment.TAG_IS_FIXED_DATE, true);
            args.putBoolean(CustomEntryPickerFragment.TAG_IS_FIXED_DIRECTION, true);
            args.putString(CustomEntryPickerFragment.TAG_TITLE, getString(R.string.custom_target_picker_title_change));
            fragment.setArguments(args);

            fragment.pickerCallback = CustomTargetPickerCallback;
            fragment.show(getSupportFragmentManager(), "customTargetPicker_with_defaults");
        }
    };

    CustomEntryPickerFragment.PickerCallback CustomTargetPickerCallback = new CustomEntryPickerFragment.PickerCallback() {
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
                        time = new DateTime(year, month, day,hour, minute);
                    }
                    DateTime centerOfDay = new DateTime(year, month, day, 0, 0).plusHours(12);
                    mUpdateValues.put(TimeEntryDbHandler.COLUMN_TIME, time.getMillis());

                    String mSelectionClause = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + "=? AND " +
                            TimeEntryDbHandler.COLUMN_DIRECTION + "=? AND " +
                            TimeEntryDbHandler.COLUMN_TYPE + "=?";
                    String[] mSelectionArgs = {String.valueOf(centerOfDay.getMillis()), direction, TimeEntry.TimeEntryType.TIME_TARGET.name()};
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
                        mNewValues.put(TimeEntryDbHandler.COLUMN_TYPE, TimeEntry.TimeEntryType.TIME_TARGET.name());
                        mNewValues.put(TimeEntryDbHandler.COLUMN_DIRECTION, direction);
                        mNewValues.put(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY, centerOfDay.getMillis());

                        mNewUri = getContentResolver().insert(
                                TimeEntryContentProvider.CONTENT_URI,   // the content URI
                                mNewValues                          // the values to insert
                        );

                        toastText = getString(R.string.custom_target_picker_notify_added);
                    } else {
                        toastText = getString(R.string.custom_target_picker_notify_updated);
                    }


                    // Display notification toast on completion
                    handler.post(new Runnable() {
                        public void run() {

                            Toast.makeText(
                                    TargetsActivity.this,
                                    toastText,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }
            }.start();


            // Tracking
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("Add custom target")
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
                    // Determine the Center Of Day
                    DateTime centerOfDay = new DateTime(year, month, day, 0, 0).plusHours(12);


                    // Defines selection criteria for the rows you want to delete
                    String mSelectionClause = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + "=? AND " +
                            TimeEntryDbHandler.COLUMN_DIRECTION + "=? AND " +
                            TimeEntryDbHandler.COLUMN_TYPE + "=?";
                    String[] mSelectionArgs = {String.valueOf(centerOfDay.getMillis()), direction, TimeEntry.TimeEntryType.TIME_TARGET.name()};

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
                                    TargetsActivity.this,
                                    getString(R.string.deleted_custom_target),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                }
            }.start();
        }
    };


    AdapterView.OnItemClickListener GridClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            Bundle bundle = (Bundle)v.getTag(R.id.VIEW_TAG_BUNDLE);
            if (bundle == null || !bundle.getBoolean(getString(R.string.TAG_CHANGEABLE))) {
                return;
            }

            DefaultTargetPickerFragment dialogFragment = new DefaultTargetPickerFragment();

            dialogFragment.pickerCallback = new DefaultTargetPickerFragment.PickerCallback() {
                @Override
                public void callback(int hourOfDay, int minute) {
                    ((DefaultTargetsGridviewAdapter)gridview.getAdapter()).notifyDataSetChanged();
                    previousHourChoice = hourOfDay;
                    previousMinuteChoice = minute;

                    // Tracking
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("Change default target")
                            .build());
                }
            };

            // Supply default time to picker if selected element not empty
            if (bundle.getLong(TimeEntryDbHandler._ID) > 0) {
                Duration d = new Duration(bundle.getLong(TimeEntryDbHandler.COLUMN_TIME));
                Period p = d.plus(Period.hours(12).toStandardDuration()).toPeriod(); // Normalize, duration is centered on noon
                bundle.putInt(dialogFragment.TAG_DEFAULT_HOUR, p.getHours() % 24);
                bundle.putInt(dialogFragment.TAG_DEFAULT_MINUTE, p.getMinutes());
                bundle.putBoolean(dialogFragment.TAG_ARE_DEFAULTS, true);
            } else {
                // For empty elements, use previous picker choice, or leave defaults blank if no previous choice
                // Helps user fill defaults on first use
                if (previousHourChoice != -1 && previousMinuteChoice != -1) {
                    bundle.putInt(dialogFragment.TAG_DEFAULT_HOUR, previousHourChoice % 24);
                    bundle.putInt(dialogFragment.TAG_DEFAULT_MINUTE, previousMinuteChoice);
                    bundle.putBoolean(dialogFragment.TAG_ARE_DEFAULTS, true);
                }
            }

            dialogFragment.setArguments(bundle);

            dialogFragment.show(getSupportFragmentManager(), "defaultTimePicker");
        }
    };


    // CursorLoader used for Custom Targets
    // Callback that is invoked when system has initialized Loader and is ready to start query.
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        // Takes action based on the ID of the Loader that's being created

        String select = TimeEntryDbHandler.COLUMN_TYPE + "='" + TimeEntry.TimeEntryType.TIME_TARGET.name() + "'";

        switch (loaderID) {
            case LOADER_ID:
                // Returns a new CursorLoader
                return new CursorLoader(
                        TargetsActivity.this,   // Parent activity context
                        TimeEntryContentProvider.CONTENT_URI,        // Table to query
                        PROJECTION,     // Projection to return
                        select,            // Select only custom targets
                        null,            // No selection arguments
                        TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + " DESC"    // sort order
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
        getMenuInflater().inflate(R.menu.menu_targets, menu);
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

    // Method to add a new custom target to the database
    public void addCustomTarget(View view) {
        CustomEntryPickerFragment fragment = new CustomEntryPickerFragment();
        fragment.pickerCallback = CustomTargetPickerCallback;

        Bundle args = new Bundle();
        args.putString(CustomEntryPickerFragment.TAG_TITLE, getString(R.string.custom_target_picker_title_new));
        fragment.setArguments(args);
        fragment.show(getSupportFragmentManager(), CUSTOM_TARGET_PICKER_TAG);
    }
}
