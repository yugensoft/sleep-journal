package com.yugensoft.simplesleepjournal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
    private static final String TAG = "records_visual";

    private ListView mListView;
    private TimeEntryDisplayCursorAdapter mAdapter;
    private Tracker mTracker;
    private CustomEntryPickerFragment.PickerCallback CustomRecordPickerCallback;
    private DayRecordsDisplayBar mHeaderBar;
    private ViewGroup mHeader;

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
        mHeader = (ViewGroup) findViewById(R.id.header);
        mHeaderBar = (DayRecordsDisplayBar)mHeader.findViewById(R.id.display_bar);
        mHeaderBar.makeHeader();
        TextView headerLabel = (TextView)mHeader.findViewById(R.id.date_textview);
        headerLabel.setText(R.string.date);

        // apply scale if different
        float prefScale = loadScale();
        if(Float.compare(prefScale, mRoundedScaleFactor) != 0){
            mScaleFactor = prefScale;
            mRoundedScaleFactor = prefScale;
            changeViewScale(prefScale);
        }

        //load data
        getSupportLoaderManager().initLoader(RECORDS_LOADER_ID,null,this).forceLoad();
        //load targets
        getSupportLoaderManager().initLoader(TARGETS_LOADER_ID,null,this).forceLoad();

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // create the callback
        CustomRecordPickerCallback = CustomEntryPickerFragment.getStandardCustomRecordPickerCallback(this, mTracker, onComplete);

        // focus level
        mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);

                return false;
            }
        });
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

    // Zoom / scaling
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.0f;
    private float mRoundedScaleFactor = 1.0f;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
        return false;
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            Log.d(TAG, "scale input: " + String.valueOf(mScaleFactor));

            // Don't let the scale get too small or too large.
            mScaleFactor = Math.max(DayRecordsDisplayBar.ScalableSizeParameters.MINIMUM_SCALE,
                    Math.min(mScaleFactor, DayRecordsDisplayBar.ScalableSizeParameters.MAXIMUM_SCALE));

            // quantize the scale before invalidating views (to avoid excessive re-rendering)
            float roundedScaleFactor = DayRecordsDisplayBar.ScalableSizeParameters.ROUNDING_FRACTION *
                    Math.round(mScaleFactor / DayRecordsDisplayBar.ScalableSizeParameters.ROUNDING_FRACTION);

            // check if the new resulting rounded scale is different to the old one
            if (Float.compare(roundedScaleFactor, mRoundedScaleFactor) != 0) {  // means "different"
                mRoundedScaleFactor = roundedScaleFactor;
                changeViewScale(roundedScaleFactor);
                saveScale(roundedScaleFactor);
            }

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }
    }

    /**
     * Does everything required to change the scales of the views.
     * @param roundedScaleFactor New scale.
     */
    private void changeViewScale(float roundedScaleFactor) {
        // change scales
        mAdapter.setScale(roundedScaleFactor);
        mHeaderBar.setRenderScale(roundedScaleFactor);
        Log.d(TAG, "scale change: " + String.valueOf(roundedScaleFactor));
        mListView.setAdapter(mAdapter);
        mListView.invalidateViews();
        mHeaderBar.requestLayout();
    }

    /**
     * Saves the scale as a preference.
     * @param roundedScaleFactor The scale to save.
     */
    private void saveScale(float roundedScaleFactor) {
        // save as preference
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(getString(R.string.preference_records_visual_scale), roundedScaleFactor);
        editor.apply();
    }

    /**
     * Loads the scale preference.
     * @return The scale.
     */
    private float loadScale(){
        //load the scale preference
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getFloat(getString(R.string.preference_records_visual_scale), 1.0f);
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
