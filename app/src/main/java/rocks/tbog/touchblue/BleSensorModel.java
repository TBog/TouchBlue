package rocks.tbog.touchblue;

import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.UUID;

import rocks.tbog.touchblue.helpers.BleHelper;

public class BleSensorModel {
    @NonNull
    private final String address;
    private final String name;
    private final HashMap<UUID, MutableLiveData<Object>> mData = new HashMap<>(0);
    private boolean connected = false;

    public BleSensorModel(@NonNull ScanResult result) {
        this.address = result.getDevice().getAddress();
        this.name = BleHelper.getName(result);
    }

    public BleSensorModel(@NonNull String address, String name) {
        this.address = address;
        this.name = name != null ? name : "-";
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public Object getDataValue(@NonNull UUID characteristic) {
        var data = mData.get(characteristic);
        return data != null ? data.getValue() : null;
    }

    public LiveData<Object> getData(@NonNull UUID characteristic) {
        var data = mData.get(characteristic);
        if (data == null) {
            data = new MutableLiveData<>();
            mData.put(characteristic, data);
        }
        return data;
    }

    public void setData(@NonNull UUID characteristic, Object newData) {
        var data = mData.get(characteristic);
        if (data == null) {
            data = new MutableLiveData<>(newData);
            mData.put(characteristic, data);
        } else {
            data.setValue(newData);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean equalsResult(ScanResult result) {
        if (result == null)
            return false;
        var otherDevAddress = result.getDevice().getAddress();
        return address.equals(otherDevAddress);
    }
}
