package rocks.tbog.touchblue.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class BleCharacteristicOperation extends BleStatusOperation {
    private static final String TAG = BleCharacteristicOperation.class.getSimpleName();
    protected final BluetoothGattCharacteristic mCharacteristic;
    protected final IStatusCharacteristicCallback mCallbackOnDone;

    public BleCharacteristicOperation(@NonNull BleDeviceWrapper deviceWrapper, @NonNull BluetoothGattCharacteristic characteristic, @Nullable IStatusCharacteristicCallback onDoneCallback) {
        super(deviceWrapper);
        mCharacteristic = characteristic;
        mCallbackOnDone = onDoneCallback;
    }

    @Override
    public void finished() {
        if (!mDevice.isConnected())
            Log.e(TAG, "device is not connected " + this);
        if (mCallbackOnDone != null)
            mCallbackOnDone.onCharacteristicCallback(mCharacteristic, mStatus);
    }

    public void verify(BluetoothGattCharacteristic characteristic) {
        if (!characteristic.equals(mCharacteristic)) {
            throw new IllegalStateException(characteristic.getUuid() + " != " + mCharacteristic.getUuid());
        }
    }
}
