package rocks.tbog.touchblue.ble;

import android.bluetooth.BluetoothGattCharacteristic;

public interface IStatusCharacteristicCallback {
    void onCharacteristicCallback(BluetoothGattCharacteristic characteristic, int status);
}
