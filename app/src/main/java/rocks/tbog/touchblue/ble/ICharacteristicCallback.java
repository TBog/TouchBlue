package rocks.tbog.touchblue.ble;

import android.bluetooth.BluetoothGattCharacteristic;

public interface ICharacteristicCallback {
    void onCharacteristicCallback(BluetoothGattCharacteristic characteristic);
}
