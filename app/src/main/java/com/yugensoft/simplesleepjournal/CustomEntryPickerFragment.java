package com.yugensoft.simplesleepjournal;

import android.graphics.Color;
import android.os.Bundle;
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
import com.yugensoft.simplesleepjournal.database.TimeEntry;

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


    public static abstract class PickerCallback {
        public abstract void callbackSet(int year, int month, int day, int hour, int minute, String direction);
        public abstract void callbackDelete(int year, int month, int day, int hour, int minute, String direction, long rowId);
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
                    fragment.pickerCallback = new CustomDatePickerFragment.PickerCallback() {
                        @Override
                        public void callback(int year, int month, int day) {
                            mYear = year;
                            mMonth = month;
                            mDay = day;
                            updateDateTextview();
                        }
                    };
                    fragment.show(getActivity().getSupportFragmentManager(), "customDatePicker");
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
                    fragment.pickerCallback = new CustomTimePickerFragment.PickerCallback() {
                        @Override
                        public void callback(int hour, int minute) {
                            mHour = hour;
                            mMinute = minute;
                            updateTimeTextview();
                        }
                    };
                    fragment.show(getActivity().getSupportFragmentManager(), "customDatePicker");
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
    }


    private boolean checkFields() {
        // Check all fields filled
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

    public PickerCallback pickerCallback;

    public void updateDateTextview() {
        TextView dateTextview = (TextView)fragmentView.findViewById(R.id.date_textview);
        dateTextview.setText(new HumanReadableConverter(getActivity()).ConvertDate(mYear, mMonth, mDay));
    }

    public void updateTimeTextview() {
        TextView timeTextview = (TextView)fragmentView.findViewById(R.id.time_textview);
        timeTextview.setText(new HumanReadableConverter(getActivity()).ConvertTime(mHour, mMinute));
    }

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
