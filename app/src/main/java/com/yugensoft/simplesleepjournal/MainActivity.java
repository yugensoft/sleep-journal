package com.yugensoft.simplesleepjournal;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.yugensoft.simplesleepjournal.contentprovider.TimeEntryContentProvider;
import com.yugensoft.simplesleepjournal.database.TimeEntry;
import com.yugensoft.simplesleepjournal.database.TimeEntryDbHandler;
import com.squareup.picasso.Picasso;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


// General Todos (v1.2):
// todo: fix random "premium monthly" error

// Next Revision Notes:
// Add graphical time bars on current day + add/modify records + in records list
// Add records month splitters
public class MainActivity extends ActionBarActivity {

    // states
    public final int STATE_ASLEEP = 0;
    public final int STATE_AWAKE = 1;
    public final int STATE_UNKNOWN = 2;

    private int state = STATE_UNKNOWN;

    // other constants
    public final int REQUEST_CODE_BUY_AD_REMOVE = 1001;
    public final String PRODUCT_ID_AD_REMOVE = "ad_remove"; //TODO
//    public final String PRODUCT_ID_AD_REMOVE = "android.test.purchased";

    private Tracker mTracker;
    private AdView mAdView;
    private MenuItem mRemoveAds;


    /**
     * This section is related to the in-app billing
     */

    private IInAppBillingService mService;
    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            Log.d("in-app billing", "onServiceConnected ");

            // Check if user owns any in-app billing items and perform associated tasks
            processOwnedAndAvailableItems();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pre-fetch the state images
        Picasso.with(MainActivity.this).load(R.drawable.moon).fetch();
        Picasso.with(MainActivity.this).load(R.drawable.sun).fetch();
        Picasso.with(MainActivity.this).load(R.drawable.unknown).fetch();

        // The ad
        mAdView = (AdView) findViewById(R.id.adView);

        // Obtain the shared Tracker instance.
        SimpleSleepJournalApplication application = (SimpleSleepJournalApplication) getApplication();
        mTracker = application.getDefaultTracker();

