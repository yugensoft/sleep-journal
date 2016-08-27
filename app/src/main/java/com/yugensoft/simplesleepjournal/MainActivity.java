package com.yugensoft.simplesleepjournal;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;
import com.squareup.picasso.Picasso;

import org.joda.time.DateTime;


// General Todos:

// Next Revision Notes:
// Add graphical time bars on current day + add/modify records + in records list
// Add records month splitters
public class MainActivity extends ActionBarActivity {

    // states
    public final int STATE_ASLEEP = 0;
    public final int STATE_AWAKE = 1;
    public final int STATE_UNKNOWN = 2;

    private int state = STATE_UNKNOWN;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pre-fetch the state images
        Picasso.with(MainActivity.this).load(R.drawable.moon).fetch();
        Picasso.with(MainActivity.this).load(R.drawable.sun).fetch();
        Picasso.with(MainActivity.this).load(R.drawable.unknown).fetch();

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("F961E2E362704F9592CC2F9CC025A1BF")
                .addKeyword("sleep")
                .addKeyword("rest")
                .addKeyword("tiredness")
                .addKeyword("insomnia")
                .addKeyword("well-rested")
                .addKeyword("lethargic")
                .addKeyword("bedtime")
                .addKeyword("exhausted")
                .addKeyword("exhaustion")
                .addKeyword("bed")
                .build();
        mAdView.loadAd(adRequest);

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Update the current state
        final SQLiteDatabase db = new TimeEntryDbHandler(this).getReadableDatabase();
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                long time24hoursAgo = new DateTime().minusHours(24).getMillis();
                // Get latest time entry that is more recent than 24 hours ago
                String q = "SELECT * FROM " + TimeEntryDbHandler.TABLE_TIME_ENTRIES + " " +
                           "WHERE " + TimeEntryDbHandler.COLUMN_TYPE + "='" + TimeEntry.TimeEntryType.TIME_RECORD.name() + "' " +
                           "AND " + TimeEntryDbHandler.COLUMN_TIME + " > " + String.valueOf(time24hoursAgo) + " " +
                           "ORDER BY " + TimeEntryDbHandler.COLUMN_TIME + " DESC LIMIT 1";
                Cursor c = db.rawQuery(q, null);

