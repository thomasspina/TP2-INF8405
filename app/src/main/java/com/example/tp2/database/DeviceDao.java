package com.example.tp2.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.tp2.models.BluetoothDeviceModel;

import java.util.List;

@Dao
public interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDevice(BluetoothDeviceModel device);

    @Query("SELECT * FROM devices ORDER BY detectionTimestamp DESC")
    List<BluetoothDeviceModel> getAllDevices();

    @Query("SELECT * FROM devices WHERE macAddress = :mac")
    BluetoothDeviceModel getDeviceByMac(String mac);

    @Query("UPDATE devices SET isFavourite = :fav WHERE macAddress = :mac")
    void setFavourite(String mac, boolean fav);

    @Query("SELECT * FROM devices WHERE isFavourite = 1 ORDER BY detectionTimestamp DESC")
    List<BluetoothDeviceModel> getFavourites();

    @Delete
    void deleteDevice(BluetoothDeviceModel device);
}
