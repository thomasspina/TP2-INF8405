package com.example.tp2.activities;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tp2.R;
import com.example.tp2.database.DeviceRepository;
import com.example.tp2.models.BluetoothDeviceModel;
import com.example.tp2.utils.LocationHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceDetailActivity extends AppCompatActivity {

    private BluetoothDeviceModel device;
    private DeviceRepository deviceRepository;
    private MaterialButton btnDirections;
    private MaterialButton btnFavourite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        deviceRepository = new DeviceRepository(this);

        device = (BluetoothDeviceModel) getIntent().getSerializableExtra("device");
        if (device == null) {
            finish();
            return;
        }

        // Initialize UI with initial data
        populateDeviceInfo();
        setupActionButtons();

        // Refresh data from database to get the latest favourite status
        refreshDeviceData();
    }

    private void refreshDeviceData() {
        deviceRepository.getDeviceByMac(device.getMacAddress(), updatedDevice -> {
            if (updatedDevice != null) {
                runOnUiThread(() -> {
                    device = updatedDevice;
                    updateFavouriteButton(btnFavourite);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGpsActionState();
    }

    private void populateDeviceInfo() {
        TextView nameView = findViewById(R.id.detailDeviceName);
        TextView macView = findViewById(R.id.detailMacAddress);

        nameView.setText(device.getDisplayName());
        macView.setText(device.getMacAddress());

        setInfoRow(R.id.rowDeviceClass, getString(R.string.label_device_class),
                device.getDeviceClassDescription());

        setInfoRow(R.id.rowDeviceType, getString(R.string.label_device_type),
                deviceTypeToString(device.getType()));

        setInfoRow(R.id.rowBondState, getString(R.string.label_bond_state),
                bondStateToString(device.getBondState()));

        setInfoRow(R.id.rowRssi, getString(R.string.label_rssi),
                device.getRssi() + " dBm");

        setInfoRow(R.id.rowUuids, getString(R.string.label_uuids),
                device.getUuids() != null ? device.getUuids() : getString(R.string.not_available));

        setInfoRow(R.id.rowLocation, getString(R.string.label_location),
                String.format(Locale.getDefault(), "%.6f, %.6f",
                        device.getLatitude(), device.getLongitude()));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        setInfoRow(R.id.rowDetectedAt, getString(R.string.label_detected_at),
                sdf.format(new Date(device.getDetectionTimestamp())));
    }

    private void setInfoRow(int rowId, String label, String value) {
        View row = findViewById(rowId);
        TextView labelView = row.findViewById(R.id.rowLabel);
        TextView valueView = row.findViewById(R.id.rowValue);
        labelView.setText(label);
        valueView.setText(value);
    }

    private void setupActionButtons() {
        btnDirections = findViewById(R.id.btnDirections);
        btnFavourite = findViewById(R.id.btnFavourite);
        MaterialButton btnShare = findViewById(R.id.btnShare);

        updateGpsActionState();
        btnDirections.setOnClickListener(v -> {
            if (!LocationHelper.isGpsEnabled(this)) {
                Toast.makeText(this, R.string.gps_disabled_message, Toast.LENGTH_SHORT).show();
                updateGpsActionState();
                return;
            }
            
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", 
                device.getLatitude(), device.getLongitude(), 
                device.getLatitude(), device.getLongitude(), 
                Uri.encode(device.getDisplayName()));
            
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Aucune application de carte trouvée", Toast.LENGTH_SHORT).show();
            }
        });

        updateFavouriteButton(btnFavourite);
        btnFavourite.setOnClickListener(v -> {
            boolean newState = !device.isFavourite();
            device.setFavourite(newState);
            deviceRepository.setFavourite(device.getMacAddress(), newState);
            updateFavouriteButton(btnFavourite);
            Toast.makeText(this, newState ? "Ajouté aux favoris" : "Retiré des favoris", Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> shareDeviceInfo());
    }

    private void updateGpsActionState() {
        if (btnDirections == null) return;
        boolean gpsEnabled = LocationHelper.isGpsEnabled(this);
        btnDirections.setEnabled(gpsEnabled);
        btnDirections.setAlpha(gpsEnabled ? 1.0f : 0.5f);
    }

    private void updateFavouriteButton(MaterialButton btn) {
        if (btn == null) return;
        if (device.isFavourite()) {
            btn.setText(R.string.btn_remove_favourite);
        } else {
            btn.setText(R.string.btn_add_favourite);
        }
    }

    private void shareDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.label_device_name)).append(": ").append(device.getDisplayName()).append("\n");
        sb.append(getString(R.string.label_mac_address)).append(": ").append(device.getMacAddress()).append("\n");
        sb.append(getString(R.string.label_device_class)).append(": ").append(device.getDeviceClassDescription()).append("\n");
        sb.append(getString(R.string.label_device_type)).append(": ").append(deviceTypeToString(device.getType())).append("\n");
        sb.append(getString(R.string.label_bond_state)).append(": ").append(bondStateToString(device.getBondState())).append("\n");
        sb.append(getString(R.string.label_rssi)).append(": ").append(device.getRssi()).append(" dBm\n");
        sb.append(getString(R.string.label_location)).append(": ")
                .append(String.format(Locale.getDefault(), "%.6f, %.6f",
                        device.getLatitude(), device.getLongitude())).append("\n");

        if (device.getUuids() != null) {
            sb.append(getString(R.string.label_uuids)).append(": ").append(device.getUuids()).append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_device_info_title)));
    }

    private String bondStateToString(int state) {
        switch (state) {
            case BluetoothDevice.BOND_BONDED:
                return getString(R.string.bond_bonded);
            case BluetoothDevice.BOND_BONDING:
                return getString(R.string.bond_bonding);
            default:
                return getString(R.string.bond_none);
        }
    }

    private String deviceTypeToString(int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return getString(R.string.type_classic);
            case BluetoothDevice.DEVICE_TYPE_LE:
                return getString(R.string.type_le);
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return getString(R.string.type_dual);
            default:
                return getString(R.string.type_unknown);
        }
    }
}