        /*
        ** This part is related to in-ap billing
         */

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        //-- In-app billing
    }

    /**
     * Function to check for any owned and available in-app billing items, and perform associated tasks
     * If the ad-remove is owned, removes the option to buy it, and disables the ad
     * Otherwise, gets the price and enables buying menu item
     *
     * Notes:
     * design is that ads will only ever be displayed upon confirmed lack of ad_remove purchase
     */
    private void processOwnedAndAvailableItems() {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {

                final String f_adRemovePrice;
                final boolean f_isAdRemovePurchased;

                // Owned items
                try {
                    boolean isAdRemovePurchased = false;
                    Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
                    if (ownedItems.getInt("RESPONSE_CODE") == 0) {
                        ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                        ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                        ArrayList<String> signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                        String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN"); // Not used, only have single-digit number of items

                        for (int i = 0; i < purchaseDataList.size(); ++i) {
                            String purchaseData = purchaseDataList.get(i);
                            String signature = signatureList.get(i);
                            String sku = ownedSkus.get(i);

                            // do something with this purchase information
                            // e.g. display the updated list of products owned by user
                            Log.d("in-app billing", "owned item: " + purchaseData + ", " + signature + ", " + sku);

                            if(sku.equals(PRODUCT_ID_AD_REMOVE)){
                                isAdRemovePurchased = true;
                            }
                        }
                    }
                    f_isAdRemovePurchased = isAdRemovePurchased;
                } catch(RemoteException e) {
                    // Problem of some sort, abandon and leave ad unused
                    Log.d("in-app billing", "exception");
                    return;
                }

                // Available items
                ArrayList<String> skuList = new ArrayList<String> ();
                skuList.add(PRODUCT_ID_AD_REMOVE);
                Bundle querySkus = new Bundle();
                querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

                try {
                    String adRemovePrice = null;
                    Bundle skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
                    if (skuDetails.getInt("RESPONSE_CODE") == 0) {
                        ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");

                        for (String thisResponse : responseList) {
                            JSONObject object = new JSONObject(thisResponse);
                            String sku = object.getString("productId");
                            String price = object.getString("price");
                            Log.d("in-app billing", "available item: " + sku + ", " + price);

                            if (sku.equals(PRODUCT_ID_AD_REMOVE)) {
                                adRemovePrice = price;
                                Log.d("in-app billing", "adRemove price: " + adRemovePrice);
                                break;
                            }
                        }
                    }
                    f_adRemovePrice = adRemovePrice;
                } catch(RemoteException|JSONException e) {
                    // Problem of some sort, abandon and leave ad unused
                    Log.d("in-app billing", "exception");
                    return;
                }

                // Wait for the options menu to be constructed
                while(mRemoveAds == null){
                    Log.d("in-app billing", "no remove_ads menu item");
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e){
                        Log.d("in-app billing", "thread sleep interrupted ");
                        return;
                    }
                }

                // Process and apply results
                handler.post(new Runnable() {
                    public void run() {
                        // Check if ad_remove is purchased
                        if(!f_isAdRemovePurchased){
                            AdFunctions.loadAdIntoAdView(mAdView);
                            // Check if we have a price for ad_remove
                            if(f_adRemovePrice != null){
                                mRemoveAds.setTitle(getString(R.string.remove_ads) + " (" + f_adRemovePrice + ")");
                                mRemoveAds.setEnabled(true);
                            } else {
                                mRemoveAds.setEnabled(false);
                            }
                        } else {
                            mRemoveAds.setTitle(getString(R.string.remove_ads) + " (" + getString(R.string.purchased) + ")");
                            mRemoveAds.setEnabled(false);
                        }
                    }
                });
            }
        }.start(); //--thread
    }

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

    }

    @Override
    public void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // The remove-ad menu item
        mRemoveAds = menu.findItem(R.id.action_remove_ads);
        return true;
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
            case R.id.action_remove_ads:
                purchaseAdRemove();
                return true;
            // TODO remove after testing
//            case R.id.action_cancel_remove_ads:
//                cancelAdRemove();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * Function that launches the activity to remove the ads via in-app billing
     */
    public void purchaseAdRemove(){
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(
                    3,
                    getPackageName(),
                    PRODUCT_ID_AD_REMOVE,
                    "inapp",
                    "ts6broa23q0gqy3"  // once-off random string as "developer payload"
            );
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            startIntentSenderForResult(
                    pendingIntent.getIntentSender(),
                    REQUEST_CODE_BUY_AD_REMOVE,
                    new Intent(),
                    Integer.valueOf(0),
                    Integer.valueOf(0),
                    Integer.valueOf(0)
            );
        } catch (IntentSender.SendIntentException|RemoteException e){
            return;
        }

    }

    // TODO remove after testing
    public void cancelAdRemove(){
        try {
            int response = mService.consumePurchase(3, getPackageName(), "inapp:" + getPackageName() + ":android.test.purchased");
            if (response == 0) {
                Toast.makeText(MainActivity.this, "Consumed", Toast.LENGTH_SHORT).show();
                processOwnedAndAvailableItems();
            } else {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e){

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BUY_AD_REMOVE) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Remove Ads successfully purchased!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Remove Ads purchase failed.", Toast.LENGTH_LONG).show();
            }
            processOwnedAndAvailableItems();
        }
    }

    public void openAboutDialog(View view) {
        openAboutDialog();
    }

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
        long time = now.getMillis();

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
                        TimeEntryDbHandler.COLUMN_DIRECTION + "=?",
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
                        TimeEntryDbHandler.COLUMN_DIRECTION + "=?",
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
    public void exportData(View view) {
        exportData();
    }

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
        final TextView txtState = (TextView) findViewById(R.id.current_state);
        final ImageView imgState = (ImageView) findViewById(R.id.sleep_state_image);
        final TextView txtLastTime = (TextView) findViewById(R.id.last_time);
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

    @Override
    public void onStop() {
        super.onStop();

    }
}
