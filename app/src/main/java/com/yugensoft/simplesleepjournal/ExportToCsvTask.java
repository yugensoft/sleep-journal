package com.yugensoft.simplesleepjournal;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;
import com.opencsv.CSVWriter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.database.Cursor.FIELD_TYPE_BLOB;
import static android.database.Cursor.FIELD_TYPE_FLOAT;
import static android.database.Cursor.FIELD_TYPE_INTEGER;
import static android.database.Cursor.FIELD_TYPE_NULL;
import static android.database.Cursor.FIELD_TYPE_STRING;

/**
 */
public class ExportToCsvTask extends AsyncTask<Object, Integer, File> {
    private static final String TAG = "export_csv";

    private Context mContext;
    private ProgressDialog mDialog;

    public ExportToCsvTask(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        mDialog =  new ProgressDialog(mContext);
        mDialog.setMessage("Exporting data to CSV...");
        mDialog.show();

    }

    @Override
    protected File doInBackground (Object... objs) {

        final SQLiteDatabase db = new TimeEntryDbHandler(mContext).getReadableDatabase();

        // Get current date and time to label the file
        final DateTime now = new DateTime();
        final DateTimeFormatter fmt1 = DateTimeFormat.forPattern("YYYY-MM-dd_HH-mm-ss");
        final String timeAndDate = fmt1.print(now);
        final String filename = "sleep_journal_data_export_" + timeAndDate + ".csv";

        // Get a directory to export to
        final String exportDirPath = mContext.getExternalCacheDir().getPath();
        if (MainActivity.DEBUG_LOGS) Log.d(TAG, "export dir path: " + exportDirPath);
        final File exportDir = new File(exportDirPath, "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        final File file = new File(exportDir, filename);
        // Pull the contents of the database and write it to the CSV
        try {
            file.createNewFile();
            final CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
            final Cursor curCSV = db.rawQuery("SELECT * FROM " + TimeEntryDbHandler.TABLE_TIME_ENTRIES, null);
            csvWriter.writeNext(curCSV.getColumnNames(), false);
            // loop through rows
            while (curCSV.moveToNext()) {
                writeCursorToCsv(curCSV, csvWriter, mContext);
                publishProgress((int)((float) curCSV.getPosition() / curCSV.getCount() * 100));
            }
            csvWriter.close();
            curCSV.close();

        }
        catch (SQLException |IOException e) {
            if (MainActivity.DEBUG_LOGS) Log.d(TAG, e.getMessage());

            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            mContext,
                            mContext.getString(R.string.error_generating_csv) + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }

        // Return the file so it is passed to onPostExecute, so it can then be sent
        return file;

    }

    /**
     * Writes a cursor out to csv in a human readable format
     * @param cursor
     * @param csvWriter
     */
    private static void writeCursorToCsv(Cursor cursor, CSVWriter csvWriter, Context context) {
        final ArrayList<String> list = new ArrayList<String>();
        final String[] columnNames = cursor.getColumnNames();

        final HashMap<String,Object> map = CursorToMap(cursor);
        final HumanReadableConverter converter = new HumanReadableConverter(context);

        // loop through columns
        for (String columnName : columnNames) {
            switch (columnName) {
                case TimeEntryDbHandler.COLUMN_CENTER_OF_DAY:
                    list.add(converter.ConvertDate((Long)map.get(columnName)));
                    break;
                case TimeEntryDbHandler.COLUMN_TIME:
                    list.add(converter.RelativeTime(
                            (Long) map.get(TimeEntryDbHandler.COLUMN_CENTER_OF_DAY),
                            (Long) map.get(TimeEntryDbHandler.COLUMN_TIME),
                            (String) map.get(TimeEntryDbHandler.COLUMN_DIRECTION)
                    ));
                    break;
                default:
                    list.add(map.get(columnName).toString());
            }
        }
        if (MainActivity.DEBUG_LOGS) Log.d(TAG, "writeCursorToCsv: " + list.toString());
        csvWriter.writeNext(list.toArray(new String[list.size()]), false);
    }

    /**
     * Convert a cursor to a map of column names -> column values at its current position
     * @param cursor
     * @return
     */
    private static HashMap<String,Object> CursorToMap(Cursor cursor){
        HashMap<String, Object> map = new HashMap<>();

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            switch(cursor.getType(i)){
                case	FIELD_TYPE_BLOB:
                    map.put(cursor.getColumnName(i), cursor.getBlob(i));
                    break;
                case	FIELD_TYPE_FLOAT:
                    map.put(cursor.getColumnName(i), cursor.getFloat(i));
                    break;
                case	FIELD_TYPE_INTEGER:
                    map.put(cursor.getColumnName(i), cursor.getLong(i));
                    break;
                case	FIELD_TYPE_NULL:
                    map.put(cursor.getColumnName(i), "NULL");
                    break;
                case	FIELD_TYPE_STRING:
                    map.put(cursor.getColumnName(i), cursor.getString(i));
                    break;
            }
        }
        return map;
    }

    protected void onProgressUpdate (Integer... progress) {
        mDialog.setMessage("Exporting data to CSV... " + String.valueOf(progress[0]) + "%");
    }

    protected void onPostExecute (File file) {
        if (mDialog.isShowing()){
            mDialog.dismiss();
        }

        if (file.length() == 0) {
            // Failed to generate a file
            return;
        }

        // Share the file
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        intentShareFile.setType("application/csv");

        intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
        intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");

        mContext.startActivity(Intent.createChooser(intentShareFile, "Share File"));
    }
}
