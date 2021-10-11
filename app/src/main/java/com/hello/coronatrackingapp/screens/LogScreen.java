package com.hello.coronatrackingapp.screens;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.hello.coronatrackingapp.R;
import com.hello.coronatrackingapp.asyncoperations.ClientThread;
import com.hello.coronatrackingapp.asyncoperations.RiskEvaluationThread;
import com.hello.coronatrackingapp.database.DatabaseHandler;
import com.hello.coronatrackingapp.blecommunication.TrackingHandler;
import com.hello.coronatrackingapp.database.TestDatabaseHandler;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * This class is the activity that lets the developer (or other interested people) play
 * around with features of the app and test its behaviour. All possible operations are
 * listed in the field 'entries'. More detailed information will be provided in the
 * javadoc description of each single operation. Most of them will log a lot of information
 * that helps to understand the inner workings of the app and to debug its components.
 */
public class LogScreen extends AppCompatActivity {
    private static final String TAG = "at LogScreen";

    private TrackingHandler trackingHandler;

    BroadcastReceiver broadcastReceiverEvaluationDone;
    DatabaseHandler handler;
    TestDatabaseHandler testHandler;

    SharedPreferences preferences;

    Context context = this;
    // button descriptions
    public static final String[] entries = {"G: Start Proximity Recording", "G: Stop Proximity Recording", "B: Advertise", "B: Stop Advertising",
            "B: Scan", "B: Stop Scanning", "B: Start Peripheral Mode (GATT Server)", "B: Stop Peripheral Mode (GATT Server)",
            "S: Test Report Infection", "S: Test Download Keys","D: Number of Database entries", "D: Print Database", "D: Clear Database",
            "D: Log timestamp in test database", "D: Print Test Database", "D: Clear Test Database", "D: Delete two weeks old entries",
            "E: encrypt timestamp/UUID", "E: risk evaluation"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_screen);

        // making the content scrollable by using a ListView
        ListView listView = findViewById(R.id.listview);
        ArrayList<String> list = new ArrayList<>(Arrays.asList(entries));

