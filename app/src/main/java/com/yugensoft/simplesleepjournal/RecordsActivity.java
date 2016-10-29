package com.yugensoft.simplesleepjournal;

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import org.joda.time.DateTime;

import java.util.ArrayList;

public class RecordsActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String DIALOG_TAG = "customRecordPicker_with_defaults";

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
    private static final String KEY_RANGE_LOW = "rangelow";
    private static final String KEY_RANGE_HIGH = "rangehigh";
    // The adapter that binds the data to the listview
    private TimeEntryListCursorAdapter mAdapter;
    private CustomEntryPickerFragment.PickerCallback CustomRecordPickerCallback;

    private ListView listView;
    private TextView txtTitle;
    private Tracker mTracker;

    private Long mRangeLow = null;
    private Long mRangeHigh = null;

    /**
     * Factory method.
     * @param context Context.
     * @param rangeLow Center-of-day low range to display.
     * @param rangeHigh Center-of-day high range to display.
     * @return Intent.
     */
    public static Intent newInstance(Context context, @Nullable Long rangeLow, @Nullable Long rangeHigh){
        Intent intent = new Intent(context, RecordsActivity.class);
        if(rangeLow != null) intent.putExtra(KEY_RANGE_LOW,rangeLow);
        if(rangeHigh != null) intent.putExtra(KEY_RANGE_HIGH,rangeHigh);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get any parameters
        long rangeHigh = getIntent().getLongExtra(KEY_RANGE_HIGH,-1);
        long rangeLow = getIntent().getLongExtra(KEY_RANGE_LOW,-1);
        mRangeHigh = (rangeHigh != -1) ? rangeHigh : null;
        mRangeLow = (rangeLow != -1) ? rangeLow : null;

        // change the title to match parameters
        txtTitle = (TextView)findViewById(R.id.text_title);
        String title = getString(R.string.activity_records_title);
        HumanReadableConverter hcr = new HumanReadableConverter(this);
        if(mRangeHigh == null && mRangeLow == null){
            title += " (" + getString(R.string.all) + ")";
        } else if (mRangeHigh.equals(mRangeLow)) {
            title += " " + getString(R.string.for_str) + " " + hcr.ConvertDate(mRangeLow);
        } else {
            if (mRangeLow != null) {
                title += " " + getString(R.string.from) + " " + hcr.ConvertDate(mRangeLow);

            }
            if (mRangeHigh != null) {
                title += " " + getString(R.string.to) + " " + hcr.ConvertDate(mRangeHigh);
            }
        }
        txtTitle.setText(title);

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

        // Create the callback
       CustomRecordPickerCallback = CustomEntryPickerFragment.getStandardCustomRecordPickerCallback(this,mTracker,null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // Restore the picker callback if dialog fragment still exists
        CustomEntryPickerFragment dialogFragment = (CustomEntryPickerFragment) getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
        if(dialogFragment != null){
            dialogFragment.pickerCallback = CustomRecordPickerCallback;
        }
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
            fragment.show(getSupportFragmentManager(), DIALOG_TAG);

        }
    };


    // Method called by the Add New button
    public void addRecord(View v) {
        CustomEntryPickerFragment fragment = new CustomEntryPickerFragment();
        Bundle args = new Bundle();
        args.putString(fragment.TAG_TITLE, getString(R.string.add_sleep_record_dialog_title));
        if(mRangeLow != null) {
            args.putLong(fragment.TAG_DEFAULT_DATE, mRangeLow);
        };
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
                String selection = TimeEntryDbHandler.COLUMN_TYPE + "=? ";
                ArrayList<String> selectionArgs = new ArrayList<>();
                selectionArgs.add(TimeEntry.TimeEntryType.TIME_RECORD.name());

                // apply range constaints if present
                if(mRangeLow != null) {
                    selection += " AND " + TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + ">=? ";
                    selectionArgs.add(mRangeLow.toString());
                }
                if(mRangeHigh != null){
                    selection += " AND " + TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + "<=? ";
                    selectionArgs.add(mRangeHigh.toString());
                }

                String sortOrder = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + " DESC, " +
                        TimeEntryDbHandler.COLUMN_TIME + " DESC";
                return new CursorLoader(
                        RecordsActivity.this,   // Parent activity context
                        TimeEntryContentProvider.CONTENT_URI,        // Table to query
                        PROJECTION,     // Projection to return
                        selection,            // selection clause
                        selectionArgs.toArray(new String[selectionArgs.size()]),        // selection arguments
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
