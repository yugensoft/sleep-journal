package com.yugensoft.simplesleepjournal;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import org.joda.time.DateTime;

/**
 */
public class CustomEntryPickerFragment extends DialogFragment {
    public static final String TAG_TITLE = "title";
    public static final String TAG_DEFAULT_TIME = "time";
    public static final String TAG_DEFAULT_DATE = "date";
    public static final String TAG_DEFAULT_DIRECTION = "type";
    public static final String TAG_IS_FIXED_DIRECTION = "isfixedtype";
    public static final String TAG_IS_FIXED_DATE = "isfixeddate";
    public static final String TAG_IS_FIXED_TIME = "isfixedtime";
    public static final String TAG_HAS_DELETE = "hasdelete";
    public static final String TAG_ROW_ID = "rowid";

    private static final String TIME_PICKER_TAG = "customTimePicker";
    private static final String DATE_PICKER_TAG = "customDatePicker";

    private int mYear = -1;
    private int mMonth = -1;
    private int mDay = -1;
    private int mHour = -1;
    private int mMinute = -1;
    private int mSecond = -1;
    private long mRowId = -1;

    private boolean fixedDate = false;
    private boolean fixedDirection = false;
    private boolean fixedTime = false;
    private boolean hasDelete = false;

    private String direction;

    private View fragmentView;
    private Tracker mTracker;

    /**
     * Callbacks called whenever final buttons are pressed
     */
    public static abstract class PickerCallback {
        public abstract void callbackSet(int year, int month, int day, int hour, int minute, String direction);
        public abstract void callbackDelete(int year, int month, int day, int hour, int minute, String direction, long rowId);
    }
    public PickerCallback pickerCallback;

