package rocks.tbog.touchblue.ble;

import androidx.annotation.NonNull;

public abstract class BleConnectOperation extends BleDeviceOperation {
    protected int mStatus;
    protected int mState;

    protected BleConnectOperation(@NonNull BleDeviceWrapper deviceWrapper) {
        super(deviceWrapper);
    }

    /**
     * Called before {@link BleConnectOperation#finished} to set the status and state of the connection
     *
     * @param status Status of the connect or disconnect operation. BluetoothGatt.GATT_SUCCESS if the operation succeeds.
     * @param state  the new connection state. Can be one of BluetoothProfile.STATE_DISCONNECTED or BluetoothProfile.STATE_CONNECTED
     */
    public void setConnectionState(int status, int state) {
        mStatus = status;
        mState = state;
    }
}
