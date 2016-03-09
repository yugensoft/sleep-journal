package com.yugensoft.simplesleepjournal;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;


public class WholeDatabaseActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whole_database);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize the CursorLoader
        getLoaderManager().initLoader(LOADER_ID, null, this);


        listView = (ListView) findViewById(R.id.records_list);

        String[] from =
        {
            TimeEntryDbHandler.COLUMN_CENTER_OF_DAY,
            TimeEntryDbHandler.COLUMN_DIRECTION,
            TimeEntryDbHandler.COLUMN_TIME,
            TimeEntryDbHandler.COLUMN_TYPE,
            //TimeEntryDbHandler._ID,
        };
        int[] to =
        {
            R.id.TimeEntryListviewDay,
            R.id.TimeEntryListviewDirection,
            R.id.TimeEntryListviewTime,
            R.id.TimeEntryListviewType,
           // R.id.TimeEntryListviewId,
        };

        // Initialize the adapter. A null cursor is used on initialization, it will be passed when
        // LoaderManager delivers the data on onLoadFinished
        mAdapter = new TimeEntryListCursorAdapter(this, R.layout.time_entry_database_listview_row, null, from, to, 0);
        listView.setAdapter(mAdapter);

        // Initialize the loader
        LoaderManager lm = getLoaderManager();
        lm.initLoader(LOADER_ID, null, this);

        // Set onclick action of listview list items
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);

                String msg = "ID: " + String.valueOf(id);
                Toast.makeText(WholeDatabaseActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Callback that is invoked when system has initialized Loader and is ready to start query.
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

        // Takes action based on the ID of the Loader that's being created

        switch (loaderID) {
            case LOADER_ID:
                // Returns a new CursorLoader
                return new CursorLoader(
                        WholeDatabaseActivity.this,   // Parent activity context
                        TimeEntryContentProvider.CONTENT_URI,        // Table to query
                        PROJECTION,     // Projection to return
                        null,            // No selection clause
                        null,            // No selection arguments
                        null             // Default sort order
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
