package com.example.tp2.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tp2.R;
import com.example.tp2.adapters.DeviceListAdapter;
import com.example.tp2.database.DeviceRepository;
import com.example.tp2.models.BluetoothDeviceModel;
import com.example.tp2.utils.BluetoothScanner;
import com.example.tp2.utils.LocationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final double DEFAULT_LAT = 45.5048;
    private static final double DEFAULT_LNG = -73.6132;

    private MapView mapView;
    private BluetoothScanner bluetoothScanner;
    private LocationHelper locationHelper;
    private DeviceRepository deviceRepository;
    private DeviceListAdapter deviceListAdapter;

    private Marker userPositionMarker;
    private final Map<String, Marker> deviceMarkers = new HashMap<>();
    private final List<BluetoothDeviceModel> detectedDevices = new ArrayList<>();
    private boolean hasMovedCameraToUser = false;
    private boolean btEnableDeclined = false;

    private RecyclerView deviceRecyclerView;
    private TextView emptyStateText;
    private TextView scanStatusText;
    private ProgressBar scanProgressBar;

    private ActivityResultLauncher<Intent> enableBtLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initMap();
        initServices();
        setupRecyclerView();
        setupButtons();
        registerBtEnableLauncher();
        loadSavedDevices();
        checkAndRequestPermissions();
    }

    private void initViews() {
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        scanStatusText = findViewById(R.id.scanStatusText);
        scanProgressBar = findViewById(R.id.scanProgressBar);
    }

    private void initMap() {
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));
        applyMapStyle();
    }

    private void initServices() {
        bluetoothScanner = new BluetoothScanner(this);
        locationHelper = new LocationHelper(this);
        deviceRepository = new DeviceRepository(this);

        bluetoothScanner.setOnDeviceFoundListener((device, rssi) -> {
            Location loc = locationHelper.getLastKnownLocation();
            double lat = (loc != null) ? loc.getLatitude() : DEFAULT_LAT;
            double lng = (loc != null) ? loc.getLongitude() : DEFAULT_LNG;

            BluetoothDeviceModel model = BluetoothDeviceModel.fromBluetoothDevice(device, rssi, lat, lng);
            deviceRepository.insertDevice(model);
            addDeviceToUI(model);
        });

        bluetoothScanner.setOnDiscoveryFinishedListener(() -> {
            runOnUiThread(() -> {
                scanProgressBar.setVisibility(View.GONE);
                scanStatusText.setText(R.string.scan_complete);
            });
        });

        locationHelper.setOnLocationUpdatedListener(location -> {
            updateUserMarker(location);
            if (!hasMovedCameraToUser) {
                mapView.getController().animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()));
                hasMovedCameraToUser = true;
            }
        });
    }

    private void setupRecyclerView() {
        deviceListAdapter = new DeviceListAdapter();
        deviceListAdapter.setOnDeviceClickListener(this::openDeviceDetail);
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceRecyclerView.setAdapter(deviceListAdapter);
    }

    private void setupButtons() {
        findViewById(R.id.btnSwapTheme).setOnClickListener(v -> toggleTheme());
        findViewById(R.id.btnScan).setOnClickListener(v -> startBluetoothScan());
    }

    private void registerBtEnableLauncher() {
        enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (bluetoothScanner.isBluetoothEnabled()) {
                        startBluetoothScan();
                    }
                });
    }

    @SuppressWarnings("MissingPermission")
    private void startBluetoothScan() {
        if (!bluetoothScanner.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
            return;
        }

        if (!LocationHelper.isGpsEnabled(this)) {
            Toast.makeText(this, R.string.gps_disabled_message, Toast.LENGTH_LONG).show();
            return;
        }

        scanProgressBar.setVisibility(View.VISIBLE);
        scanStatusText.setText(R.string.scanning);
        bluetoothScanner.startDiscovery(this);
    }

    private void addDeviceToUI(BluetoothDeviceModel device) {
        runOnUiThread(() -> {
            boolean isNew = true;
            for (int i = 0; i < detectedDevices.size(); i++) {
                if (detectedDevices.get(i).getMacAddress().equals(device.getMacAddress())) {
                    detectedDevices.set(i, device);
                    isNew = false;
                    break;
                }
            }
            if (isNew) {
                detectedDevices.add(device);
            }

            deviceListAdapter.addOrUpdateDevice(device);
            updateEmptyState();
            addDeviceMarkerToMap(device);
        });
    }

    private void addDeviceMarkerToMap(BluetoothDeviceModel device) {
        Marker existing = deviceMarkers.get(device.getMacAddress());
        if (existing != null) {
            // Mise à jour de la position sans supprimer le marqueur pour éviter le clignotement
            existing.setPosition(new GeoPoint(device.getLatitude(), device.getLongitude()));
            existing.setTitle(device.getDisplayName());
            mapView.invalidate();
            return;
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(device.getLatitude(), device.getLongitude()));
        marker.setTitle(device.getDisplayName());
        marker.setSnippet(device.getMacAddress());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setOnMarkerClickListener((m, map) -> {
            openDeviceDetail(device);
            return true;
        });

        mapView.getOverlays().add(marker);
        deviceMarkers.put(device.getMacAddress(), marker);
        mapView.invalidate();
    }

    private void updateUserMarker(Location location) {
        GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (userPositionMarker == null) {
            userPositionMarker = new Marker(mapView);
            userPositionMarker.setTitle(getString(R.string.current_position));
            userPositionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(userPositionMarker);
        }
        userPositionMarker.setPosition(userPoint);
        mapView.invalidate();
    }

    private void updateEmptyState() {
        boolean isEmpty = detectedDevices.isEmpty();
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        deviceRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void loadSavedDevices() {
        deviceRepository.getAllDevices(devices -> runOnUiThread(() -> {
            detectedDevices.clear();
            detectedDevices.addAll(devices);
            deviceListAdapter.updateDevices(new ArrayList<>(devices));
            updateEmptyState();
            for (BluetoothDeviceModel device : devices) {
                addDeviceMarkerToMap(device);
            }
        }));
    }

    private void openDeviceDetail(BluetoothDeviceModel device) {
        Intent intent = new Intent(this, DeviceDetailActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
    }

    private void toggleTheme() {
        int newMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) 
            ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putInt("night_mode", newMode).apply();
        AppCompatDelegate.setDefaultNightMode(newMode);
    }

    private void checkAndRequestPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (needed.isEmpty()) onPermissionsGranted();
        else ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) onPermissionsGranted();
    }

    private void onPermissionsGranted() { locationHelper.startLocationUpdates(); }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // Rafraîchir la liste pour voir les changements de favoris
        loadSavedDevices();
    }

    @Override
    protected void onPause() { super.onPause(); mapView.onPause(); }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothScanner.stopDiscovery(this);
        locationHelper.stopLocationUpdates();
    }

    private void applyMapStyle() {
        mapView.setTileSource(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES 
            ? TileSourceFactory.OpenTopo : TileSourceFactory.MAPNIK);
    }
}
