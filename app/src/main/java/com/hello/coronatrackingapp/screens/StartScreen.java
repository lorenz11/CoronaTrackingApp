package com.hello.coronatrackingapp.screens;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.hello.coronatrackingapp.R;
import com.hello.coronatrackingapp.asyncoperations.ClientThread;
import com.hello.coronatrackingapp.asyncoperations.RiskEvaluationThread;
import com.hello.coronatrackingapp.database.DatabaseHandler;
import com.hello.coronatrackingapp.blecommunication.TrackingHandler;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * This class is the activity that is presented to th user at startup.
 * It lets the user start and stop the proximity tracking, evaluate the
 * current risk of being infected (which it does on its own too in a
 * given time interval), report to the main server if infected and provides
 * information about the virus as well as about the app itself.
 */
public class StartScreen extends AppCompatActivity {
    private static final String TAG = "at StartScreen";

    private TrackingHandler trackingHandler;

    Switch recordSwitch;
    ProgressBar riskProgress;

    BroadcastReceiver broadcastReceiverEvaluationDone;
    DatabaseHandler handler;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Context context = this;
    Timer timer;

    boolean fromLogScreen;

    /**
     * Activity entry point in Android
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);
        setTitle("Corona Tracker");

        // permission required by Androids specifications
        ActivityCompat.requestPermissions(StartScreen.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                1);

        handler = new DatabaseHandler(this);
        trackingHandler = new TrackingHandler(this, handler);

        // delete entries older than 2 weeks once a day
        TimerTask timerTaskDatabase = new TimerTask() {
            @Override
            public void run() {
                handler.deleteOldEntries();
            }
        };
        timer = new Timer();
        timer.schedule(timerTaskDatabase, 10 * 1000, DatabaseHandler.DAY);

        // evaluate risk once an hour
        TimerTask timerTaskEvaluation = new TimerTask() {
            @Override
            public void run() {
                evaluateRisk();
            }
        };
        timer = new Timer();
        timer.schedule(timerTaskEvaluation, 3600 * 6000, 3600 * 6000);

        // assign a (more or less) unique number, identifying this instance of the app at first startup
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();
        if (!preferences.contains("ID")) {
            editor.putString("ID", UUID.randomUUID().toString());
            editor.apply();
        }

        // if risk was already evaluated once set the risk bar accordingly
        riskProgress = findViewById(R.id.progressrisk);
        if (preferences.contains("Risk")) {
            int progress = preferences.getInt("Risk", 0);
            riskProgress.setProgress(progress);
            setProgressBarColor(progress);
        }

        recordSwitch = findViewById(R.id.switch1);
        addSwitchListener();


    }

    /**
     * provides menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * providing one entry to the menu, letting the user get to the Log Screen
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item1) {
            fromLogScreen = true;
            Intent intent = new Intent(this, LogScreen.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @see TrackingHandler starts the proximity tracking
     */
    public void onStartRecording() {
        Toast.makeText(this, "recording started", Toast.LENGTH_SHORT).show();
        trackingHandler.startTracking(this);
    }

    /**
     * @see TrackingHandler stops the proximity tracking
     */
    public void onStopRecording() {
        Toast.makeText(this, "recording stopped", Toast.LENGTH_SHORT).show();
        trackingHandler.stopTracking();
    }

    /**
     * @see ClientThread will upload the apps UUID, after this method displays a dialog
     * that lets the user enter the password, which will be checked by the server. It is
     * set to 123 for convenience in testing.
     * @param view links this method to the correct button
     */
    public void onReportInfection(View view) {
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
                        Toast.makeText(context, "you reported your infection", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    /**
     * see description below.
     *
     * @param view links this method to the correct button.
     */
    public void onEvaluateRisk(View view) {
        evaluateRisk();
    }

    /**
     * This method will be called every six hours (see the Timer that is set
     * up in the onCreate() method of this activity), but also, when the user
     * presses the corresponding button.
     *
     * @see DatabaseHandler will fetch the whole log of recorded contacts
     * (pairs of timestamps and checksums). Then
     * @see ClientThread will fetch the list of infected users from the main server.
     * Then a BroadcastReceiver will be set up that receives the evaluation results when
     * @see RiskEvaluationThread is done evaluating the users risk of infection and
     * displays an appropriate notification.
     */
    private void evaluateRisk() {
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

        // Broadcastreceiver that receives the result of the evaluation from the Evaluation Thread
        broadcastReceiverEvaluationDone = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.hello.coronatrackingapp.EVALUATION_DONE".equals(intent.getAction())) {
                    Bundle bundle = intent.getBundleExtra("com.hello.coronatrackingapp.COUNT");
                    int intervalsExposed = bundle.getInt("intervalsExposed");
                    //intervalsExposed = 90;        // test value
                    riskProgress.setProgress(intervalsExposed);
                    // add the value to the SharedPreferences, so there is no need to reevaluate when restarting the app.
                    editor.putInt("Risk", intervalsExposed);
                    editor.apply();

                    // set the notification to the user upon evaluation result, according to the risk value.
                    String riskString = intervalsExposed > 60 ? context.getString(R.string.high_risk_string) : context.getString(R.string.low_risk_string);
                    setProgressBarColor(intervalsExposed);


                    new AlertDialog.Builder(context)
                            .setTitle("Risk of infection")
                            .setMessage(riskString + "\n\nRisk at " + intervalsExposed + " %")
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

    /**
     * sets the color of the progressbar depending on at how high a risk
     * a user is of being infected.
     *
     * @param progress in percent (one percent = one time interval of 15 seconds).
     */
    private void setProgressBarColor(int progress) {
        if (progress > 60) {
            riskProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.progressred)));
        } else {
            riskProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.progressgreen)));
        }
    }

    /**
     * starts the activity that displays information about the virus.
     *
     * @param view links this method to the correct button.
     */
    public void onAboutVirus(View view) {
        Intent intent = new Intent(this, AboutVirusScreen.class);
        startActivity(intent);
    }

    /**
     * starts the activity that displays information about the app.
     *
     * @param view links this method to the correct button.
     */
    public void onAboutApp(View view) {
        Intent intent = new Intent(this, AboutAppScreen.class);
        startActivity(intent);
    }

    /**
     * assigns the behaviour to the "start proximity recording" switch button.
     */
    private void addSwitchListener() {
        recordSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    onStartRecording();
                    recordSwitch.setText("Proximity Recording Started");
                } else {
                    onStopRecording();
                    recordSwitch.setText("Proximity Recording Stopped");
                }
            }
        });
    }

    /**
     * show user alertdialog at display startup only if  high risk evaluated
     */
    @Override
    public void onResume() {
        super.onResume();
        if (fromLogScreen) {
            fromLogScreen = false;
            return;
        }

        int risk = preferences.getInt("Risk", -1);
        if (preferences.getInt("Risk", -1) > 60) {
            new AlertDialog.Builder(context)
                    .setTitle("Risk of infection")
                    .setMessage(getString(R.string.high_risk_string) + "\n\nRisk at " + risk + " %")
                    .setCancelable(true)
                    .setPositiveButton("Ok", null)
                    .show();
        }
    }
}
