package com.hello.coronatrackingapp.blecommunication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.hello.coronatrackingapp.R;
import com.hello.coronatrackingapp.database.DatabaseHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * With this class we can make the phone, the app is installed on a
 * peripheral (BLE terminology for GATT Server). With an instance of this
 * class we can start the GATT Server and prepare a service that will be
 * offered by it, containing various characteristics that can be written
 * to by connected devices.
 */
public class Peripheral {
    private static final String TAG = "at Peripheral";

    // UUIDs identifying the peripherals service and its content
    private UUID Service_UUID;
    private UUID Characteristic_UUID_Date;
    private UUID Characteristic_UUID_Checksum1;
    private UUID Characteristic_UUID_Checksum2;


    Context context;
    BluetoothAdapter bluetoothAdapter;
    DatabaseHandler databaseHandler;

    BluetoothGattServer bluetoothGattServer;

    // a map to distinguish between different devices that are connected at the
    // same time thereby not mixing up the received information and assigning it
    // to the correct device.
    Map<BluetoothDevice, ArrayList<String>> deviceMap = new HashMap<>();

    public Peripheral(Context context, BluetoothAdapter bluetoothAdapter, DatabaseHandler databaseHandler) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.databaseHandler = databaseHandler;

        Service_UUID = UUID.fromString(context.getResources().getString(R.string.Service_UUID));
        Characteristic_UUID_Date = UUID.fromString(context.getResources().getString(R.string.Characteristic_UUID_Date));
        Characteristic_UUID_Checksum1 = UUID.fromString(context.getResources().getString(R.string.Characteristic_UUID_Checksum1));
        Characteristic_UUID_Checksum2 = UUID.fromString(context.getResources().getString(R.string.Characteristic_UUID_Checksum2));
    }

    /**
     * The Gatt server also needs a callback to be started with the following methods overridden.
     */
    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        /**
         * This will be called every time another device connects to or disconnects from
         * this device.
         *
         * @param device the device that connected/disconnected
         * @param status 0 means everything went fine, the common and very unspecific 133 just means something went wrong
         * @param newState indicates the kind of connection change
         */
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "server connected to device.");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "server disconnected from device.");
                deviceMap.remove(device);   // When done with this device it can be removed from the device map (see description at declaration)
            }
        }

        /**
         * This is called whenever a connected device wants to write something to this peripherals
         * services characteristics. The information, transmitted to three different characteristics
         * (see below for a description of these characteristics), will be logged into the database.
         */
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            String content = new String(value);
            Log.i(TAG, "value written to characteristic by connected device: " + new String(value) + "  " + device.getAddress());

            if(deviceMap.keySet().contains(device)) {
                deviceMap.get(device).add(content);
                deviceMap.put(device, deviceMap.get(device));
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add(content);
                deviceMap.put(device, list);
            }

            if(deviceMap.get(device).size() == 3) {
                ArrayList<String> list = deviceMap.get(device);
                databaseHandler.logContact(list.get(0), list.get(1) + list.get(2));
                deviceMap.remove(device);
            }

        }

        /**
         * Although it is currently not used by the app, it is overridden here
         * in an exemplary way for future adjustments and improvements.
         */
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, "response".getBytes());
        }
    };

    /**
     * This starts the GATT server
     */
    public void  startServer() {
        Log.i(TAG, "opening a GATT sever.");
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothGattServer = mBluetoothManager.openGattServer(context, gattServerCallback);
        bluetoothGattServer.addService(newService());
    }

    /**
     * This method creates a new service, that will be offered by the peripheral. It
     * contains three different characteristics: one for the timestamp (time a connected
     * device sent its information) the second for the first half of the checksum, the third
     * for the second half (it is only possible to send < 20 bytes, so it needs to be split up). Check
     * @see com.hello.coronatrackingapp.asyncoperations.RiskEvaluationThread for detailed
     * information about how this data is used later on.
     * @return
     */
    private BluetoothGattService newService() {
        // the characteristics are equipped with a few more permissions and properties than actually needed (also for potential adjustments in the future).
        BluetoothGattCharacteristic characteristicDate = new BluetoothGattCharacteristic(Characteristic_UUID_Date, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattCharacteristic characteristicChecksum1 = new BluetoothGattCharacteristic(Characteristic_UUID_Checksum1, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattCharacteristic characteristicChecksum2 = new BluetoothGattCharacteristic(Characteristic_UUID_Checksum2, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattService service = new BluetoothGattService(Service_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        service.addCharacteristic(characteristicDate);
        service.addCharacteristic(characteristicChecksum1);
        service.addCharacteristic(characteristicChecksum2);

        return service;
    }

    /**
     * this closes the server.
     */
    public void closeServer() {
        if(bluetoothGattServer != null) {
            Log.i(TAG, "peripheral server closed.");
            bluetoothGattServer.clearServices();
            bluetoothGattServer.close();
        }
    }
}
