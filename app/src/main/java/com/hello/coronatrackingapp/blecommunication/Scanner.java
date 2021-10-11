package com.hello.coronatrackingapp.blecommunication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.hello.coronatrackingapp.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This class is responsible for scanning the devices environment for
 * beacons which indicate the presence of another app user.
 */
public class Scanner {
    private static final String TAG = "at Scanner";
    Context context;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;

    Central central;

    private ParcelUuid App_UUID;

    public Scanner(Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        App_UUID = new ParcelUuid(UUID.fromString(context.getResources().getString(R.string.APP_UUID)));
    }

    /**
     * this callback is needed to start the scanner.
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        /**
         * This method is called, when a nearby device with the appropriate UUID is detected.
         * If there are no devices in the queue, it will connect to that device, otherwise
         * it will place it in the queue. (This queue is not a feature of the BLE API. It is a LinkedHashmap field in
         * @see Central that makes sure that resources are distributed equally. Because it is a Map
         * every device can only have one entry. And because it is a LinkedHashmap it has a queue-like
         * FIFO behaviour. This is necessary, because queuing connections by just calling connect will
         * quite often cause connection errors, while not distributing the channel equitably either.
         * @param callbackType
         * @param result contains the scanRecord which contains other necessary information.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) { // if startScan without settings and filters
            if (result.getScanRecord().getServiceUuids() != null) {
                UUID resultUUID = result.getScanRecord().getServiceUuids().get(0).getUuid();
                Log.i(TAG, "result UUID: " + resultUUID);
                if (resultUUID.equals(App_UUID.getUuid())) {
                    Log.i(TAG, "detected other device.");
                    if (!central.isConnecting()) {              // if queue is empty connect right here
                        central.connect(result.getDevice());
                    } else {                                    // otherwise put the device in the queue and let 'Central' handle the connection later on.
                        central.getConnectionQueue().put(result.getDevice(), new Date().getTime());
                    }
                }
            }
        }

        /**
         * if the scanner is set to report with a delay, this callback method
         * will deliver all the results from the specified period of time. This
         * is just overridden here for potential future adjustments, because it
         * actually never worked (when scanning with a delay, the test devices
         * wouldn't find any devices at all.)
         * @param results
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) { // if startScan with settings (delay > 0)
            for (ScanResult r : results) {
                if (results.get(0).getScanRecord().getServiceUuids() != null) {
                    UUID resultUUID = results.get(0).getScanRecord().getServiceUuids().get(0).getUuid();
                    Log.i(TAG, "result UUID: " + resultUUID);

                    if (resultUUID.equals(App_UUID.getUuid())) {
                        Log.i(TAG, "detected other device.");
                        if (!central.isConnecting()) {
                            central.connect(results.get(0).getDevice());
                        } else {
                            central.getConnectionQueue().put(results.get(0).getDevice(), new Date().getTime());
                        }
                    }
                }
            }
        }

        /**
         * called when the scan failed. (Was never called during testing)
         * @param errorCode
         */
        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "scan failed.");
        }
    };

    /**
     * starts the scanning for other devices.
     */
    public void startScanning() {
        // for future adjustments. StartScan is called without these settings.
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(400)
                .build();
        ScanFilter scanFilterTest = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(context.getResources().getString(R.string.APP_UUID)))).build();
        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilterTest);
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        //bluetoothLeScanner.startScan(filters, settings, scanCallback);  //!!for starting with report delay > 0!!
        central = new Central(context);
        Log.i(TAG, "started scanning.");
        bluetoothLeScanner.startScan(scanCallback);
    }

    /**
     * this stops the scanning.
     */
    public void stopScanning() {
        if(bluetoothLeScanner != null) {
            Log.i(TAG, "scanning stopped.");
            bluetoothLeScanner.stopScan(scanCallback);
        }
        if(central != null) {
            central.closeGATT();
            central = null;
        }
    }
}
