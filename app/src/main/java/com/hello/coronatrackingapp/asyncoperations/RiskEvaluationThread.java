package com.hello.coronatrackingapp.asyncoperations;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A Thread that evaluates the users risk of being infected. This is
 * happening asynchronously, because it can take a long time to do that and
 * would therefore block the UI Thread.
 */
public class RiskEvaluationThread extends Thread {
    private static final String TAG = "at RiskEvaluation";
    public static final int EXPOSURE_INTERVAL = 15;

    ArrayList<String> timestamps;
    ArrayList<String> checksums;
    ArrayList<String> infectedList;

    Context context;

    /**
     * Constructor, getting all the necessary information that this class needs
     * for risk evaluation.
     *
     * @param timestamps list of timestamps from the database, describing the time at which a contact took place.
     * @param checksums list of associated checksums, that need to be reproduced or discarded if that's not possible.
     * @param infectedList list of keys corresponding to infected user, downloaded from the main server.
     * @param context the calling activity's context, needed to send an internal broadcast.
     */
    public RiskEvaluationThread(ArrayList<String> timestamps, ArrayList<String> checksums, ArrayList<String> infectedList, Context context) {
        this.timestamps = timestamps;
        this.checksums = checksums;
        this.infectedList = infectedList;

        this.context = context;
    }

    /**
     * Threads code to be executed when started.
     */
    @Override
    public void run() {
        Log.i(TAG, "evaluating risk by comparing encrypted strings in database with results of apps encryption method.");
        int contactCounter = 0;
        long milSecsExposed = 0;
        // looping through all entries in the list of infected people, trying to reproduce all the checksums with the corresponding timestamps.
        for(int i = 0; i < infectedList.size(); i++) {
            ArrayList<String> stampsInOneKey = new ArrayList<>();
            // check all database entries for every key from the main server
            for (int j = 0; j < timestamps.size(); j++) {
                if (isInfected(timestamps.get(j), infectedList.get(i), checksums.get(j))) {
                    stampsInOneKey.add(timestamps.get(j));
                    contactCounter++;
                }
            }
            // if successful for one key, calculate the time the user was exposed to that keys device
            milSecsExposed += calculateTimeExposed(stampsInOneKey);
        }

        // send some information about the evaluation to the calling activity via internal broadcast
        int secondsExposed = (int) (milSecsExposed / 1000);
        int intervalsExposed = (int) (milSecsExposed / (EXPOSURE_INTERVAL * 1000));
        Bundle bundle = new Bundle();
        bundle.putInt("contactCounter", contactCounter);
        bundle.putInt("intervalsExposed", intervalsExposed);
        bundle.putInt("secondsExposed", secondsExposed);

        sendBroadcast(bundle);
    }

    /**
     * calculates the time a user was exposed according to one set of timestamps
     * (adding the time differences between the stamps if they aren't more than
     * one minute apart: if they are, the user was probably out of the exposure
     * range for a while).
     * @param stamps list of the timestamps belonging to one key from the main server.
     * @return the time a user was exposed to that particular devices user.
     */
    private long calculateTimeExposed(ArrayList<String> stamps) {
        long timeExposed = 0;
        long lastEncounter = 0;
        for(String entry : stamps) {
            long encounterTime = Long.parseLong(entry);
            long difference = encounterTime - lastEncounter;
            if(difference < 60 * 1000) {
                timeExposed += difference;
            }
            lastEncounter = encounterTime;
        }

        return timeExposed;
    }

    /**
     * check if the output of Javas MessageDigest (an encryption algorithm)
     * given the timestamp and one of the keys from the server matches the
     * checksum that corresponds to the timestamp.
     *
     * @param timeStamp timestamp from database.
     * @param listItem key from the list of infected users
     * @param checksum checksum logged with the timestamp
     * @return true if checksum could be reproduced.
     */
    private boolean isInfected(String timeStamp, String listItem, String checksum) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        messageDigest.update((timeStamp + listItem).getBytes());
        byte[] bytes = messageDigest.digest();
        String encryptedString = new String(bytes);
        bytes = encryptedString.getBytes();

        return Arrays.equals(bytes, checksum.getBytes());
    }

    /**
     * send internal broadcast with the results of the evaluation to the
     * calling activity.
     * @param exposureInfo contains different pieces of that result
     */
    private void sendBroadcast(Bundle exposureInfo) {
        Intent intent = new Intent("com.hello.coronatrackingapp.EVALUATION_DONE");
        intent.putExtra("com.hello.coronatrackingapp.COUNT", exposureInfo);
        context.sendBroadcast(intent);
    }
}
