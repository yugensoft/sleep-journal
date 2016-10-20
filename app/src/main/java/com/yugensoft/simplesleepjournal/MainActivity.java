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

<<<<<<< HEAD
=======
import com.google.android.gms.ads.AdRequest;
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;
import com.squareup.picasso.Picasso;
import com.yugensoft.simplesleepjournal.util.IabHelper;
import com.yugensoft.simplesleepjournal.util.IabResult;
import com.yugensoft.simplesleepjournal.util.Inventory;
import com.yugensoft.simplesleepjournal.util.Purchase;
import com.yugensoft.simplesleepjournal.util.SkuDetails;

import org.joda.time.DateTime;
<<<<<<< HEAD
=======


// General Todos:
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy

// Next Revision Notes:
// Add graphical time bars on current day + add/modify records + in records list
// Add records month splitters
public class MainActivity extends ActionBarActivity {

    // states
    public final int STATE_ASLEEP = 0;
    public final int STATE_AWAKE = 1;
    public final int STATE_UNKNOWN = 2;

    private int state = STATE_UNKNOWN;

<<<<<<< HEAD
    // other constants
    public final int REQUEST_CODE_BUY_AD_REMOVE = 1001;
//    public final String SKU_AD_REMOVE = "ad_remove";
    public final String SKU_AD_REMOVE = "android.test.purchased";

    // Debug tag, for logging
    static final String TAG = "in-app billing";

    // Tracking and ads
    private Tracker mTracker;
    private AdView mAdView;

    // In-app billing related
    private IabHelper mHelper;
    private MenuItem mRemoveAdsMenuItem;
    private boolean mIsAdRemovePurchased;
    private Purchase mAdRemovePurchase;
    private String mAdRemovePrice;

=======
    private Tracker mTracker;
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy

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
<<<<<<< HEAD

    }

=======
    }
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy

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
<<<<<<< HEAD

=======
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy
    }

    @Override
    public void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
<<<<<<< HEAD
    public void onDestroy() {
        super.onDestroy();

        // in-app billing
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    @Override
=======
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
<<<<<<< HEAD
        // The remove-ad menu item
        mRemoveAdsMenuItem = menu.findItem(R.id.action_remove_ads);

        setupInAppBilling();

        return true;
    }

    /**
     * Start up the in-app billing system
     */
    public void setupInAppBilling(){
        // App public key
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlSr2iT8DA5dsWwd6dBVbrWszGt+vNiYEinMpnkcJs0C79FqcnzGuQcMnZegb50o0smC7bONAHwpTP56YSdZ4rIuPtw2WBpqSzOTtrU+S93OH6A7uIDW1hd8LYuD0b16qbuqE3hHOF9QYw96KnPqK8PPLrat2BEx8VYREuQ+l88J655rH+ipzYrjjNqx8bM/yTTAr1BnBs9OBjJNaQ3Hc6BNFlu7jbTeSBVJ0C/GA49crYwE4ELTaC2SCjAScDe0675211ML8oyd3jQmswtlx6RKGbC6/cOCo/c3ge7a2mz59kMI9Ec/D9OowR2LNbz1ERnoXyyolBv4wYu3B0zgxawIDAQAB";

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false). todo
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });

    }
    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }


            Log.d(TAG, "Query inventory was successful: " + result.getMessage());

            // Do we have the remove-ads upgrade?
            Purchase adRemove = inventory.getPurchase(SKU_AD_REMOVE);
            mIsAdRemovePurchased = (adRemove != null && verifyDeveloperPayload(adRemove));
            Log.d(TAG, "User has " + (mIsAdRemovePurchased ? "NO ADS" : "ADS"));
            if(mIsAdRemovePurchased) {
                mAdRemovePurchase = adRemove;
            } else {
                // If we don't, get the price
                SkuDetails adRemoveDetails = inventory.getSkuDetails(SKU_AD_REMOVE);
                if (adRemoveDetails != null) {
                    mAdRemovePrice = adRemoveDetails.getPrice();
                    Log.d(TAG, "Price is: " + mAdRemovePrice);
                } else {
                    complain("Couldn't get ad-remove price: " + inventory.mSkuMap.toString());

                }
            }

            updateUi();
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };
    public void updateUi(){
        if(!mIsAdRemovePurchased){
            AdFunctions.loadAdIntoAdView(mAdView);
            // Check if we have a price for ad_remove
            if(mAdRemovePrice != null){
                mRemoveAdsMenuItem.setTitle(getString(R.string.remove_ads) + " (" + mAdRemovePrice + ")");
                mRemoveAdsMenuItem.setEnabled(true);
            } else {
                mRemoveAdsMenuItem.setEnabled(false);
            }
        } else {
            mRemoveAdsMenuItem.setTitle(getString(R.string.remove_ads) + " (" + getString(R.string.purchased) + ")");
            mRemoveAdsMenuItem.setEnabled(false);
        }
    }
    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        //todo verify

=======
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy
        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_AD_REMOVE)) {
                // bought the ad-remove upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                alert("Ad Remove Purchased!!");
                mIsAdRemovePurchased = true;
                mAdRemovePurchase = purchase;
                updateUi();
            }
        }
    };


    public void complain(String s){
        Log.e(TAG, s);
    }
    public void alert(String s){
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
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
<<<<<<< HEAD
            case R.id.action_remove_ads:
                purchaseAdRemove();
                return true;
            // TODO remove after testing
            case R.id.action_cancel_remove_ads:
                cancelAdRemove();
                return true;
=======
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy
            default:
                return super.onOptionsItemSelected(item);
        }

    }
<<<<<<< HEAD

    /**
     * Function that starts a purchase to remove the ads via in-app billing
     */
    public void purchaseAdRemove(){
        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");

        String payload = "";

        try {
            mHelper.launchPurchaseFlow(this, SKU_AD_REMOVE, REQUEST_CODE_BUY_AD_REMOVE, mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
    }

    public void cancelAdRemove(){
        if(!mIsAdRemovePurchased) return;

        // Consume the ad-remove, for testing purposes
        Log.d(TAG, "Consuming ad remove.");
        try {
            mHelper.consumeAsync(mAdRemovePurchase, mConsumeFinishedListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error consuming ad remove. Another async operation in progress.");
        }
        return;
    }

    // TODO remove after testing
    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item
                Log.d(TAG, "Consumption successful. ");
            }
            else {
                complain("Error while consuming: " + result);
            }
            updateUi();
            Log.d(TAG, "End consumption flow.");
        }
    };

    public void openAboutDialog(View view) {
        openAboutDialog();
    }

=======
    public void openAboutDialog(View view) {openAboutDialog();}
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy
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

<<<<<<< HEAD
    @Override
    public void onStop() {
        super.onStop();

    }
=======
>>>>>>> parent of e4647c7... manual in-app billing add, which is buggy
}
