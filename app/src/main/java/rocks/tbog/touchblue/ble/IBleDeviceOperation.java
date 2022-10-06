package rocks.tbog.touchblue.ble;

import androidx.annotation.NonNull;

interface IBleDeviceOperation {

    @NonNull
    BleDeviceWrapper getDevice();
    /**
     * @return true if we're expecting a response for this operation
     */
    boolean execute();

    /**
     * Called after the operation completed even if execute returned false
     */
    void finished();
}
