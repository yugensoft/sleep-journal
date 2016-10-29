package com.yugensoft.simplesleepjournal;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.customviews.SleepComparisonBar;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class ReportActivity extends ActionBarActivity {
    private static final int TOTAL_DEFAULT_TARGETS = 14; // 7 days, 2 directions

    // State variables and tags
    public final String STATE_START_OF_PERIOD = "startOfPeriod";
    public final String STATE_END_OF_PERIOD = "endOfPeriod";
    private long sStartOfPeriod = 0;
    private long sEndOfPeriod = 0;

    // Intent bundle calling tags
    public static final String TAG_AD_REMOVE_PURCHASED = "showads";

    private Tracker mTracker;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of state variables from saved state
            sStartOfPeriod = savedInstanceState.getLong(STATE_START_OF_PERIOD);
            sEndOfPeriod = savedInstanceState.getLong(STATE_END_OF_PERIOD);
        }

        // Load the options into the Time Period spinner
        Spinner spinner = (Spinner) findViewById(R.id.time_period_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.time_period_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(SpinnerClickListener);

        // Load the ad
        mAdView = (AdView) findViewById(R.id.adView);
        boolean isAdRemovePurchased = getIntent().getExtras().getBoolean(TAG_AD_REMOVE_PURCHASED, true);
        if (MainActivity.DEBUG_LOGS) Log.d("sleep-report", "onCreate: show ad is: "+String.valueOf(isAdRemovePurchased));
        if(!isAdRemovePurchased) {
            AdFunctions.loadAdIntoAdView(mAdView);
        }

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

    @Override
    public void onStart() {
        super.onStart();

        generateReport(sStartOfPeriod, sEndOfPeriod);
    }

    AdapterView.OnItemSelectedListener SpinnerClickListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selected = parent.getSelectedItem().toString().trim();

            DateTime noonYesterday = new DateTime().withTimeAtStartOfDay().minusHours(12);

            // Check if custom period selected first
            if (selected.equalsIgnoreCase(getString(R.string.custom))) {
                enableCustomTimePeriod(true);
                // If no period set already, use a default custom time period of yesterday->yesterday
                if (sStartOfPeriod == 0) {
                    sStartOfPeriod = noonYesterday.getMillis();
                }
                if (sEndOfPeriod == 0) {
                    sEndOfPeriod = noonYesterday.getMillis();
                }

                // Tracking
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Select report custom")
                        .build());
            } else {
                enableCustomTimePeriod(false);
            }

            // Check remainder of period options
            if (selected.equalsIgnoreCase(getString(R.string.last_week))) {
                // Last week period, yesterday -> -7 days
                sEndOfPeriod = noonYesterday.getMillis();
                sStartOfPeriod = noonYesterday.minusDays(7).getMillis();

                // Tracking
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Select report week")
                        .build());
            } else if (selected.equalsIgnoreCase(getString(R.string.last_fortnight))) {
                // Last fortnight period, yesterday -> -14 days
                sStartOfPeriod = noonYesterday.minusDays(14).getMillis();

                // Tracking
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Select report fortnight")
                        .build());
            } else if (selected.equalsIgnoreCase(getString(R.string.last_month))) {
                // Last month period, yesterday -> 30 days
                sStartOfPeriod = noonYesterday.minusDays(30).getMillis();

                // Tracking
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Select report month")
                        .build());
            }

            // Update the time period textview texts
            ((TextView) findViewById(R.id.time_period_start)).setText(new HumanReadableConverter(ReportActivity.this).ConvertDate(sStartOfPeriod));
            ((TextView) findViewById(R.id.time_period_end)).setText(new HumanReadableConverter(ReportActivity.this).ConvertDate(sEndOfPeriod));

            // Regenerate report
            generateReport(sStartOfPeriod, sEndOfPeriod);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    public void generateReport(final long timeStart, final long timeEnd) {
        // Fill the report values from the database
        final SQLiteDatabase db = new TimeEntryDbHandler(ReportActivity.this).getReadableDatabase();
        final TextView txtTotalSleep = (TextView)findViewById(R.id.total_sleep);
        final TextView txtAverageSleep = (TextView)findViewById(R.id.average_sleep);
        final TextView txtWakeupCompliance = (TextView)findViewById(R.id.wakeup_compliance);
        final TextView txtBedtimeCompliance = (TextView)findViewById(R.id.bedtime_compliance);
        final TextView txtAppUsage = (TextView)findViewById(R.id.app_usage);
        final SleepComparisonBar wakeupBar = (SleepComparisonBar)findViewById(R.id.wakeup_comparison_bar);
        final SleepComparisonBar bedtimeBar = (SleepComparisonBar)findViewById(R.id.bedtime_comparison_bar);

        // Needs the time period set to generate the report
        if (timeStart == 0 || timeEnd == 0)
            return;

        final android.os.Handler handler = new android.os.Handler();
        new Thread() {
            @Override
            public void run() {
                /* Query that returns the following columns:
                ** total_sleep, average_sleep, sleep_count
                */
                final String colTime = TimeEntryDbHandler.COLUMN_TIME; // used to reduce verbosity
                final String colType = TimeEntryDbHandler.COLUMN_TYPE;
                final String colDay = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY;
                final String colDir = TimeEntryDbHandler.COLUMN_DIRECTION;
                String querySleepAggregates =
                        "SELECT " +
                        "  SUM(awake_time_entries." + colTime + " - bedtime_time_entries." + colTime + ") AS total_sleep," +
                        "  SUM(awake_time_entries." + colTime + " - bedtime_time_entries." + colTime + ") / COUNT(*) AS average_sleep," +
                        "  COUNT(*) as sleep_count " +
                        "FROM " +
                        TimeEntryDbHandler.TABLE_TIME_ENTRIES + " AS awake_time_entries " +
                        "INNER JOIN " +
                        TimeEntryDbHandler.TABLE_TIME_ENTRIES + " AS bedtime_time_entries ON ( " +
                        "  awake_time_entries." + colDay + " = (bedtime_time_entries." + colDay + " + 86400000) " +   // 'Today awake' matched to 'yesterday bedtime'
                        "AND bedtime_time_entries." + colTime + " < awake_time_entries." + colTime + " " +
                        ")" +
                        "WHERE awake_time_entries." + colDir + " = '" + TimeEntry.Direction.WAKE + "' " +
                        "  AND bedtime_time_entries." + colDir + " = '" + TimeEntry.Direction.BEDTIME + "' " +
                        "  AND awake_time_entries." + colType + " = '" + TimeEntry.TimeEntryType.TIME_RECORD + "' " +
                        "  AND bedtime_time_entries." + colType + " = '" + TimeEntry.TimeEntryType.TIME_RECORD + "' " +
                        "  AND bedtime_time_entries." + colDay + " <= " + timeEnd + " " +
                        "  AND bedtime_time_entries." + colDay + " >= " + timeStart + " " +
                        "";
                Cursor cursorSleepAggregates = db.rawQuery(querySleepAggregates, null);

                final long total_sleep, average_sleep, sleep_count;
                if(cursorSleepAggregates != null && cursorSleepAggregates.getCount() > 0) {
                    cursorSleepAggregates.moveToNext();
                    total_sleep = cursorSleepAggregates.getLong(cursorSleepAggregates.getColumnIndexOrThrow("total_sleep"));
                    average_sleep = cursorSleepAggregates.getLong(cursorSleepAggregates.getColumnIndexOrThrow("average_sleep"));
                    sleep_count = cursorSleepAggregates.getLong(cursorSleepAggregates.getColumnIndexOrThrow("sleep_count"));

                } else {
                    return;
                }

                /* Query that returns the following columns:
                ** defaults_count
                 */
                String queryDefaultsCheck = "SELECT COUNT(*) AS defaults_count FROM " +
                    "( " +
                    "SELECT DISTINCT " +
                    "  default_targets." + colType + ", " +
                    "  " + colDir + " " +
                    "FROM " +
                    TimeEntryDbHandler.TABLE_TIME_ENTRIES + " AS default_targets " +
                    "WHERE " +
                    "  default_targets." + colType + " LIKE '%DEFAULT' " +
                    ")";
                Cursor cursorDefaultsCheck = db.rawQuery(queryDefaultsCheck, null);

                final long defaults_count;
                final long bedtime_compliance_ms, wakeup_compliance_ms;
                final String wakeup_compliance;
                final String bedtime_compliance;
                final boolean defaultsMissing;
                if(cursorDefaultsCheck != null && cursorSleepAggregates.getCount() > 0) {
                    cursorDefaultsCheck.moveToNext();
                    defaults_count = cursorDefaultsCheck.getLong(cursorDefaultsCheck.getColumnIndexOrThrow("defaults_count"));
                    if (defaults_count < TOTAL_DEFAULT_TARGETS) {
                        defaultsMissing = true;
                    } else if (defaults_count > TOTAL_DEFAULT_TARGETS) {
                        throw new RuntimeException("Database corruption: more than 14 default targets");
                    } else {
                        defaultsMissing = false;
                    }
                } else {
                    defaultsMissing = true;
                }
                if (defaultsMissing) {
                    wakeup_compliance = getString(R.string.default_targets_missing);
                    bedtime_compliance = getString(R.string.default_targets_missing);
                    wakeup_compliance_ms = 0;
                    bedtime_compliance_ms = 0;
                } else {
                    Cursor cursorWakeupCompliance = db.rawQuery(complianceQuery(TimeEntry.Direction.WAKE, timeStart, timeEnd),null);
                    cursorWakeupCompliance.moveToNext();
                    wakeup_compliance_ms = cursorWakeupCompliance.getLong(cursorWakeupCompliance.getColumnIndexOrThrow("average_offset_time"));
                    wakeup_compliance = new HumanReadableConverter(ReportActivity.this).MillisecondsToStandard(wakeup_compliance_ms,
                            true);
                    Cursor cursorBedtimeCompliance = db.rawQuery(complianceQuery(TimeEntry.Direction.BEDTIME, timeStart, timeEnd), null);
                    cursorBedtimeCompliance.moveToNext();
                    bedtime_compliance_ms = cursorBedtimeCompliance.getLong(cursorBedtimeCompliance.getColumnIndexOrThrow("average_offset_time"));
                    bedtime_compliance = new HumanReadableConverter(ReportActivity.this).MillisecondsToStandard(bedtime_compliance_ms,
                            true);
                    
                }

                // Set the textviews and bars with the values
                handler.post(new Runnable() {
                    public void run() {
                        HumanReadableConverter hcr = new HumanReadableConverter(ReportActivity.this);
                        txtAverageSleep.setText(hcr.MillisecondsToStandard(average_sleep, false));
                        txtTotalSleep.setText(hcr.MillisecondsToStandard(total_sleep, false));
                        if(defaultsMissing) {
                            txtWakeupCompliance.setText(wakeup_compliance);
                            txtBedtimeCompliance.setText(bedtime_compliance);

                            txtWakeupCompliance.setVisibility(View.VISIBLE);
                            txtBedtimeCompliance.setVisibility(View.VISIBLE);
                            wakeupBar.setVisibility(View.GONE);
                            bedtimeBar.setVisibility(View.GONE);
                        } else {
                            bedtimeBar.setTimeDifference(bedtime_compliance_ms);
                            wakeupBar.setTimeDifference(wakeup_compliance_ms);

                            txtWakeupCompliance.setVisibility(View.GONE);
                            txtBedtimeCompliance.setVisibility(View.GONE);
                            wakeupBar.setVisibility(View.VISIBLE);
                            bedtimeBar.setVisibility(View.VISIBLE);

                        }

                        int sleeps_in_period = (int)(new Duration(sStartOfPeriod, sEndOfPeriod).getStandardDays());
                        int appUsagePercentage = (int)(((double)sleep_count / (double)sleeps_in_period) * 100);
                        txtAppUsage.setText("" + sleep_count + " / " + sleeps_in_period + " (" + appUsagePercentage + "%)");
                    }
                });

            }
        }.start();
    }

    private String complianceQuery(TimeEntry.Direction direction, long timeStart, long timeEnd) {
        /* Query that returns the following columns:
        ** average_offset_time
         */
        final String colTime = TimeEntryDbHandler.COLUMN_TIME; // used to reduce verbosity
        final String colType = TimeEntryDbHandler.COLUMN_TYPE;
        final String colDay = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY;
        final String colDir = TimeEntryDbHandler.COLUMN_DIRECTION;
        return  "SELECT " +
                "	 SUM(records." + colTime + " -  " +
                "	 (CASE WHEN custom_targets." + colTime + " IS null THEN (records." + colDay + " + targets." + colTime + ") " +
                "	 ELSE (custom_targets." + colTime + ") END " +
                "	 ))  " +
                "	 / COUNT(*) AS average_offset_time " +
                "FROM  " +
                "	" + TimeEntryDbHandler.TABLE_TIME_ENTRIES + " as records " +
                "CROSS JOIN " +
                "	" + TimeEntryDbHandler.TABLE_TIME_ENTRIES + " as targets ON ( " +
                "		case cast (strftime('%w', records." + colTime + "/1000, 'unixepoch') as integer)  " +    //-- Get dayname of record
                "		when 0 then 'Sunday' " +
                "		when 1 then 'Monday' " +
                "		when 2 then 'Tuesday' " +
                "		when 3 then 'Wednesday' " +
                "		when 4 then 'Thursday' " +
                "		when 5 then 'Friday' " +
                "		else 'Saturday' end || '_default'  " +
                "		= targets." + colType + " COLLATE NOCASE   " +    //-- Cross join matching default target
                "		AND records." + colDir + " = targets." + colDir + " " +
                "		) " +
                "LEFT OUTER JOIN  " +
                "	" + TimeEntryDbHandler.TABLE_TIME_ENTRIES + " AS custom_targets ON ( " +
                "		records." + colDay + " = custom_targets." + colDay + "  " +   //-- Outer join matching custom targets
                "		AND custom_targets." + colType + " = 'TIME_TARGET' " +
                "		AND records." + colDir + " = custom_targets." + colDir + " " +
                "		) " +
                "WHERE " +
                "	records." + colType + " = 'TIME_RECORD' " +
                "	AND records." + colDir + " = '" + direction.name() + "' " +
                "   AND records." + colDay + " <= " + timeEnd + " " +
                "   AND records." + colDay + " >= " + timeStart + " " +
                "";
        
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the state variables
        savedInstanceState.putLong(STATE_END_OF_PERIOD, sEndOfPeriod);
        savedInstanceState.putLong(STATE_START_OF_PERIOD, sStartOfPeriod);

        // Call super to save view hierarchy
        super.onSaveInstanceState(savedInstanceState);
    }

    // Method which enables/disables Clickability of the Time Period textviews
    // thereby enabling custom time periods
    // and also changes their style to indicate this
    public void enableCustomTimePeriod(boolean enable) {
        TextView txtStart = (TextView)findViewById(R.id.time_period_start);
        TextView txtEnd = (TextView)findViewById(R.id.time_period_end);
        if (enable) {
            txtStart.setClickable(true);
            txtStart.setTextColor(getResources().getColor(R.color.changeable_textview_colour));
            txtEnd.setClickable(true);
            txtEnd.setTextColor(getResources().getColor(R.color.changeable_textview_colour));
        } else {
            txtStart.setClickable(false);
            txtStart.setTextColor(getResources().getColor(R.color.primary_text_default_material_light));
            txtEnd.setClickable(false);
            txtEnd.setTextColor(getResources().getColor(R.color.primary_text_default_material_light));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_report, menu);
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

    public void pickStartOfPeriod(View view) {
        CustomDatePickerFragment fragment = new CustomDatePickerFragment();
        if (sStartOfPeriod > 0) {
            DateTime startOfPeriod = new DateTime(sStartOfPeriod);
            Bundle args = new Bundle();
            args.putBoolean(fragment.TAG_ARE_DEFAULTS, true);
            args.putInt(fragment.TAG_DEFAULT_DAY, startOfPeriod.getDayOfMonth());
            args.putInt(fragment.TAG_DEFAULT_MONTH, startOfPeriod.getMonthOfYear());
            args.putInt(fragment.TAG_DEFAULT_YEAR, startOfPeriod.getYear());
            fragment.setArguments(args);
        }
        fragment.pickerCallback = new CustomDatePickerFragment.PickerCallback() {
            @Override
            public void callback(int year, int month, int day) {
                sStartOfPeriod = new DateTime(year, month, day, 12, 0, 0).getMillis();
                ((TextView) findViewById(R.id.time_period_start)).setText(new HumanReadableConverter(ReportActivity.this).ConvertDate(sStartOfPeriod));
                generateReport(sStartOfPeriod, sEndOfPeriod);
            }
        };

        fragment.show(getSupportFragmentManager(), "startOfPeriodPicker");


        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Select report custom start")
                .build());

    }

    public void pickEndOfPeriod(View view) {
        CustomDatePickerFragment fragment = new CustomDatePickerFragment();
        if (sEndOfPeriod > 0) {
            DateTime endOfPeriod = new DateTime(sEndOfPeriod);
            Bundle args = new Bundle();
            args.putBoolean(fragment.TAG_ARE_DEFAULTS, true);
            args.putInt(fragment.TAG_DEFAULT_DAY, endOfPeriod.getDayOfMonth());
            args.putInt(fragment.TAG_DEFAULT_MONTH, endOfPeriod.getMonthOfYear());
            args.putInt(fragment.TAG_DEFAULT_YEAR, endOfPeriod.getYear());
            fragment.setArguments(args);
        }
        fragment.pickerCallback = new CustomDatePickerFragment.PickerCallback() {
            @Override
            public void callback(int year, int month, int day) {
                sEndOfPeriod = new DateTime(year, month, day, 12, 0, 0).getMillis();
                ((TextView) findViewById(R.id.time_period_end)).setText(new HumanReadableConverter(ReportActivity.this).ConvertDate(sEndOfPeriod));
                generateReport(sStartOfPeriod, sEndOfPeriod);
            }
        };

        fragment.show(getSupportFragmentManager(), "endOfPeriodPicker");

        // Tracking
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Select report custom end")
                .build());
    }


}