    /**
     * Method to return a standard custom record picker callback.
     * The callback will update a database record (if doesn't exist), or add a new record to the database, on Set.
     * The callback will delete the database record on Delete.
     * @param context Context.
     * @param tracker Tracker.
     * @param onComplete Runnable to run after database change is done.
     * @return Picker callback.
     */
    public static PickerCallback getStandardCustomRecordPickerCallback(final Context context, final Tracker tracker, @Nullable final Runnable onComplete){
        return new PickerCallback() {
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

                        // check time isn't in future
                        if(time.getMillis() > DateTime.now().getMillis()){
                            // notify of error
                            handler.post(new Runnable() {
                                public void run() {

                                    Toast.makeText(
                                            context,
                                            R.string.fail_future_time,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });

                            return;
                        }

                        DateTime centerOfDay = new DateTime(year, month, day, 0, 0).plusHours(12);
                        mUpdateValues.put(TimeEntryDbHandler.COLUMN_TIME, time.getMillis());

                        String mSelectionClause = TimeEntryDbHandler.COLUMN_CENTER_OF_DAY + "=? AND " +
                                TimeEntryDbHandler.COLUMN_DIRECTION + "=? AND " +
                                TimeEntryDbHandler.COLUMN_TYPE + "=?";
                        String[] mSelectionArgs = {String.valueOf(centerOfDay.getMillis()), direction, TimeEntry.TimeEntryType.TIME_RECORD.name()};
                        int mRowsUpdated = 0;

                        mRowsUpdated = context.getContentResolver().update(
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

                            mNewUri = context.getContentResolver().insert(
                                    TimeEntryContentProvider.CONTENT_URI,   // the content URI
                                    mNewValues                          // the values to insert
                            );

                            toastText = context.getString(R.string.custom_record_picker_notify_added);
                        } else {
                            toastText = context.getString(R.string.custom_record_picker_notify_changed);
                        }


                        // Display notification toast on completion
                        handler.post(new Runnable() {
                            public void run() {

                                Toast.makeText(
                                        context,
                                        toastText,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                        // Do any on-completion actions
                        if(onComplete != null){
                            handler.post(onComplete);
                        }
                    }
                }.start();

                tracker.send(new HitBuilders.EventBuilder()
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
                        mRowsDeleted = context.getContentResolver().delete(
                                TimeEntryContentProvider.CONTENT_URI,   // the user dictionary content URI
                                mSelectionClause,                    // the column to select on
                                mSelectionArgs                      // the value to compare to
                        );

                        assert (mRowsDeleted != 1) : "Only one row should have been deleted, total: " + String.valueOf(mRowsDeleted);

                        // Display notification toast on completion
                        handler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(
                                        context,
                                        context.getString(R.string.deleted_sleep_record),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                        // Do any on-completion actions
                        if(onComplete != null){
                            handler.post(onComplete);
                        }

                    }
                }.start();
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_custom_entry, container, false);

        final RadioGroup radioGroup = (RadioGroup) fragmentView.findViewById(R.id.RG1);

        // Get and process arguments
        Bundle args = getArguments();
        if (args != null) {
            if (args.getBoolean(TAG_HAS_DELETE)) {
                // Show and enable functionality of the delete button
                hasDelete = true;
                Button deleteButton = (Button) fragmentView.findViewById(R.id.delete_button);
                RelativeLayout deleteLayout = (RelativeLayout) fragmentView.findViewById(R.id.RL_Original);
                deleteLayout.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(DeleteButtonListener);
            }
            if (args.getBoolean(TAG_IS_FIXED_DIRECTION)) {
                // Disable changing the type
                fixedDirection = true;
                // Disable use of the radiobuttons
                radioGroup.setEnabled(false);
                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    ((RadioButton) radioGroup.getChildAt(i)).setEnabled(false);
                }
            }
            if (args.getBoolean(TAG_IS_FIXED_DATE)) {
                // Disables changing the date, and greys it out
                fixedDate = true;
            }
            if (args.getBoolean(TAG_IS_FIXED_TIME)) {
                // Disables chaning the time, and greys it out
                fixedTime = true;
            }
            if (args.getString(TAG_TITLE) != null) {
                getDialog().setTitle(args.getString(TAG_TITLE));
            } else {
                throw new RuntimeException("No Title passed in argument bundle to CustomEntryPickerFragment");
            }
            if (args.getLong(TAG_ROW_ID) != 0L) {
                mRowId = args.getLong(TAG_ROW_ID);
            }
        }

        // Set the OnClick methods for the clickable widgets
        // The Date textview
        if (!fixedDate) {
            fragmentView.findViewById(R.id.date_textview).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open a date picker dialog, with current-set date as defaults, which returns the values
                    // to the fields of this class
                    CustomDatePickerFragment fragment = new CustomDatePickerFragment();
                    if (mYear != -1 && mMonth != -1 && mDay != -1) {
                        // set default
                        Bundle args = new Bundle();
                        args.putInt(fragment.TAG_DEFAULT_YEAR, mYear);
                        args.putInt(fragment.TAG_DEFAULT_MONTH, mMonth);
                        args.putInt(fragment.TAG_DEFAULT_DAY, mDay);
                        args.putBoolean(fragment.TAG_ARE_DEFAULTS, true);
                        fragment.setArguments(args);

                    }
                    fragment.pickerCallback = DatePickerCallback;
                    fragment.show(getActivity().getSupportFragmentManager(), DATE_PICKER_TAG);
                }
            });
        } else {
            // fade the date textview so user knows he can't click it
            ((TextView) fragmentView.findViewById(R.id.date_textview)).setTextColor(Color.GRAY);
        }
        // The Time textview
        if (!fixedTime) {
            fragmentView.findViewById(R.id.time_textview).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open a time picker dialog, with current-set time as defaults, which returns the values
                    // to the fields of this class
                    CustomTimePickerFragment fragment = new CustomTimePickerFragment();
                    if (mHour != -1 && mMinute != -1) {
                        // set default
                        Bundle args = new Bundle();
                        args.putInt(fragment.TAG_DEFAULT_HOUR, mHour);
                        args.putInt(fragment.TAG_DEFAULT_MINUTE, mMinute);
                        args.putBoolean(fragment.TAG_ARE_DEFAULTS, true);
                        fragment.setArguments(args);
                    }
                    fragment.pickerCallback = TimePickerCallback;
                    fragment.show(getActivity().getSupportFragmentManager(), TIME_PICKER_TAG);
                }
            });
        } else {
            // fade the date textview so user knows he can't click it
            ((TextView) fragmentView.findViewById(R.id.time_textview)).setTextColor(Color.GRAY);
        }
        // The Cancel button
        fragmentView.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        // The Set/OK button
        fragmentView.findViewById(R.id.ok_button).setOnClickListener(SetButtonListener);
        // The direction radiogroup
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.bedtime_radiobutton:
                        direction = TimeEntry.Direction.BEDTIME.name();
                        break;
                    case R.id.wakeup_radiobutton:
                        direction = TimeEntry.Direction.WAKE.name();
                        break;
                    default:
                        throw new RuntimeException("Unexpected radio button");
                }
            }
        });

        return fragmentView;
    }

    // Callbacks
    private CustomTimePickerFragment.PickerCallback TimePickerCallback = new CustomTimePickerFragment.PickerCallback() {
        @Override
        public void callback(int hour, int minute) {
            mHour = hour;
            mMinute = minute;
            updateTimeTextview();
        }
    };
    private CustomDatePickerFragment.PickerCallback DatePickerCallback = new CustomDatePickerFragment.PickerCallback() {
        @Override
        public void callback(int year, int month, int day) {
            mYear = year;
            mMonth = month;
            mDay = day;
            updateDateTextview();
        }
    };

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final RadioGroup radioGroup = (RadioGroup) fragmentView.findViewById(R.id.RG1);

        // Update the textviews with defaults, after they've been created
        Bundle args = getArguments();
        if (args == null)
            return;

        long defaultDate = args.getLong(TAG_DEFAULT_DATE);
        if (defaultDate > 0L) {
            // Store the default date
            DateTime date = new DateTime(defaultDate);
            mYear = date.getYear();
            mMonth = date.getMonthOfYear();
            mDay = date.getDayOfMonth();
            updateDateTextview();

        }
        long defaultTime = args.getLong(TAG_DEFAULT_TIME);
        if (defaultTime > 0L) {
            // Store the default time
            DateTime time = new DateTime(defaultTime);
            mHour = time.getHourOfDay();
            mMinute = time.getMinuteOfHour();
            updateTimeTextview();
        }
        String defaultDirection = args.getString(TAG_DEFAULT_DIRECTION);
        if (defaultDirection != null) {
            // Set the default direction
            direction = defaultDirection;
            if (defaultDirection.equalsIgnoreCase(TimeEntry.Direction.BEDTIME.name())) {
                ((RadioButton)fragmentView.findViewById(R.id.bedtime_radiobutton)).setChecked(true);
            } else if (defaultDirection.equalsIgnoreCase(TimeEntry.Direction.WAKE.name())) {
                ((RadioButton)fragmentView.findViewById(R.id.wakeup_radiobutton)).setChecked(true);
            } else {
                throw new RuntimeException("Impossible default direction passed");
            }
        }

        // If there is a delete button, it must be an existing record being changed
        // Fill in original details
        if (hasDelete) {
            TextView txtOriginal = (TextView)fragmentView.findViewById(R.id.original_record);
            HumanReadableConverter chr = new HumanReadableConverter(getActivity());
            txtOriginal.setText("On " + chr.ConvertDate(defaultDate) + ", " + defaultDirection.toLowerCase() +
                    " at " + chr.RelativeTime(defaultDate, defaultTime, defaultDirection));
        }

        // Restore picker callbacks on any dialog fragment that still exists
        CustomTimePickerFragment timePickerFragment = (CustomTimePickerFragment)(getActivity().getSupportFragmentManager().findFragmentByTag(TIME_PICKER_TAG));
        if(timePickerFragment != null){
            timePickerFragment.pickerCallback = TimePickerCallback;
        }
        CustomDatePickerFragment datePickerFragment = (CustomDatePickerFragment)(getActivity().getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG));
        if(datePickerFragment != null){
            datePickerFragment.pickerCallback = DatePickerCallback;
        }
    }

    /**
     * Method to check all fields filled
     * @return True if all fields filled; false otherwise
     */
    private boolean checkFields() {
        if (direction == null) {
            Toast.makeText(
                    getActivity(),
                    getString(R.string.must_choose_wakeup_or_bedtime),
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }
        if (mYear == -1 || mMonth == -1 || mDay == -1 || mHour == -1 || mMinute == -1) {
            Toast.makeText(
                    getActivity(),
                    getString(R.string.must_choose_date_and_time),
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        return true;
    }

    /**
     * Displays date in readable format
     */
    public void updateDateTextview() {
        TextView dateTextview = (TextView)fragmentView.findViewById(R.id.date_textview);
        dateTextview.setText(new HumanReadableConverter(getActivity()).ConvertDate(mYear, mMonth, mDay));
    }

    /**
     * Displays time in readable format
     */
    public void updateTimeTextview() {
        TextView timeTextview = (TextView)fragmentView.findViewById(R.id.time_textview);
        timeTextview.setText(new HumanReadableConverter(getActivity()).ConvertTime(mHour, mMinute));
    }

    /**
     * Run delete button callback when clicked
     */
    private View.OnClickListener DeleteButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Return the values to the calling activity
            if (pickerCallback == null)
                throw new RuntimeException("No callback passed to CustomEntryPickerFragment");
            pickerCallback.callbackDelete(mYear, mMonth, mDay, mHour, mMinute, direction, mRowId);

            // Dismiss the dialog
            dismiss();
        }
    };

    /**
     * Run set button callback when clicked
     */
    private View.OnClickListener SetButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final RadioGroup radioGroup = (RadioGroup)fragmentView.findViewById(R.id.RG1);

            // Check all fields filled
            if (!checkFields())
                return;

            // Return the values to the calling activity
            if (pickerCallback == null)
                throw new RuntimeException("No callback passed to CustomEntryPickerFragment");
            pickerCallback.callbackSet(mYear, mMonth, mDay, mHour, mMinute, direction);

            // Dismiss the dialog
            dismiss();
        }
    };

}