        ArrayAdapter adapter = new OperationsListAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);

        // setting behaviour for all ListView entries
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        onStartRecording();
                        break;
                    case 1:
                        onStopRecording();
                        break;
                    case 2:
                        onAdvertise();
                        break;
                    case 3:
                        onStopAdvertising();
                        break;
                    case 4:
                        onStartScanning();
                        break;
                    case 5:
                        onStopScanning();
                        break;
                    case 6:
                        onStartPeripheralMode();
                        break;
                    case 7:
                        onStopPeripheralMode();
                        break;
                    case 8:
                        onTestReportInfection();
                        break;
                    case 9:
                        onTestDownloadKeys();
                        break;
                    case 10:
                        onNumberOfEntries();
                        break;
                    case 11:
                        onPrintDatabase();
                        break;
                    case 12:
                        onClearDatabase();
                        break;
                    case 13:
                        onLogTestValue();
                        break;
                    case 14:
                        onPrintTestDatabase();
                        break;
                    case 15:
                        onClearTestDatabase();
                        break;
                    case 16:
                        onDeleteOldEntries();
                        break;
                    case 17:
                        onEncryptTimestampAndUUID();
                        break;
                    case 18:
                        onRiskEvaluation();
                        break;
                }
            }
        });

        // setting the users UUID (only necessary when used without the StartScreen)
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains("ID")) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("ID", UUID.randomUUID().toString());
            editor.apply();
        }

        handler = new DatabaseHandler(this);
        testHandler = new TestDatabaseHandler(this);

        trackingHandler = new TrackingHandler(this, handler);


        ActivityCompat.requestPermissions(LogScreen.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1);

    }

    /**
     * @see TrackingHandler starts the proximity tracking
     * same as starting it from the StartScreen (but with its own instance of the TrackingHandler)
     */
    private void onStartRecording() {
        Toast.makeText(this, "recording started", Toast.LENGTH_SHORT).show();
        trackingHandler.startTracking(this);
    }

    /**
     * @see TrackingHandler stops the proximity tracking
     */
    private void onStopRecording() {
        Toast.makeText(this, "recording stopped", Toast.LENGTH_SHORT).show();
        trackingHandler.stopTracking();
    }

    /**
     * starting only the advertising part of the tracking, making the device a beacon
     * @see TrackingHandler
     */
    private void onAdvertise() {
        Toast.makeText(this, "advertising started", Toast.LENGTH_SHORT).show();
        trackingHandler.startAdvertising();
    }

    /**
     * stop just the advertising.
     * @see TrackingHandler
     */
    private void onStopAdvertising() {
        Toast.makeText(this, "advertising stopped", Toast.LENGTH_SHORT).show();
        trackingHandler.stopAdvertising();
    }

    /**
     * starting only the scanning part of the tracking, which also sets up the BluetoothGATT.
     * @see TrackingHandler
     */
    private void onStartScanning() {
        Toast.makeText(this, "scanner started", Toast.LENGTH_SHORT).show();
        trackingHandler.startScanning();
    }

    /**
     * stop just the scanning.
     * @see TrackingHandler
     */
    private void onStopScanning() {
        Toast.makeText(this, "scanner stopped", Toast.LENGTH_SHORT).show();
        trackingHandler.stopScanning();
    }

    /**
     * only start the GATT server and initialize its service.
     * @see TrackingHandler
     */
    private void onStartPeripheralMode() {
        Toast.makeText(this, "GATT server started", Toast.LENGTH_SHORT).show();
        trackingHandler.startPeripheral();
    }

    /**
     * stop only GATT server.
     * @see TrackingHandler
     */
    private void onStopPeripheralMode() {
        Toast.makeText(this, "GATT server stopped", Toast.LENGTH_SHORT).show();
        trackingHandler.stopPeripheral();
    }

    /**
     * test the feature report infection.
     * @see ClientThread will upload the apps UUID, after this method displays a dialog
     * that lets the user enter the password.
     */
    private void onTestReportInfection() {
        final EditText password = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Confirm infection")
                .setMessage("Please enter the key you were given upon infection")
                .setCancelable(true)
                .setView(password)
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i(TAG, "sending infected information with password to server.");
                        String msg = password.getText().toString() + preferences.getString("ID", "default") + "\n";
                        new ClientThread(msg).start();
                        Toast.makeText(context, "key uploaded", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    /**
     * @see DatabaseHandler will fetch the whole log of recorded contacts
     * (pairs of timestamps and checksums).
     */
    private void onTestDownloadKeys() {
        Toast.makeText(this, "keys downloaded", Toast.LENGTH_SHORT).show();
        Thread clientThread = new ClientThread("requestlist");
        clientThread.start();
    }

    /**
     * @see DatabaseHandler will fetch total number of contact entries and
     * display it in a Toast message.
     */
    private void onNumberOfEntries() {
        Toast.makeText(this, "number of entries: " + handler.getCount(), Toast.LENGTH_SHORT).show();
    }

    /**
     * @see DatabaseHandler will fetch all database entries and log the timestamps in
     * a way that can easily be understood (as a date like for example Thu Jun 11 17:45:06 GMT+02:00 2020)
     * as well as in milliseconds and the checksum.
     */
    private void onPrintDatabase() {
        Toast.makeText(this, "printed database to log", Toast.LENGTH_SHORT).show();
        ArrayList<String> timestamps = handler.getLogColumn(DatabaseHandler.TIMESTAMPS);
        ArrayList<String> checksums = handler.getLogColumn(DatabaseHandler.CHECKSUMS);
        for (int i = 0; i < timestamps.size(); i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(Long.parseLong(timestamps.get(i))));
            String dateString = calendar.getTime().toString();

            Log.i(i + ".", timestamps.get(i) + " = " + dateString);
            Log.i(i + ".", checksums.get(i));
        }
    }

    /**
     * @see DatabaseHandler will delete all entries from the database, which comes
     * in handy, when debugging.
     */
    private void onClearDatabase() {
        Toast.makeText(this, "database cleared", Toast.LENGTH_SHORT).show();
        handler.clearDatabase();
    }

    /**
     * lets the user log a test value into the database, asking for a number of
     * days the timestamp is supposed to lie in the past.
     * @see TestDatabaseHandler will log that entry.
     */
    private void onLogTestValue() {
        final EditText timestampEdit = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Log Test Timestamp")
                .setMessage("Specify number of days before today (for example enter 5 for a timestamp from 5 days ago):")
                .setCancelable(true)
                .setView(timestampEdit)
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String timestamp = timestampEdit.getText().toString();
                        long date = testHandler.logTestValue(Integer.parseInt(timestamp));
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(new Date(date));
                        String dateString = calendar.getTime().toString();
                        Log.i(TAG, "date logged: " + dateString);
                        Toast.makeText(context, "date logged: " + dateString, Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    /**
     * @see TestDatabaseHandler will deliver all entries from the test database, which
     * are then logged from here.
     */
    private void onPrintTestDatabase() {
        Toast.makeText(this, "printed test database to log", Toast.LENGTH_SHORT).show();
        ArrayList<String> timestamps = testHandler.getLogColumnTest(DatabaseHandler.TIMESTAMPS);
        ArrayList<String> checksums = testHandler.getLogColumnTest(DatabaseHandler.CHECKSUMS);
        for (int i = 0; i < timestamps.size(); i++) {
            Log.i(i + ".", timestamps.get(i));

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(Long.parseLong(timestamps.get(i))));
            Log.i(i + ".", calendar.getTime().toString());

            Log.i(i + ".", checksums.get(i));
        }
    }

    /**
     * @see TestDatabaseHandler will delete all entries from the test database.
     */
    private void onClearTestDatabase() {
        Toast.makeText(this, "test database cleared", Toast.LENGTH_SHORT).show();
        testHandler.clearTestDatabase();
    }

    /**
     * @see TestDatabaseHandler will delete entries from the test database that
     * are older than two weeks. This behaviour is also coded into the main part
     * of the app, where this operation is executed once a day. It can be tested here
     * by combining this operation with 'logTestValue'.
     */
    private void onDeleteOldEntries() {
        Toast.makeText(this, "logs dated more than two weeks ago deleted", Toast.LENGTH_SHORT).show();
        testHandler.deleteOldTestEntries();
    }

    /**
     * makes a checksum from an example timestamp / UUID pair, to see
     * the encryption in action.
     */
    private void onEncryptTimestampAndUUID() {
        Toast.makeText(this, "encrypting timestamp 2020.06.06.11.18.11 + UUID 10b90ea1-3771-4e2e-9193-4288d9bdcb34", Toast.LENGTH_SHORT).show();
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        messageDigest.update(("2020.06.06.11.18.11" + "10b90ea1-3771-4e2e-9193-4288d9bdcb34").getBytes());
        byte[] bytes = messageDigest.digest();
        String encryptedString = new String(bytes);
        Log.i(TAG, "encrypt timestamp 2020.06.06.11.18.11 + UUID 10b90ea1-3771-4e2e-9193-4288d9bdcb34");
        Log.i(TAG, "result: " + encryptedString);

    }

    /**
     * @see DatabaseHandler will fetch the whole log of recorded contacts
     * (pairs of timestamps and checksums). Then
     * @see ClientThread will fetch the list of infected users from the main server.
     * Then a BroadcastReceiver will be set up that receives the evaluation results when
     * @see RiskEvaluationThread is done evaluating the users risk of infection and
     * displays an appropriate notification.
     *
     * The same is done in the app in a given time interval, but there will be more
     * information regarding the results of the evaluation both logged in the output
     * and displayed to the user of the device in an AlertDialog.
     */
    private void onRiskEvaluation() {
        Toast.makeText(this, "evaluating risk", Toast.LENGTH_SHORT).show();

        ArrayList<String> timestamps = handler.getLogColumn(DatabaseHandler.TIMESTAMPS);
        ArrayList<String> checksums = handler.getLogColumn(DatabaseHandler.CHECKSUMS);

        Thread clientThread = new ClientThread("requestlist");
        clientThread.start();
        try {
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ArrayList<String> infectedList = ((ClientThread) clientThread).getInfectedList();

        broadcastReceiverEvaluationDone = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.hello.coronatrackingapp.EVALUATION_DONE".equals(intent.getAction())) {
                    Bundle bundle= intent.getBundleExtra("com.hello.coronatrackingapp.COUNT");
                    int contactCount = handler.getCount();
                    int infectedContactCount = bundle.getInt("contactCounter");
                    int intervalsExposed = bundle.getInt("intervalsExposed");
                    int secondsExposed = bundle.getInt("secondsExposed");

                    Log.i(TAG, "Total of contacts: " + contactCount);
                    Log.i(TAG, "Contacts with infected people: " + infectedContactCount);
                    Log.i(TAG, "Seconds exposed: " + secondsExposed);
                    Log.i(TAG, "Minutes exposed: " + (intervalsExposed * RiskEvaluationThread.EXPOSURE_INTERVAL) / 60);
                    Log.i(TAG, "Risk at: " + intervalsExposed + " %.");

                    new AlertDialog.Builder(context)
                            .setTitle("Contact count")
                            .setMessage("Total of contacts: " + contactCount
                                    + "\n\nCritical contacts: " + infectedContactCount + " "
                                    + "\n\nSeconds exposed: " + secondsExposed
                                    + "\n\nMinutes exposed: " + (intervalsExposed * RiskEvaluationThread.EXPOSURE_INTERVAL) / 60
                                    + "\n\nRisk at: " + intervalsExposed + " %")
                            .setCancelable(true)
                            .setPositiveButton("Ok", null)
                            .show();
                }
                context.unregisterReceiver(broadcastReceiverEvaluationDone);
            }
        };

        IntentFilter filter = new IntentFilter("com.hello.coronatrackingapp.EVALUATION_DONE");
        registerReceiver(broadcastReceiverEvaluationDone, filter);

        Thread riskEvaluationThread = new RiskEvaluationThread(timestamps, checksums, infectedList, this);
        riskEvaluationThread.start();
    }
}
