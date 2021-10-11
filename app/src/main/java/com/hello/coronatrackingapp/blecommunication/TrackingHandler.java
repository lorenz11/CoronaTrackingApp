package com.hello.coronatrackingapp.blecommunication;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.hello.coronatrackingapp.database.DatabaseHandler;

/**
 * This is a handler that bundles and gives access to all the features
 * that are needed to successfully exchange proximity data between users,
 * using Bluetooth Low Energy. The main components for that are the ability
 * to advertise (meaning to broadcast short bursts with the data, that identifies
 * the device as user of the tracking app), to set up a peripheral
 * (a BluetoothGattServer along with the service and its characteristics)
 * to scan for those beacons, to connect to devices that are found during the scan
 * and finally to transmit a timestamp / checksum pair by writing to the
 * characteristic offered by another devices service.
 */
public class TrackingHandler {
    private BluetoothAdapter bluetoothAdapter;
    private Advertiser advertiser;
    private Peripheral peripheral;
    private Scanner scanner;
    Context context;

    /**
     * a constructor, that sets up all the functionality described in the javadoc
     * description of this class.
     * @param context the calling activity's context.
     * @param databaseHandler an instance of the Database handler needed by the GattServer
     *                        to log contact information.
     */
    public TrackingHandler(Context context, DatabaseHandler databaseHandler) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        advertiser = new Advertiser(context, bluetoothAdapter);
        peripheral = new Peripheral(context, bluetoothAdapter, databaseHandler);
        scanner = new Scanner(context, bluetoothAdapter);
    }

    /**
     * starts all necessary components at once.
     * @param context
     */
    public void startTracking(Context context) {
        advertiser.advertise();
        peripheral.startServer();
        scanner.startScanning();
    }

    /**
     * stops all necessary components at once.
     */
    public void stopTracking() {
        advertiser.stopAdvertising();
        peripheral.closeServer();
        scanner.stopScanning();
    }

    /**
     * starts just the advertising, needed for the LogScreen.
     */
    public void startAdvertising() {
        advertiser.advertise();
    }

    /**
     * stops just the advertising, needed for the LogScreen.
     */
    public void stopAdvertising() {
        advertiser.stopAdvertising();
    }

    /**
     * starts just the scanning, needed for the LogScreen.
     */
    public void startScanning() {
        scanner.startScanning();
    }

    /**
     * stops just the scanning, needed for the LogScreen.
     */
    public void stopScanning() {
        scanner.stopScanning();
    }

    /**
     * starts just the peripheral, needed for the LogScreen.
     */
    public void startPeripheral() {
        peripheral.startServer();
    }

    /**
     * stops just the peripheral, needed for the LogScreen.
     */
    public void stopPeripheral() {
        peripheral.closeServer();
    }
}
