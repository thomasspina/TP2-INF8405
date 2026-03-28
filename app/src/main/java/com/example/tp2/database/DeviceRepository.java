package com.example.tp2.database;

import android.content.Context;

import com.example.tp2.models.BluetoothDeviceModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DeviceRepository {

    private final DeviceDao deviceDao;
    private final ExecutorService executor;

    public DeviceRepository(Context context) {
        deviceDao = AppDatabase.getInstance(context).deviceDao();
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Insère ou met à jour un appareil en préservant son état "favori" s'il existe déjà.
     */
    public void insertDevice(BluetoothDeviceModel device) {
        executor.execute(() -> {
            BluetoothDeviceModel existing = deviceDao.getDeviceByMac(device.getMacAddress());
            if (existing != null) {
                // On préserve l'état favori de la base de données
                device.setFavourite(existing.isFavourite());
            }
            deviceDao.insertDevice(device);
        });
    }

    public void getAllDevices(Consumer<List<BluetoothDeviceModel>> callback) {
        executor.execute(() -> {
            List<BluetoothDeviceModel> devices = deviceDao.getAllDevices();
            callback.accept(devices);
        });
    }

    public void getDeviceByMac(String mac, Consumer<BluetoothDeviceModel> callback) {
        executor.execute(() -> {
            BluetoothDeviceModel device = deviceDao.getDeviceByMac(mac);
            callback.accept(device);
        });
    }

    public void setFavourite(String mac, boolean fav) {
        executor.execute(() -> deviceDao.setFavourite(mac, fav));
    }

    public void getFavourites(Consumer<List<BluetoothDeviceModel>> callback) {
        executor.execute(() -> {
            List<BluetoothDeviceModel> favourites = deviceDao.getFavourites();
            callback.accept(favourites);
        });
    }
}
