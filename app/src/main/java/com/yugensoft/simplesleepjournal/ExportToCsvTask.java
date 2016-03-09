package com.yugensoft.simplesleepjournal;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
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

/**
 * Created by yugensoft on 21/07/2015.
 */
public class ExportToCsvTask extends AsyncTask<Object, Integer, File> {
    private Context mContext;
    private ProgressDialog mDialog;

    public ExportToCsvTask(Context context) {
        mContext = context;
        mDialog =  new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {

        mDialog.setMessage("Exporting data to CSV...");
        mDialog.show();

    }

    @Override
    protected File doInBackground (Object... objs) {

        final SQLiteDatabase db = new TimeEntryDbHandler(mContext).getReadableDatabase();

        // Get current date and time to label the file
        DateTime now = new DateTime();
        DateTimeFormatter fmt1 = DateTimeFormat.forPattern("YYYY-MM-dd_HH-mm-ss");
        String timeAndDate = fmt1.print(now);
        String filename = "sleep_journal_data_export_" + timeAndDate + ".csv";

        // Get a directory to export to
        File exportDir = new File(Environment.getExternalStorageDirectory().getPath(), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File file = new File(exportDir, filename);
        // Pull the contents of the database and write it to the CSV
        try {
            file.createNewFile();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
            Cursor curCSV = db.rawQuery("SELECT * FROM " + TimeEntryDbHandler.TABLE_TIME_ENTRIES, null);
            csvWriter.writeNext(curCSV.getColumnNames(), false);
            // loop through rows
            while (curCSV.moveToNext()) {
                ArrayList<String> al = new ArrayList<String>();
                // loop through columns
                for (int i = 0; i < curCSV.getColumnCount(); i++) {
                    al.add(curCSV.getString(i));
                }
                csvWriter.writeNext(al.toArray(new String[al.size()]), false);

                publishProgress((int)((float) curCSV.getPosition() / curCSV.getCount() * 100));
            }
            csvWriter.close();
            curCSV.close();


        }
        catch (SQLException |IOException e) {
            Toast.makeText(
                    mContext,
                    mContext.getString(R.string.error_generating_csv) + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        // Return the file so it is passed to onPostExecute, so it can then be sent
        return file;

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
