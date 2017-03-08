package com.yugensoft.simplesleepjournal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.yugensoft.simplesleepjournal.customviews.DayRecordsDisplayBar;
import com.yugensoft.simplesleepjournal.database.TimeEntry;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public class TimeEntryDisplayCursorAdapter extends BaseAdapter {
    private Context mContext;
    // stores entry data for each day
    private LinkedHashMap<Long, ArrayList<TimeEntry>> mEntries;
    // records number of spaces (in case of no entries)
    private LinkedHashMap<Long, Long> mSpaces;
    // connects ListView index to day entries CenterOfDay
    private ArrayList<Long> mDayCenters;
    // stores default targets
    private LinkedHashMap<String, ArrayList<TimeEntry>> mTargets;
    // used to scale the display bars
    private float mScale = 1.0f;

    public TimeEntryDisplayCursorAdapter(Context context) {
        mContext = context;
        mEntries = new LinkedHashMap<>();
    }

    /**
     * Repopulate data and regenerate day spaces etc
     * @param entries List of time entry records.
     */
    public void repopulateData(ArrayList<TimeEntry> entries) {
        // start afresh
        mEntries.clear();

        // group together entries with the same Day value
        for (TimeEntry timeEntry : entries){
            long day = timeEntry.get_centerOfDay();
            if(!mEntries.containsKey(day)) {
                ArrayList<TimeEntry> timeEntryGroup = new ArrayList<TimeEntry>();
                timeEntryGroup.add(timeEntry);
                mEntries.put(day, timeEntryGroup);
            } else {
                mEntries.get(day).add(timeEntry);
            }
        }

        // get list of center-of-day for access purposes
        ArrayList<Long> dayCenters = new ArrayList<>(mEntries.keySet());
        Collections.sort(dayCenters);

        // fill any spaces in the list
        ArrayList<Long> dayCentersFilled = new ArrayList<>();
        LinkedHashMap<Long, Long> spaces = new LinkedHashMap<>();
        Iterator<Long> iterator = dayCenters.iterator();
        long oneDay = 24*60*60*1000;
        Long dayA = null;
        Long dayB = null;
        while(iterator.hasNext()){
            // add the day to the list
            dayB = iterator.next();
            dayCentersFilled.add(dayB);
            if(dayA != null){
                long diff = dayB-dayA;
                long daySpace = diff/oneDay;
                if(daySpace > 1 && daySpace <= 3){
                    for(int i = 1; i < daySpace; i++){
                        Long newDay = dayA + i*oneDay;
                        dayCentersFilled.add(newDay);
                        mEntries.put(newDay,new ArrayList<TimeEntry>()); // blank entries
                        spaces.put(newDay,1L);
                    }
                } else if(daySpace > 3){
                    Long newDay = dayA + oneDay;
                    dayCentersFilled.add(newDay);
                    mEntries.put(newDay,new ArrayList<TimeEntry>()); // blank entries
                    spaces.put(newDay, daySpace - 1);
                } else {
                    // nothing to do
                }
            }
            dayA = dayB;
        }
        Collections.sort(dayCentersFilled,Collections.<Long>reverseOrder());

        mDayCenters = dayCentersFilled;
        mSpaces = spaces;

        notifyDataSetChanged();
    }

    /**
     * Repopulates the day targets
     * @param targets List of default daily targets
     */
    public void repopulateTargets(ArrayList<TimeEntry> targets) {
        mTargets = new LinkedHashMap<>();
        for (TimeEntry target : targets){
            switch (target.get_timeEntryType()){
                case TIME_TARGET:
                    // not currently supported
                    break;
                case MONDAY_DEFAULT:
                case TUESDAY_DEFAULT:
                case WEDNESDAY_DEFAULT:
                case THURSDAY_DEFAULT:
                case FRIDAY_DEFAULT:
                case SATURDAY_DEFAULT:
                case SUNDAY_DEFAULT:
                    // add it to the map
                    String key = target.get_timeEntryType().toString().substring(0,2).toLowerCase();
                    ArrayList<TimeEntry> element = mTargets.get(key);
                    if(element == null) {
                        element = new ArrayList<>();
                    }
                    element.add(target);
                    mTargets.put(key,element);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Set scale
     * @param scale Scale.
     */
    public void setScale(float scale) {
        this.mScale = scale;
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public ArrayList<TimeEntry> getItem(int position) {
        return mEntries.get(mDayCenters.get(position));
    }

    @Override
    public long getItemId(int position) {
        return mDayCenters.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View result;
        Context context = parent.getContext();

        if (convertView == null) {
            result  = LayoutInflater.from(context).inflate(R.layout.time_entry_day_visual_listview_row,parent,false);
        } else {
            result = convertView;
        }

        TextView textView = (TextView)result.findViewById(R.id.date_textview);
        DayRecordsDisplayBar displayBar = (DayRecordsDisplayBar)result.findViewById(R.id.display_bar);

        // get the day entry data
        ArrayList<TimeEntry> item = getItem(position);

        // populate the views
        long centerOfDay = getItemId(position);
        textView.setText(new HumanReadableConverter(context).ConvertDate(centerOfDay,"dd-MMM"));
        displayBar.setRenderScale(mScale);
        if(item.size() == 0) {
            displayBar.setBlank(mSpaces.get(centerOfDay));
        } else {
            displayBar.setData(item);
            displayBar.setTargets(getTargetsForDay(centerOfDay));
        }

        return result;
    }

    /**
     * Determines the default targets for a given day.
     * @param centerOfDay The center of day (milliseconds).
     * @return The targets; a list of 0 to 2 time entries.
     */
    private ArrayList<TimeEntry> getTargetsForDay(long centerOfDay) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("E");
        String dayKey =  fmt.print(centerOfDay).substring(0,2).toLowerCase();
        return mTargets != null ? mTargets.get(dayKey) : new ArrayList<TimeEntry>();
    }

}
