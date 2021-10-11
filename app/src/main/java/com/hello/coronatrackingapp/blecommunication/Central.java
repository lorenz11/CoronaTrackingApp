package com.hello.coronatrackingapp.blecommunication;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hello.coronatrackingapp.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

/**
 * this class handles the tasks that need to be executed after the
 * @see Scanner detected a device with this app installed on it.
 */
public class Central {
    private static final String TAG = "at Central";

    private BluetoothGatt bluetoothGatt;
    private boolean connecting;
    /**
     * This will be a LinkedHashmap (see constructor). It is needed, because
     * queuing up connections using just the Android BLE API often causes errors.
     * This Map will make sure that resources are distributed equally. Because it is a Map
     * every device can only have one entry. And because it is a LinkedHashmap it has a queue-like
     * FIFO behaviour. It also holds a long value, representing the time, at which it was
     * added to the queue, so it can be discarded, if that happened too long ago. (The other
     * user might be out of range).
     */
    private Map<BluetoothDevice, Long> connectionQueue;

    Context context;

    private UUID Service_UUID;
    private UUID Characteristic_UUID_Date;
    private UUID Characteristic_UUID_Checksum1;
    private UUID Characteristic_UUID_Checksum2;

    private BluetoothDevice device;

    byte[] checksum;
    int writeCounter;

    public Central(Context context) {
        this.context = context;
        connectionQueue = new LinkedHashMap<>();

        Service_UUID = UUID.fromString(context.getResources().getString(R.string.Service_UUID));
        Characteristic_UUID_Date = UUID.fromString(context.getResources().getString(R.string.Characteristic_UUID_Date));
        Characteristic_UUID_Checksum1 = UUID.fromString(context.getResources().getString(R.string.Characteristic_UUID_Checksum1));
        Characteristic_UUID_Checksum2 = UUID.fromString(context.getResources().getString(R.string.Characteristic_UUID_Checksum2));
    }

    /**
     * for
     * @see Scanner to determine if it is supposed to start the connection on its own.
     * @return connection status
     */
    public boolean isConnecting() {
        return connecting;
    }

    /**
     * @see Scanner can use this to add a newly detected device to the queue
     * @return queue
     */
    public Map<BluetoothDevice, Long> getConnectionQueue() {
        return connectionQueue;
    }


    /**
     * this callback is needed to call connectGatt on a detected device
     */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        /**
         * is called on connection to and disconnection from a GATT server. If connected
         * it will attempt to discover the service offered by the GATT server, identified
         * with a UUID. On disconnection it will look up the oldest entry in the queue and connect
         * to it if it isn't too old (avoiding connection attempts to devices that might already be
         * out of range) and then remove that entry. If the queue is empty it will set 'connecting'
         * to false, so the scanner knows, that it should initiate the connection to the next
         * detected device itself.
         * @param gatt represents the connection and offers a few operations on it.
         * @param status indicates the success or failure of a connection
         * @param newState connected or disconnected
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "connection state changed.");
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connection established.");
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected.");
                gatt.close();

                Iterator iterator = connectionQueue.entrySet().iterator();
                BluetoothDevice device;
                while (iterator.hasNext()) {
                    Map.Entry<BluetoothDevice, Long> entry = (Map.Entry) iterator.next();
                    device = entry.getKey();
                    if (entry.getValue() > (new Date().getTime() - 5000)) {
                        connect(device);
                        connectionQueue.remove(device);
                        return;
                    } else {
                        connectionQueue.remove(device);
                    }
                }
                connecting = false;
            }
        }

        /**
         * called when the Peripherals service is discovered. Will write then
         * to the timestamp characteristic and generate the checksum from this timestamp
         * and the users UUID.
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "status at discovered " + status);

            String timeStamp = new Long(new Date().getTime()).toString();
            checksum = encryptWithDigest(timeStamp);

            BluetoothGattCharacteristic characteristicDate = gatt.getService(Service_UUID).getCharacteristic(Characteristic_UUID_Date);
            characteristicDate.setValue(timeStamp);
            gatt.writeCharacteristic(characteristicDate);
        }

        /**
         * Called when data was written to a characteristic. This will happen after
         * the timestamp was written in onServicesDiscovered() thereby triggering the
         * writing to the checksum characteristics. When this is done (indicated by a
         * counter) the thread will wait shortly before disconnecting (Without waiting
         * it seemed that the execution of disconnect was done before the writing was
         * finished, resulting in undesired behaviour).
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG, "written to characteristic of connected device." + "  " + device.getAddress());

            writeCounter++;
            if (writeCounter == 1) {
                byte[] checksum1 = Arrays.copyOfRange(checksum, 0, (checksum.length) / 2);
                BluetoothGattCharacteristic characteristicChecksum1 = gatt.getService(Service_UUID).getCharacteristic(Characteristic_UUID_Checksum1);
                characteristicChecksum1.setValue(checksum1);
                gatt.writeCharacteristic(characteristicChecksum1);
            } else if (writeCounter == 2) {
                byte[] checksum2 = Arrays.copyOfRange(checksum, (checksum.length) / 2, checksum.length);
                BluetoothGattCharacteristic characteristicChecksum2 = gatt.getService(Service_UUID).getCharacteristic(Characteristic_UUID_Checksum2);
                characteristicChecksum2.setValue(checksum2);
                gatt.writeCharacteristic(characteristicChecksum2);
            } else if (writeCounter == 3) {
                final Handler handler = new Handler(Looper.getMainLooper());
                final Runnable runnable = new Runnable()
                {
                    public void run()
                    {
                        Log.i(TAG, "arrived at execution of handler...");
                        bluetoothGatt.disconnect();
                    }
                };
                handler.postDelayed(runnable, 400);
                writeCounter = 0;
            }
        }

        /**
         * Not used. Overridden for potential future adjustments.
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG, "value received from connected device: " + new String(characteristic.getValue()));
        }
    };

    /**
     * connects the device to a peripheral and sets the status to connecting.
     * @param device to which to connect
     */
    public void connect(BluetoothDevice device) {
        this.device = device;
        bluetoothGatt = device.connectGatt(context, false, gattCallback, TRANSPORT_LE);

        connecting = true;
    }

    /**
     * used to send information to the UI Thread (for example to log debugging information
     * in a textview, this is not used in the final version of the app).
     * @param msg
     */
    private void sendBroadcast(String msg) {
        Intent intent = new Intent("com.hello.coronatrackingapp.CHARACTERISTIC_WRITTEN");
        intent.putExtra("com.hello.coronatrackingapp.CONTENT", msg);
        context.sendBroadcast(intent);
    }

    /**
     * generates a checksum from timestamp and user UUID using Javas MessageDigest.
     * @param timeStamp to be used.
     * @return a byte array containing the checksum.
     */
    private byte[] encryptWithDigest(String timeStamp) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String id = preferences.getString("ID", "default");

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        messageDigest.update((timeStamp + id).getBytes());
        byte[] checksum = messageDigest.digest();

        return checksum;
    }

    /**
     * handles the closing of the GATT (the GATT client).
     */
    public void closeGATT() {
        if (bluetoothGatt != null) {
            Log.i(TAG, "GATT client disconnected and closed.");
            gattCallback = null;
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            connecting = false;
        }
    }
}
