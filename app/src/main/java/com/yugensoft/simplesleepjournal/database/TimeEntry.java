package com.yugensoft.simplesleepjournal.database;

/**
 * Created on 23/05/2015.
 */
public class TimeEntry {
    public enum TimeEntryType {
        TIME_TARGET,
        TIME_RECORD,
        MONDAY_DEFAULT,
        TUESDAY_DEFAULT,
        WEDNESDAY_DEFAULT,
        THURSDAY_DEFAULT,
        FRIDAY_DEFAULT,
        SATURDAY_DEFAULT,
        SUNDAY_DEFAULT
    }

    public enum Direction {
        WAKE,
        BEDTIME
    }

    private int _id;
    private long _centerOfDay; // Days are market by their noon. A day's sleep time may occur on the following day.
    private long _time; // The time in milliseconds since epoc
    private TimeEntryType _timeEntryType; // Record, target, day default etc
    private Direction _direction; // Waking up or going to bed

    // Constructor with id
    public TimeEntry(int id, long centerOfDay, long time, TimeEntryType timeEntryType, Direction direction) {
        this._id = id;
        this._centerOfDay = centerOfDay;
        this._time = time;
        this._timeEntryType = timeEntryType;
        this._direction = direction;

    }

    // Constructor without id
    public TimeEntry(long centerOfDay, long time, TimeEntryType timeEntryType, Direction direction) {
        this._centerOfDay = centerOfDay;
        this._time = time;
        this._timeEntryType = timeEntryType;
        this._direction = direction;

    }

    // Empty constructor
    public TimeEntry() {}

    // Setters and getters

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public long get_centerOfDay() {
        return _centerOfDay;
    }

    public void set_centerOfDay(long _centerOfDay) {
        this._centerOfDay = _centerOfDay;
    }

    public long get_time() {
        return _time;
    }

    public void set_time(long _time) {
        this._time = _time;
    }

    public TimeEntryType get_timeEntryType() {
        return _timeEntryType;
    }

    public void set_timeEntryType(TimeEntryType _timeEntryType) {
        this._timeEntryType = _timeEntryType;
    }

    public Direction get_direction() {
        return _direction;
    }

    public void set_direction(Direction _direction) {
        this._direction = _direction;
    }


}

