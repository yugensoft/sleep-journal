package com.yugensoft.simplesleepjournal;

import android.content.Context;

import com.yugensoft.simplesleepjournal.database.TimeEntry;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.concurrent.TimeUnit;

/**
 * Created by yugensoft on 31/07/2015.
 */
public class HumanReadableConverter {
    private Context mContext;

    public HumanReadableConverter(Context context) {
        mContext = context;
    }

    // Method to convert a Duration centered on noon to a human readable time
    public String DurationFromNoon(long duration) {
        DateTime dummyDate = new DateTime(2000,1,1,0,0);

        org.joda.time.Duration d = new org.joda.time.Duration(duration);
        Duration dNormal = d.plus(Period.hours(12).toStandardDuration()); // Normalize, duration is centered on noon
        DateTime dateTime = dummyDate.withDurationAdded(dNormal, 1);

        DateTimeFormatter fmt2;
        if (android.text.format.DateFormat.is24HourFormat(mContext)) {
            fmt2 = DateTimeFormat.forPattern("HH:mm");
        } else {
            fmt2 = DateTimeFormat.forPattern("hh:mm a");
        }
        String strTime = fmt2.print(dateTime);

        if (d.isLongerThan(Period.hours(12).toStandardDuration())) {
            // Sleep time occurs on following day
            strTime += " n.d.";
        }

        return strTime;
    }

    // Methods to convert time fields into the correct reading
    public String ConvertTime(int hours, int minutes) {
        DateTime dummyDate = new DateTime(2000,1,1,hours,minutes);
        DateTimeFormatter fmt;
        if (android.text.format.DateFormat.is24HourFormat(mContext)) {
            fmt = DateTimeFormat.forPattern("HH:mm");
        } else {
            fmt = DateTimeFormat.forPattern("hh:mm a");
        }
        return fmt.print(dummyDate);

    }
    public String ConvertTime(int hours, int minutes, int seconds) {
        DateTime dummyDate = new DateTime(2000,1,1,hours,minutes,seconds); // Using mock y/m/d
        DateTimeFormatter fmt;
        if (android.text.format.DateFormat.is24HourFormat(mContext)) {
            fmt = DateTimeFormat.forPattern("HH:mm:ss");
        } else {
            fmt = DateTimeFormat.forPattern("hh:mm:ss a");
        }
        return fmt.print(dummyDate);

    }
    public String MillisecondsToStandard(long millis, boolean plusSign) {
        long millisAbs = Math.abs(millis);
        String s = String.format("%d:%02d",
                TimeUnit.MILLISECONDS.toHours(millisAbs),
                TimeUnit.MILLISECONDS.toMinutes(millisAbs) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisAbs))
        );
        if (millis < 0) {
            s = "-" + s;
        } else if (plusSign) {
            s = "+" + s;
        }

        return s;
    }

    // Method to return the time of a time entry in human readable form,
    // relative to the given centerOfDay
    public String RelativeTime(long centerOfDay, long time, String direction) {
        Duration difference = new Duration(time - centerOfDay);
        DateTimeFormatter fmt;
        if (android.text.format.DateFormat.is24HourFormat(mContext)) {
            fmt = DateTimeFormat.forPattern("HH:mm");
        } else {
            fmt = DateTimeFormat.forPattern("hh:mm a");
        }

        if (direction.equalsIgnoreCase(TimeEntry.Direction.WAKE.name())) {
            return fmt.print(time);
        } else if (direction.equalsIgnoreCase(TimeEntry.Direction.BEDTIME.name())) {
            if (difference.isLongerThan(Duration.standardHours(12))) {
                // after midnight, so affix "n.d." for "next day"
                return fmt.print(time) + " n.d.";
            } else {
                return fmt.print(time);
            }
        } else {
            throw new IllegalArgumentException("Impossible Direction argument supplied");
        }

    }

    // Method to return human readable date from a 'milliseconds from epoch' long time, in the desired format
    public String ConvertDate(long time) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/YY");
        return fmt.print(time);
    }

    // Method to return human readable date from given year-month-day
    public String ConvertDate(int year, int month, int day) {
        DateTime date = new DateTime(year, month, day, 0,0); // time not used
        return DateTimeFormat.forPattern("dd/MM/YY").print(date);
    }

}
