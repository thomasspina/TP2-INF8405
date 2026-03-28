package com.example.tp2.models;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "devices")
public class BluetoothDeviceModel implements Serializable {

    @PrimaryKey
    @NonNull
    private String macAddress;

    private String name;
    private int deviceClass;
    private String deviceClassDescription;
    private int bondState;
    private int type;
    private short rssi;
    private String uuids;
    private double latitude;
    private double longitude;
    private long detectionTimestamp;
    private boolean isFavourite;

    public BluetoothDeviceModel() {
        this.macAddress = "";
    }

    @SuppressWarnings("MissingPermission")
    public static BluetoothDeviceModel fromBluetoothDevice(BluetoothDevice device, short rssi,
                                                           double latitude, double longitude) {
        BluetoothDeviceModel model = new BluetoothDeviceModel();
        model.macAddress = device.getAddress();
        model.name = device.getName();
        model.bondState = device.getBondState();
        model.type = device.getType();
        model.rssi = rssi;
        model.latitude = latitude;
        model.longitude = longitude;
        model.detectionTimestamp = System.currentTimeMillis();
        model.isFavourite = false;

        BluetoothClass btClass = device.getBluetoothClass();
        if (btClass != null) {
            model.deviceClass = btClass.getDeviceClass();
            model.deviceClassDescription = getDeviceClassDescription(btClass);
        } else {
            model.deviceClass = 0;
            model.deviceClassDescription = "Inconnu";
        }

        ParcelUuid[] deviceUuids = device.getUuids();
        if (deviceUuids != null && deviceUuids.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < deviceUuids.length; i++) {
                sb.append(deviceUuids[i].toString());
                if (i < deviceUuids.length - 1) sb.append(", ");
            }
            model.uuids = sb.toString();
        }

        return model;
    }

    private static String getDeviceClassDescription(BluetoothClass btClass) {
        int majorClass = btClass.getMajorDeviceClass();
        int devicePart = btClass.getDeviceClass();

        // Check specifically for audio devices/headphones
        if (majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO) {
            switch (devicePart) {
                case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                    return "Casque Audio";
                case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                    return "Kit Mains-libres";
                case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                    return "Écouteurs";
                case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                    return "Haut-parleur";
                default:
                    return "Audio/Vidéo";
            }
        }

        switch (majorClass) {
            case BluetoothClass.Device.Major.COMPUTER:
                return "Ordinateur";
            case BluetoothClass.Device.Major.HEALTH:
                return "Santé";
            case BluetoothClass.Device.Major.IMAGING:
                return "Imagerie";
            case BluetoothClass.Device.Major.MISC:
                return "Divers";
            case BluetoothClass.Device.Major.NETWORKING:
                return "Réseau";
            case BluetoothClass.Device.Major.PERIPHERAL:
                return "Périphérique";
            case BluetoothClass.Device.Major.PHONE:
                return "Téléphone";
            case BluetoothClass.Device.Major.TOY:
                return "Jouet";
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return "Non catégorisé";
            case BluetoothClass.Device.Major.WEARABLE:
                return "Portable";
            default:
                return "Inconnu";
        }
    }

    @NonNull
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(@NonNull String macAddress) { this.macAddress = macAddress; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDeviceClass() { return deviceClass; }
    public void setDeviceClass(int deviceClass) { this.deviceClass = deviceClass; }

    public String getDeviceClassDescription() { return deviceClassDescription; }
    public void setDeviceClassDescription(String deviceClassDescription) { this.deviceClassDescription = deviceClassDescription; }

    public int getBondState() { return bondState; }
    public void setBondState(int bondState) { this.bondState = bondState; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public short getRssi() { return rssi; }
    public void setRssi(short rssi) { this.rssi = rssi; }

    public String getUuids() { return uuids; }
    public void setUuids(String uuids) { this.uuids = uuids; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getDetectionTimestamp() { return detectionTimestamp; }
    public void setDetectionTimestamp(long detectionTimestamp) { this.detectionTimestamp = detectionTimestamp; }

    public boolean isFavourite() { return isFavourite; }
    public void setFavourite(boolean favourite) { isFavourite = favourite; }

    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : macAddress;
    }
}
