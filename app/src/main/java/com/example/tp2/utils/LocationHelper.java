package com.example.tp2.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {

    public interface OnLocationUpdatedListener {
        void onLocationUpdated(Location location);
    }

    private static final long UPDATE_INTERVAL_MS = 5000;
    private static final long FASTEST_INTERVAL_MS = 2000;

    private final FusedLocationProviderClient fusedClient;
    private OnLocationUpdatedListener locationListener;
    private Location lastKnownLocation;

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;
            Location location = locationResult.getLastLocation();
            if (location != null) {
                lastKnownLocation = location;
                if (locationListener != null) {
                    locationListener.onLocationUpdated(location);
                }
            }
        }
    };

    public LocationHelper(Context context) {
        fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void setOnLocationUpdatedListener(OnLocationUpdatedListener listener) {
        this.locationListener = listener;
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                .build();

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                lastKnownLocation = location;
                if (locationListener != null) {
                    locationListener.onLocationUpdated(location);
                }
            }
        });
    }

    public void stopLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback);
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public static boolean isGpsEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
