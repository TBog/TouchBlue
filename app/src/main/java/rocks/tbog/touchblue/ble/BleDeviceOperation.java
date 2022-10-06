package rocks.tbog.touchblue.ble;

import androidx.annotation.NonNull;

public abstract class BleDeviceOperation implements IBleDeviceOperation {
    @NonNull
    protected final BleDeviceWrapper mDevice;

    protected BleDeviceOperation(@NonNull BleDeviceWrapper deviceWrapper) {
        mDevice = deviceWrapper;
    }

    @NonNull
    @Override
    public BleDeviceWrapper getDevice() {
        return mDevice;
    }

}
