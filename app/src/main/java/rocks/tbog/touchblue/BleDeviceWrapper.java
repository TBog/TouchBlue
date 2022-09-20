package rocks.tbog.touchblue;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.UUID;

import rocks.tbog.touchblue.helpers.BleHelper;

public class BleDeviceWrapper {
    private static final String TAG = BleDeviceWrapper.class.getSimpleName();

    @NonNull
    private final BluetoothDevice device;
    private final String name;
    private BluetoothGatt bluetoothGatt = null;
    private GattCallback internalCallback = null;
    private BluetoothGattCallback gattCallback = null;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public List<BluetoothGattService> services = null;

    public BleDeviceWrapper(@NonNull ScanResult result) {
        device = result.getDevice();
        name = BleHelper.getName(result);
    }

    public BleDeviceWrapper(@NonNull BluetoothDevice bluetoothDevice) {
        device = bluetoothDevice;
        name = "-";
    }

    public String getAddress() {
        return device.getAddress();
    }

    public String getName() {
        return name;
    }

    public boolean isConnected(BluetoothManager bluetoothManager) {
        if (bluetoothGatt == null || mConnectionState == STATE_DISCONNECTED)
            return false;
        @SuppressLint("MissingPermission")
        var connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        for (var connectedDevice : connectedDevices) {
            if (connectedDevice.equals(device))
                return true;
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public BluetoothGatt connectGatt(@NonNull Context ctx, @Nullable BluetoothGattCallback callback, @Nullable Handler callbackHandler) {
        gattCallback = callback;
        if (bluetoothGatt != null && bluetoothGatt.connect()) {
            Log.i(TAG, "re-connected to " + getAddress());
            mConnectionState = STATE_CONNECTED;
            return bluetoothGatt;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Handler handler = callbackHandler;
            if (handler == null)
                handler = new Handler(Looper.getMainLooper());
            // starting with SDK 27 we can set the thread of the callback using a handler
            bluetoothGatt = device.connectGatt(ctx, false, getInternalCallback(), BluetoothDevice.TRANSPORT_AUTO, BluetoothDevice.PHY_LE_1M_MASK, handler);
        } else {
            bluetoothGatt = device.connectGatt(ctx, false, getInternalCallback());
        }
        mConnectionState = STATE_CONNECTING;
        return bluetoothGatt;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (bluetoothGatt == null)
            Log.w(TAG, "disconnect called but we're not connected " + getAddress());
        if (mConnectionState == STATE_DISCONNECTED)
            Log.w(TAG, "disconnect called when state disconnected " + getAddress());
        bluetoothGatt.disconnect();
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (bluetoothGatt == null)
            Log.w(TAG, "close called but we're not connected " + getAddress());
        if (mConnectionState == STATE_DISCONNECTED)
            Log.w(TAG, "close called when state disconnected " + getAddress());
        bluetoothGatt.close();
    }

    public boolean equalsResult(ScanResult result) {
        if (result == null)
            return false;
        return getAddress().equals(result.getDevice().getAddress());
    }

    @NonNull
    private BluetoothGattCallback getInternalCallback() {
        if (internalCallback == null)
            internalCallback = new GattCallback(this);
        return internalCallback;
    }

    @SuppressLint("MissingPermission")
    public boolean writeCharacteristic(UUID service, UUID characteristic, int newValue) {
        var serviceCharacteristic = getCharacteristic(service, characteristic);
        if (serviceCharacteristic == null) {
            Log.e(TAG, "characteristic not found!");
            return false;
        }

        byte[] value = new byte[1];
        value[0] = (byte) (newValue & 0xFF);
        serviceCharacteristic.setValue(value);
        return bluetoothGatt.writeCharacteristic(serviceCharacteristic);
    }

    @SuppressLint("MissingPermission")
    public boolean readCharacteristic(UUID service, UUID characteristic) {
        var serviceCharacteristic = getCharacteristic(service, characteristic);
        if (serviceCharacteristic == null) {
            Log.e(TAG, "characteristic not found!");
            return false;
        }
        return bluetoothGatt.readCharacteristic(serviceCharacteristic);
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
        private final BleDeviceWrapper mEntry;

        public GattCallback(BleDeviceWrapper entry) {
            mEntry = entry;
            mEntry.internalCallback = this;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            var deviceAddress = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Successfully connected to " + deviceAddress);
                    mEntry.bluetoothGatt = gatt;
                    mEntry.mConnectionState = STATE_CONNECTED;
                    new Handler(Looper.getMainLooper()).post(gatt::discoverServices);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from " + deviceAddress);
                    gatt.close();
                    mEntry.bluetoothGatt = null;
                    mEntry.mConnectionState = STATE_DISCONNECTED;
                }
            } else {
                Log.w(TAG, "Error 0x" + Integer.toHexString(status) + " encountered for " + deviceAddress + "! Disconnecting...");
                gatt.close();
                mEntry.bluetoothGatt = null;
                mEntry.mConnectionState = STATE_DISCONNECTED;
            }

            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onConnectionStateChange(gatt, status, newState);
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

            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (mEntry.gattCallback != null)
                mEntry.gattCallback.onMtuChanged(gatt, mtu, status);
        }
    }
}
