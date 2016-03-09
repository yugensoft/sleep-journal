package com.yugensoft.simplesleepjournal.contentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created on 19/06/2015.
 */
public class TimeEntryContentProvider extends ContentProvider {

    // database
    private TimeEntryDbHandler dbHandler;

    // used for the URI
    private static final int TIME_ENTRIES = 10;
    private static final int TIME_ENTRY_ID = 20;

    private static final String AUTHORITY = "com.yugensoft.simplesleepjournal.time_entries.contentprovider";

    private static final String BASE_PATH = "time_entries";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/time_entries";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/time_entry";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, TIME_ENTRIES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", TIME_ENTRY_ID);
    }

    @Override
    public boolean onCreate() {
        dbHandler = new TimeEntryDbHandler(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(TimeEntryDbHandler.TABLE_TIME_ENTRIES);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case TIME_ENTRIES:
                break;
            case TIME_ENTRY_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(TimeEntryDbHandler._ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = dbHandler.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHandler.getWritableDatabase();
        int rowsDeleted = 0;
        long id = 0;
        switch (uriType) {
            case TIME_ENTRIES:
                id = sqlDB.insert(TimeEntryDbHandler.TABLE_TIME_ENTRIES, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHandler.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
            case TIME_ENTRIES:
                rowsDeleted = sqlDB.delete(TimeEntryDbHandler.TABLE_TIME_ENTRIES, selection,
                        selectionArgs);
                break;
            case TIME_ENTRY_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(TimeEntryDbHandler.TABLE_TIME_ENTRIES,
                            TimeEntryDbHandler._ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(TimeEntryDbHandler.TABLE_TIME_ENTRIES,
                            TimeEntryDbHandler._ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHandler.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case TIME_ENTRIES:
                rowsUpdated = sqlDB.update(TimeEntryDbHandler.TABLE_TIME_ENTRIES,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TIME_ENTRY_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(TimeEntryDbHandler.TABLE_TIME_ENTRIES,
                            values,
                            TimeEntryDbHandler._ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(TimeEntryDbHandler.TABLE_TIME_ENTRIES,
                            values,
                            TimeEntryDbHandler._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                TimeEntryDbHandler._ID,
                TimeEntryDbHandler.COLUMN_TIME,
                TimeEntryDbHandler.COLUMN_CENTER_OF_DAY,
                TimeEntryDbHandler.COLUMN_DIRECTION,
                TimeEntryDbHandler.COLUMN_TYPE,
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }


}