                if (c != null && c.getCount() > 0) {
                    c.moveToNext();
                    String direction = c.getString(c.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_DIRECTION));
                    if (direction.equalsIgnoreCase(TimeEntry.Direction.WAKE.name())) {
                        // Currently awake
                        handler.post(new Runnable() {
                            public void run() {
                                setState(STATE_AWAKE);
                            }
                        });
                    } else if (direction.equalsIgnoreCase(TimeEntry.Direction.BEDTIME.name())) {
                        // Currently asleep
                        handler.post(new Runnable() {
                            public void run() {
                                setState(STATE_ASLEEP);
                            }
                        });
                    } else {
                        // Should be an impossible state
                        throw new RuntimeException("Last Time Record was of an impossible direction");
                    }
                } else {
                    // No entry
                    handler.post(new Runnable() {
                        public void run() {
                            setState(STATE_UNKNOWN);
                        }
                    });
                }

            }
        }.start();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_about:
                openAboutDialog();
                return true;
            case R.id.action_export:
                exportData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    public void openAboutDialog(View view) {openAboutDialog();}
    public void openAboutDialog() {
        new AboutFragment().show(getSupportFragmentManager(), "about_dialog");
    }


    // Function to record an awake time
    // Assumption: awake time is expected to be after midnight of the previous day, and before midnight of the current day
    public void onWakeup(View view) {
        // Check if already asleep
        if (state == STATE_AWAKE) {
            // Now permitted, will update instead
        }

        DateTime now = new DateTime();
        DateTime startOfCurrentDay = now.withTimeAtStartOfDay();
        DateTime noonOfCurrentDay = startOfCurrentDay.plusHours(12);
        long centerOfDay = noonOfCurrentDay.getMillis();
        long time =  now.getMillis();

        // Attempt update of wakeup time, otherwise add new
        Uri mNewUri;
        ContentValues mNewValues = new ContentValues();

        mNewValues.put(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY, centerOfDay);
        mNewValues.put(TimeEntryDbHandler.COLUMN_TIME, time);
        mNewValues.put(TimeEntryDbHandler.COLUMN_TYPE, TimeEntry.TimeEntryType.TIME_RECORD.name());
        mNewValues.put(TimeEntryDbHandler.COLUMN_DIRECTION, TimeEntry.Direction.WAKE.name());

        String[] selectionArgs = {
                String.valueOf(centerOfDay),
                TimeEntry.TimeEntryType.TIME_RECORD.name(),
                TimeEntry.Direction.WAKE.name()
        };
        int rowsUpdated = getContentResolver().update(
                TimeEntryContentProvider.CONTENT_URI,
                mNewValues,
                TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + "=? AND " +
                        TimeEntryDbHandler.COLUMN_TYPE + "=? AND " +
                        TimeEntryDbHandler.COLUMN_DIRECTION + "=?" ,
                selectionArgs
        );
        if (rowsUpdated == 0) {
            mNewUri = getContentResolver().insert(
                    TimeEntryContentProvider.CONTENT_URI,   // the content URI
                    mNewValues                          // the values to insert
            );
            // Notify user of new record
            Toast.makeText(this, R.string.toast_awake_registered, Toast.LENGTH_SHORT).show();

        } else {
            // Notify user of update
            Toast.makeText(this, R.string.toast_awake_updated, Toast.LENGTH_SHORT).show();
        }


        // Update the state
        setState(STATE_AWAKE);

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Record wakeup")
                .build());
    }

    // Function to record a bed time
    // Assumption: bed time is expected to be after noon of current day, and before noon of the next day
    public void onBedtime(View view) {
        // Check if already asleep
        if (state == STATE_ASLEEP) {
            // Now permitted, will update instead
        }

        DateTime now = new DateTime();
        DateTime centerOfDay;

        if (now.hourOfDay().get() >= 12) {
            // after noon of current day
            centerOfDay = now.withTimeAtStartOfDay().plusHours(12);
        } else {
            // before noon of next day
            centerOfDay = now.withTimeAtStartOfDay().minusHours(12);
        }

        // Attempt update of bedtime, otherwise add new
        Uri mNewUri;
        ContentValues mNewValues = new ContentValues();

        mNewValues.put(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY, centerOfDay.getMillis());
        mNewValues.put(TimeEntryDbHandler.COLUMN_TIME, now.getMillis());
        mNewValues.put(TimeEntryDbHandler.COLUMN_TYPE, TimeEntry.TimeEntryType.TIME_RECORD.name());
        mNewValues.put(TimeEntryDbHandler.COLUMN_DIRECTION, TimeEntry.Direction.BEDTIME.name());

        String[] selectionArgs = {
                String.valueOf(centerOfDay.getMillis()),
                TimeEntry.TimeEntryType.TIME_RECORD.name(),
                TimeEntry.Direction.BEDTIME.name()
        };
        int rowsUpdated = getContentResolver().update(
                TimeEntryContentProvider.CONTENT_URI,
                mNewValues,
                TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + "=? AND " +
                        TimeEntryDbHandler.COLUMN_TYPE + "=? AND " +
                        TimeEntryDbHandler.COLUMN_DIRECTION + "=?" ,
                selectionArgs
        );
        if (rowsUpdated == 0) {
            mNewUri = getContentResolver().insert(
                    TimeEntryContentProvider.CONTENT_URI,   // the content URI
                    mNewValues                          // the values to insert
            );
            // Notify user of new record
            Toast.makeText(this, R.string.toast_bedtime_registered, Toast.LENGTH_SHORT).show();
        } else {
            // Notify user of update
            Toast.makeText(this, R.string.toast_bedtime_updated, Toast.LENGTH_SHORT).show();
        }

        // Update the state
        setState(STATE_ASLEEP);

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Record bedtime")
                .build());
    }


    // Method to open the list of records
    public void openRecords(View view) {
        openRecords();
    }
    public void openRecords() {
        Intent intent = new Intent(this, RecordsActivity.class);
        startActivity(intent);
    }

    // Method to export all data to CSV and share
    public void exportData(View view) { exportData();}
    public void exportData() {
        ExportToCsvTask task = new ExportToCsvTask(MainActivity.this);
        task.execute();

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("ExportToCSV")
                .build());
    }

    // Method to open targets activity to manage targets
    public void openTargets(View view) {
        openTargets();
    }
    public void openTargets() {
        Intent intent = new Intent(this, TargetsActivity.class);
        startActivity(intent);
    }

    public void openReport(View view) {
        openReport();
    }
    public void openReport() {
        Intent intent = new Intent(this, ReportActivity.class);
        startActivity(intent);
    }

    public void openHowToPage(View view) {
        openHowToPage();
    }
    public void openHowToPage() {
        Intent intent = new Intent(this, HowToActivity.class);
        startActivity(intent);
    }

    public void setState(int state) {
        final TextView txtState = (TextView)findViewById(R.id.current_state);
        final ImageView imgState = (ImageView)findViewById(R.id.sleep_state_image);
        final TextView txtLastTime = (TextView)findViewById(R.id.last_time);
        String lastTime;

        int previousState = this.state;
        int newState = state;
        this.state = state;

        switch (newState) {
            case STATE_AWAKE:
                txtState.setText(getString(R.string.main_state_awake));
                Picasso.with(MainActivity.this).load(R.drawable.sun).noFade().into(imgState);
                Cursor cursorAwake = getContentResolver().query(
                        TimeEntryContentProvider.CONTENT_URI,
                        TimeEntryDbHandler.FULL_PROJECTION,
                        TimeEntryDbHandler.COLUMN_TYPE + "='" + TimeEntry.TimeEntryType.TIME_RECORD.name() + "' " +
                                "AND " + TimeEntryDbHandler.COLUMN_DIRECTION + "='" + TimeEntry.Direction.WAKE.name() + "' ",
                        null,
                        TimeEntryDbHandler.COLUMN_TIME + " DESC"
                );
                cursorAwake.moveToNext();
                lastTime = new HumanReadableConverter(this).RelativeTime(
                        cursorAwake.getLong(cursorAwake.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY)),
                        cursorAwake.getLong(cursorAwake.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_TIME)),
                        cursorAwake.getString(cursorAwake.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_DIRECTION))
                );
                lastTime = lastTime.replaceAll(" *n\\.d\\.", ""); // Remove any trailing n.d.
                txtLastTime.setText(getString(R.string.last_awoke) + lastTime);
                break;
            case STATE_ASLEEP:
                txtState.setText(getString(R.string.main_state_asleep));
                Picasso.with(MainActivity.this).load(R.drawable.moon).noFade().into(imgState);
                Cursor cursorAsleep = getContentResolver().query(
                        TimeEntryContentProvider.CONTENT_URI,
                        TimeEntryDbHandler.FULL_PROJECTION,
                        TimeEntryDbHandler.COLUMN_TYPE + "='" + TimeEntry.TimeEntryType.TIME_RECORD.name() + "' " +
                                "AND " + TimeEntryDbHandler.COLUMN_DIRECTION + "='" + TimeEntry.Direction.BEDTIME.name() + "' ",
                        null,
                        TimeEntryDbHandler.COLUMN_TIME + " DESC"
                );
                cursorAsleep.moveToNext();
                
                lastTime = new HumanReadableConverter(this).RelativeTime(
                        cursorAsleep.getLong(cursorAsleep.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY)),
                        cursorAsleep.getLong(cursorAsleep.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_TIME)),
                        cursorAsleep.getString(cursorAsleep.getColumnIndexOrThrow(TimeEntryDbHandler.COLUMN_DIRECTION))
                );
                lastTime = lastTime.replaceAll(" *n\\.d\\.", ""); // Remove any trailing n.d.
                txtLastTime.setText(getString(R.string.last_asleep) + lastTime);
                break;
            case STATE_UNKNOWN:
                txtState.setText(getString(R.string.main_state_unknown));
                Picasso.with(MainActivity.this).load(R.drawable.unknown).noFade().into(imgState);
                txtLastTime.setText("");
                break;
            default:
                throw new RuntimeException("Unknown new state");
        }

    }

}
