package rocks.tbog.touchblue.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BleOpManager {
    private static final ConcurrentLinkedQueue<IBleDeviceOperation> opQueue = new ConcurrentLinkedQueue<>();
    private static IBleDeviceOperation pendingOperation = null;

    private static final String TAG = BleOpManager.class.getSimpleName();

    private BleOpManager() {
        // don't instantiate me!
    }

    public static void enqueueOperation(IBleDeviceOperation operation) {
        synchronized (BleOpManager.class) {
            opQueue.add(operation);
            if (pendingOperation != null)
                return;
            nextOperation();
        }
        executePending();
    }

    private static void nextOperation() {
        if (pendingOperation != null) {
            Log.e(TAG, "doNextOperation() called when an operation is pending! Aborting.");
            return;
        }
        var op = opQueue.poll();
        if (op == null) {
            Log.i(TAG, "Operation queue is empty, returning");
            return;
        }

        pendingOperation = op;
    }

    private static void donePending() {
        pendingOperation.finished();
        synchronized (BleOpManager.class) {
            pendingOperation = null;
            nextOperation();
        }
        executePending();
    }

    private static void executePending() {
        final Handler handler;
        synchronized (BleOpManager.class) {
            if (pendingOperation == null)
                return;
            handler = pendingOperation.getDevice().getHandler();
        }
        handler.post(() -> {
            synchronized (BleOpManager.class) {
                if (pendingOperation != null && !pendingOperation.execute()) {
                    donePending();
                }
            }
        });
    }

    public static void onConnectionStateChange(BleDeviceWrapper device, int status, int newState) {
        if (pendingOperation == null) {
            Log.e(TAG, "onConnectionStateChange() called when no operation is pending! device=" + device);
            return;
        }
        if (pendingOperation instanceof BleConnectOperation)
            ((BleConnectOperation) pendingOperation).setConnectionState(status, newState);

        donePending();
    }

    public static void onServicesDiscovered(BleDeviceWrapper device, int status) {
        if (pendingOperation == null) {
            Log.e(TAG, "onServicesDiscovered() called when no operation is pending! device=" + device);
            return;
        }

        if (pendingOperation instanceof BleStatusOperation)
            ((BleStatusOperation) pendingOperation).setStatus(status);

        donePending();
    }

    /**
     * Called after a read or write operation
     * @param device wrapper that got the callback
     * @param characteristic what changed
     * @param status was it a {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} or an error
     */
    public static void onCharacteristicChange(BleDeviceWrapper device, BluetoothGattCharacteristic characteristic, int status) {
        if (pendingOperation == null) {
            Log.e(TAG, "onServicesDiscovered() called when no operation is pending! device=" + device);
            return;
        }

        if (pendingOperation instanceof BleCharacteristicOperation)
            ((BleCharacteristicOperation) pendingOperation).verify(characteristic);

        if (pendingOperation instanceof BleStatusOperation)
            ((BleStatusOperation) pendingOperation).setStatus(status);

        donePending();
    }
}
