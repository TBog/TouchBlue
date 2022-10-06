package rocks.tbog.touchblue.ble;

import android.bluetooth.BluetoothGatt;

public interface IDeviceCallback {
    void callback(BleDeviceWrapper device, BluetoothGatt bluetoothGatt);
}
