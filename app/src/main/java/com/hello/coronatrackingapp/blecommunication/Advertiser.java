package com.hello.coronatrackingapp.blecommunication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.hello.coronatrackingapp.R;

import java.util.UUID;

/**
 * This class handles the advertising part of the data exchange between two
 * users. (Sending out a beacon, that carries an identifier to the app).
 */
public class Advertiser {
    private static final String TAG = "at Advertiser";

    // A UUID that tells a beacon receiving user, that a user with the same app is nearby
    private ParcelUuid App_UUID;

    Context context;

    BluetoothAdapter bluetoothAdapter;
    BluetoothLeAdvertiser advertiser;

    public Advertiser (Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        App_UUID = new ParcelUuid(UUID.fromString(context.getResources().getString(R.string.APP_UUID)));
    }

    /**
     * callback needed to start an advertiser, with its methods overridden.
     */
    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "started advertising successfully.");

        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "starting advertising failed. Error code: " + errorCode);
        }
    };

    /**
     * this method actually starts the advertising, after preparing some settings
     * and data that is supposed to be contained by the beacon.
     */
    public void advertise() {
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)           // very important setting!
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(App_UUID)
                .build();

        advertiser.startAdvertising(advertiseSettings, advertiseData, advertisingCallback);
        Log.i(TAG, "start advertising.");
    }

    /**
     * this stops the advertising
     */
    public void stopAdvertising() {
        if(advertiser != null) {
            Log.i(TAG, "advertising stopped.");
            advertiser.stopAdvertising(advertisingCallback);
        }
    }
}
