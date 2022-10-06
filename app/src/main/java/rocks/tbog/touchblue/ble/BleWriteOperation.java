package rocks.tbog.touchblue.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BleWriteOperation extends BleCharacteristicOperation {
    protected final ICharacteristicCallback mCallbackBeforeExecute;

    public BleWriteOperation(@NonNull BleDeviceWrapper deviceWrapper, @NonNull BluetoothGattCharacteristic characteristic, @Nullable IStatusCharacteristicCallback onDoneCallback) {
        super(deviceWrapper, characteristic, onDoneCallback);
        mCallbackBeforeExecute = null;
    }

    public BleWriteOperation(@NonNull BleDeviceWrapper deviceWrapper, @NonNull BluetoothGattCharacteristic characteristic, @NonNull ICharacteristicCallback onWriteCallback, @Nullable IStatusCharacteristicCallback onDoneCallback) {
        super(deviceWrapper, characteristic, onDoneCallback);
        mCallbackBeforeExecute = onWriteCallback;
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean execute() {
        boolean ok = false;
        if (mDevice.isConnected()) {
            if (mCallbackBeforeExecute != null)
                mCallbackBeforeExecute.onCharacteristicCallback(mCharacteristic);
            ok = getDevice().requireGatt().writeCharacteristic(mCharacteristic);
        }
        if (!ok)
            setStatus(0x0a);// GATT_NOT_FOUND = 0x0a
        return ok;
    }
}
