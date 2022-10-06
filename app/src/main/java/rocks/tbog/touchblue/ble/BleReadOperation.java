package rocks.tbog.touchblue.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BleReadOperation extends BleCharacteristicOperation {

    public BleReadOperation(@NonNull BleDeviceWrapper deviceWrapper, @NonNull BluetoothGattCharacteristic characteristic, @Nullable IStatusCharacteristicCallback onDoneCallback) {
        super(deviceWrapper, characteristic, onDoneCallback);
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean execute() {
        boolean ok = false;
        if (mDevice.isConnected())
            ok = mDevice.requireGatt().readCharacteristic(mCharacteristic);
        if (!ok)
            setStatus(0x0a);// GATT_NOT_FOUND = 0x0a
        return ok;
    }
}
