package rocks.tbog.touchblue;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;

public class BleEntry {
    private static final String TAG = BleEntry.class.getSimpleName();

    @NonNull
    private final ScanResult scanResult;
    private BluetoothGatt bluetoothGatt = null;
    private GattCallback gattCallback = null;

    public List<BluetoothGattService> services = null;

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

    @SuppressLint("MissingPermission")
    public boolean writeCharacteristic(UUID service, UUID characteristic, int newValue) {
        //check mBluetoothGatt is available
        if (bluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        BluetoothGattService Service = bluetoothGatt.getService(service);
        if (Service == null) {
            Log.e(TAG, "service not found!");
            return false;
        }
        var serviceCharacteristic = Service.getCharacteristic(characteristic);
        if (serviceCharacteristic == null) {
            Log.e(TAG, "characteristic not found!");
            return false;
        }

        byte[] value = new byte[1];
        value[0] = (byte) (newValue & 0xFF);
        serviceCharacteristic.setValue(value);
        return bluetoothGatt.writeCharacteristic(serviceCharacteristic);
    }

    public BluetoothGattCharacteristic getCharacteristic(UUID service, UUID characteristic) {
        //check mBluetoothGatt is available
        if (bluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return null;
        }
        BluetoothGattService Service = bluetoothGatt.getService(service);
        if (Service == null) {
            Log.e(TAG, "service not found!");
            return null;
        }
        var serviceCharacteristic = Service.getCharacteristic(characteristic);
        if (serviceCharacteristic == null) {
            Log.e(TAG, "characteristic not found!");
            return null;
        }

        return serviceCharacteristic;
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
                    mEntry.bluetoothGatt = gatt;
                    new Handler(Looper.getMainLooper()).post(gatt::discoverServices);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Successfully disconnected from " + deviceAddress);
                    gatt.close();
                    mEntry.bluetoothGatt = null;
                }
            } else {
                Log.w(TAG, "Error 0x" + Integer.toHexString(status) + " encountered for " + deviceAddress + "! Disconnecting...");
                gatt.close();
                mEntry.bluetoothGatt = null;
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
