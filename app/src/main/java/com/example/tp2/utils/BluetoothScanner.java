package com.example.tp2.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

public class BluetoothScanner {

    private static final String TAG = "BluetoothScanner";

    public interface OnDeviceFoundListener {
        void onDeviceFound(BluetoothDevice device, short rssi);
    }

    public interface OnDiscoveryFinishedListener {
        void onDiscoveryFinished();
    }

    private final BluetoothAdapter bluetoothAdapter;
    private OnDeviceFoundListener deviceFoundListener;
    private OnDiscoveryFinishedListener discoveryFinishedListener;
    private boolean isReceiverRegistered = false;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                
                if (device != null) {
                    // Log details for debugging
                    String name = device.getName();
                    if (name == null) {
                        // Try to get name from intent
                        name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    }
                    
                    Log.d(TAG, "Appareil détecté: " + device.getAddress() + " | Nom: " + (name != null ? name : "Inconnu"));
                    
                    if (deviceFoundListener != null) {
                        deviceFoundListener.onDeviceFound(device, rssi);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "Scan Bluetooth démarré");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Scan Bluetooth terminé");
                if (discoveryFinishedListener != null) {
                    discoveryFinishedListener.onDiscoveryFinished();
                }
            }
        }
    };

    public BluetoothScanner(Context context) {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = (manager != null) ? manager.getAdapter() : null;
    }

    public void setOnDeviceFoundListener(OnDeviceFoundListener listener) {
        this.deviceFoundListener = listener;
    }

    public void setOnDiscoveryFinishedListener(OnDiscoveryFinishedListener listener) {
        this.discoveryFinishedListener = listener;
    }

    @SuppressWarnings("MissingPermission")
    public boolean startDiscovery(Context context) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter est null");
            return false;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(discoveryReceiver, filter);
            }
            isReceiverRegistered = true;
        }

        boolean success = bluetoothAdapter.startDiscovery();
        Log.d(TAG, "Démarrage du scan: " + (success ? "SUCCÈS" : "ÉCHEC"));
        return success;
    }

    @SuppressWarnings("MissingPermission")
    public void stopDiscovery(Context context) {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver);
            } catch (IllegalArgumentException ignored) {}
            isReceiverRegistered = false;
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
}
