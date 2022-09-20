package rocks.tbog.touchblue;

import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;

import rocks.tbog.touchblue.helpers.BleHelper;

public class BleEntry {
    @NonNull
    private final String address;
    private String name;
    private boolean connected = false;

    public BleEntry(@NonNull ScanResult result) {
        this.address = result.getDevice().getAddress();
        this.name = BleHelper.getName(result);
    }

    public BleEntry(@NonNull String address) {
        this.address = address;
        this.name = "";
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
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
