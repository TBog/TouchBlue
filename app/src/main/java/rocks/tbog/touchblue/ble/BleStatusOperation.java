package rocks.tbog.touchblue.ble;

import androidx.annotation.NonNull;

public abstract class BleStatusOperation extends BleDeviceOperation {
    protected int mStatus = 0x85; // GATT_ERROR = 0x85 = 133

    protected BleStatusOperation(@NonNull BleDeviceWrapper deviceWrapper) {
        super(deviceWrapper);
    }

    public void setStatus(int status) {
        mStatus = status;
    }
}
