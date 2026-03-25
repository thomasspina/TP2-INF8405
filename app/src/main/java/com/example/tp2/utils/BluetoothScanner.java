package com.example.tp2.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

public class BluetoothScanner {

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
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                if (device != null && deviceFoundListener != null) {
                    deviceFoundListener.onDeviceFound(device, rssi);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
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
    public void startDiscovery(Context context) {
        if (bluetoothAdapter == null) return;

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(discoveryReceiver, filter);
            }
            isReceiverRegistered = true;
        }

        bluetoothAdapter.startDiscovery();
    }

    @SuppressWarnings("MissingPermission")
    public void stopDiscovery(Context context) {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver);
            } catch (IllegalArgumentException ignored) {
                // Receiver was already unregistered
            }
            isReceiverRegistered = false;
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }
}
