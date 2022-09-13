package rocks.tbog.touchblue;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

public class BleEntry {
    @NonNull
    private final ScanResult scanResult;
    public List<BluetoothGattService> services = null;
    public BluetoothGatt bluetoothGatt = null;
    private GattCallback gattCallback = null;

    public BleEntry(@NonNull ScanResult result) {
        scanResult = result;
    }

    public String getAddress() {
        return scanResult.getDevice().getAddress();
    }

    @NonNull
    public ScanResult getScanResult() {
        return scanResult;
    }

    public boolean equalsResult(ScanResult result) {
        if (scanResult.equals(result))
            return true;
        if (result == null)
            return false;
        var thisDevAddress = scanResult.getDevice().getAddress();
        var otherDevAddress = result.getDevice().getAddress();
        return thisDevAddress.equals(otherDevAddress);
    }

    @NonNull
    public BluetoothGattCallback getCallback() {
        if (gattCallback == null)
            gattCallback = new GattCallback(this);
        return gattCallback;
    }

    public static class GattCallback extends BluetoothGattCallback {
        private static final String TAG = GattCallback.class.getSimpleName();
        private final BleEntry mEntry;

        public GattCallback(BleEntry entry) {
            mEntry = entry;
            mEntry.gattCallback = this;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            var deviceAddress = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Successfully connected to " + deviceAddress);
                    new Handler(Looper.getMainLooper()).post(gatt::discoverServices);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Successfully disconnected from " + deviceAddress);
                    gatt.close();
                }
            } else {
                Log.w(TAG, "Error 0x" + Integer.toHexString(status) + " encountered for " + deviceAddress + "! Disconnecting...");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            var deviceAddress = gatt.getDevice().getAddress();
            Log.i(TAG, "ServicesDiscovered status=" + status + " for " + deviceAddress);
            var services = gatt.getServices();
            var stringBuilder = new StringBuilder();
            stringBuilder
                    .append("device ")
                    .append(deviceAddress)
                    .append(" has:\n");
            for (var service : services) {
                stringBuilder
                        .append("service ")
                        .append(service.getUuid())
                        .append(" with ")
                        .append(service.getCharacteristics().size())
                        .append(" characteristic(s) and ")
                        .append(service.getIncludedServices().size())
                        .append(" included service(s)\n");
            }
            Log.i(TAG, stringBuilder.toString());
            mEntry.services = services;
        }
    }
}
