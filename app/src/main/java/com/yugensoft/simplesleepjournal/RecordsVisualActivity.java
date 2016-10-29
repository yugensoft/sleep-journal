package com.yugensoft.simplesleepjournal;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.customviews.DayRecordsDisplayBar;
import com.yugensoft.simplesleepjournal.database.TimeEntry;

import java.util.ArrayList;

public class RecordsVisualActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks {
    private static final int RECORDS_LOADER_ID = 1;
    private static final int TARGETS_LOADER_ID = 2;
    private static final String DIALOG_TAG = "customRecordPicker";

    private ListView mListView;
    private TimeEntryDisplayCursorAdapter mAdapter;
    private Tracker mTracker;
    private CustomEntryPickerFragment.PickerCallback CustomRecordPickerCallback;

    public static Intent newInstance(Context context) {
        return new Intent(context, RecordsVisualActivity.class);
    }

    private AdapterView.OnItemClickListener ListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(RecordsActivity.newInstance(RecordsVisualActivity.this,id,id));
        }
    };

    Runnable onComplete = new Runnable() {
        @Override
        public void run() {
            getSupportLoaderManager().restartLoader(RECORDS_LOADER_ID, null, RecordsVisualActivity.this).forceLoad();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records_visual);

        mListView = (ListView)findViewById(R.id.listview);

        mAdapter = new TimeEntryDisplayCursorAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(ListItemClickListener);

        //header
        ViewGroup header = (ViewGroup)getLayoutInflater().inflate(R.layout.time_entry_day_visual_listview_row, mListView,false);
        DayRecordsDisplayBar headerBar = (DayRecordsDisplayBar)header.findViewById(R.id.display_bar);
        headerBar.makeHeader();
        TextView headerLabel = (TextView)header.findViewById(R.id.date_textview);
        headerLabel.setText(R.string.date);
        mListView.addHeaderView(header);

        //load data
        getSupportLoaderManager().initLoader(RECORDS_LOADER_ID,null,this).forceLoad();
        //load targets
        getSupportLoaderManager().initLoader(TARGETS_LOADER_ID,null,this).forceLoad();

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // create the callback
        CustomRecordPickerCallback = CustomEntryPickerFragment.getStandardCustomRecordPickerCallback(this, mTracker, onComplete);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_records_visual, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_list_all:
                startActivity(RecordsActivity.newInstance(this, null, null));
                return true;
            case R.id.action_add:
                addRecord(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method to add new record
     * @param view Optional calling view.
     */
    public void addRecord(@Nullable View view) {
        CustomEntryPickerFragment fragment = new CustomEntryPickerFragment();
        Bundle args = new Bundle();
        args.putString(fragment.TAG_TITLE, getString(R.string.add_sleep_record_dialog_title));
        fragment.setArguments(args);

        fragment.pickerCallback = CustomRecordPickerCallback;
        fragment.show(getSupportFragmentManager(), DIALOG_TAG);
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

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch(id){
            case RECORDS_LOADER_ID:
                return new DayRecordsLoader(RecordsVisualActivity.this);
            case TARGETS_LOADER_ID:
                return new DayTargetsLoader(RecordsVisualActivity.this);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch(loader.getId()){
            case RECORDS_LOADER_ID:
                mAdapter.repopulateData((ArrayList<TimeEntry>)data);
                mListView.setAdapter(mAdapter);
                mListView.invalidateViews();
                break;
            case TARGETS_LOADER_ID:
                mAdapter.repopulateTargets((ArrayList<TimeEntry>)data);
                mListView.setAdapter(mAdapter);
                mListView.invalidateViews();
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch(loader.getId()) {
            case RECORDS_LOADER_ID:
                mAdapter.repopulateData(new ArrayList<TimeEntry>());
                break;
            case TARGETS_LOADER_ID:
                mAdapter.repopulateTargets(new ArrayList<TimeEntry>());
                break;
        }
    }


    /**
     * For testing use
     * @return A fake list of time entries
     */
    private ArrayList<TimeEntry> getTestData(){
        ArrayList<TimeEntry> data = new ArrayList<>();
        data.add(new TimeEntry(1486555200000L, 1486540800000L, TimeEntry.TimeEntryType.TIME_RECORD, TimeEntry.Direction.WAKE));
        data.add(new TimeEntry(1486555200000L, 1486584000000L, TimeEntry.TimeEntryType.TIME_RECORD, TimeEntry.Direction.BEDTIME));
        data.add(new TimeEntry(1486641600000L, 1486630800000L, TimeEntry.TimeEntryType.TIME_RECORD, TimeEntry.Direction.WAKE));
        data.add(new TimeEntry(1486641600000L, 1486681200000L, TimeEntry.TimeEntryType.TIME_RECORD, TimeEntry.Direction.BEDTIME));
        data.add(new TimeEntry(1487246400000L, 1487233800000L, TimeEntry.TimeEntryType.TIME_RECORD, TimeEntry.Direction.WAKE));

        return data;
    }


}
