package com.example.tp2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tp2.R;
import com.example.tp2.models.BluetoothDeviceModel;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDeviceModel device);
    }

    private final List<BluetoothDeviceModel> devices = new ArrayList<>();
    private OnDeviceClickListener clickListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.clickListener = listener;
    }

    public void updateDevices(List<BluetoothDeviceModel> newDevices) {
        devices.clear();
        devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    public void addOrUpdateDevice(BluetoothDeviceModel device) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getMacAddress().equals(device.getMacAddress())) {
                devices.set(i, device);
                notifyItemChanged(i);
                return;
            }
        }
        devices.add(device);
        notifyItemInserted(devices.size() - 1);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDeviceModel device = devices.get(position);
        holder.deviceName.setText(device.getDisplayName());
        holder.deviceMac.setText(device.getMacAddress());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceName;
        final TextView deviceMac;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceMac = itemView.findViewById(R.id.deviceMac);
        }
    }
}
