package rocks.tbog.touchblue.ble;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import rocks.tbog.touchblue.helpers.GattAttributes;

public class BleDeviceWrapper {
    private static final String TAG = BleDeviceWrapper.class.getSimpleName();

    @NonNull
    private final BluetoothDevice mDevice;
    private String name;
    private BluetoothGatt bluetoothGatt = null;
    private HashMap<UUID, BluetoothGattCharacteristic> mCharacteristics = null;
    private GattCallback internalCallback = null;
    private BluetoothGattCallback gattCallback = null;
    private State mConnectionState = State.STATE_DISCONNECTED;

    enum State {
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_DISCOVERING_SERVICES,
        STATE_CONNECTED
    }

    private Handler mHandler = null;

    public BleDeviceWrapper(@NonNull BluetoothDevice bluetoothDevice) {
        mDevice = bluetoothDevice;
        name = "-";
    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public void setName(String deviceName) {
        name = deviceName;
    }

    public String getName() {
        return name;
    }

    @NonNull
    public BluetoothGatt requireGatt() {
        if (bluetoothGatt == null)
            throw new IllegalStateException("null gatt for " + this);
        return bluetoothGatt;
    }

    @NonNull
    public Handler getHandler() {
        if (mHandler == null)
            mHandler = new Handler(Looper.getMainLooper());

        return mHandler;
    }

    public boolean isConnected() {
        return bluetoothGatt != null && mConnectionState == State.STATE_CONNECTED;
    }

    public boolean isConnected(BluetoothManager bluetoothManager) {
        if (bluetoothGatt == null || mConnectionState != State.STATE_CONNECTED)
            return false;
        @SuppressLint("MissingPermission")
        var connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        for (var connectedDevice : connectedDevices) {
            if (connectedDevice.equals(mDevice))
                return true;
        }
        return false;
    }

    @NonNull
    public List<BluetoothGattService> getServices() {
        if (bluetoothGatt == null || mConnectionState != State.STATE_CONNECTED)
            return Collections.emptyList();
        return bluetoothGatt.getServices();
    }

    private static class ConnectOperation extends BleConnectOperation {
        private final Context ctx;

        public ConnectOperation(Context context, BleDeviceWrapper device) {
            super(device);
            ctx = context;
        }

        @SuppressLint("MissingPermission")
        @Override
        public boolean execute() {
            var deviceWrapper = getDevice();
            final var gattCB = deviceWrapper.getInternalCallback();
            final var device = deviceWrapper.mDevice;
            BluetoothGatt bluetoothGatt;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // starting with SDK 27 we can set the thread of the callback using a handler
                bluetoothGatt = device.connectGatt(ctx, false, gattCB, BluetoothDevice.TRANSPORT_AUTO, BluetoothDevice.PHY_LE_1M_MASK, deviceWrapper.getHandler());
            } else {
                bluetoothGatt = device.connectGatt(ctx, false, gattCB);
            }
            deviceWrapper.bluetoothGatt = bluetoothGatt;
            return true;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void finished() {
            var status = this.mStatus;
            var newState = this.mState;
            var device = getDevice();
            var deviceAddress = device.getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Successfully connected to " + deviceAddress);
                    device.mConnectionState = State.STATE_DISCOVERING_SERVICES;
                    //Note: DiscoverServicesOperation should already be in the operation queue
                    //device.addOperation(new DiscoverServicesOperation(device));
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from " + deviceAddress);
                    var deviceWrapper = getDevice();
                    deviceWrapper.bluetoothGatt.close();
                    deviceWrapper.bluetoothGatt = null;
                    deviceWrapper.mConnectionState = State.STATE_DISCONNECTED;
                }
            } else {
                Log.w(TAG, "Error 0x" + Integer.toHexString(status) + " encountered for " + deviceAddress + "! Disconnecting...");
                var deviceWrapper = getDevice();
                deviceWrapper.bluetoothGatt.close();
                deviceWrapper.bluetoothGatt = null;
                deviceWrapper.mConnectionState = State.STATE_DISCONNECTED;
            }
        }
    }

    private static class DiscoverServicesOperation extends BleStatusOperation {

        protected DiscoverServicesOperation(@NonNull BleDeviceWrapper deviceWrapper) {
            super(deviceWrapper);
        }

        @SuppressLint("MissingPermission")
        @Override
        public boolean execute() {
            if (getDevice().mConnectionState == State.STATE_DISCOVERING_SERVICES)
                return getDevice().requireGatt().discoverServices();
            setStatus(BluetoothGatt.GATT_FAILURE);
            return false;
        }

        @Override
        public void finished() {
            var deviceWrapper = getDevice();
            var deviceAddress = deviceWrapper.getAddress();
            Log.i(TAG, "ServicesDiscovered status=" + mStatus + " for " + deviceAddress);
            if (mStatus != BluetoothGatt.GATT_SUCCESS)
                return;
            deviceWrapper.mConnectionState = State.STATE_CONNECTED;
            var services = deviceWrapper.getServices();
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
        }
    }

    @SuppressLint("MissingPermission")
    public void connectGatt(@NonNull Context ctx, @Nullable BluetoothGattCallback callback, @Nullable Handler callbackHandler) {
        mCharacteristics = null;
        if (callbackHandler != null)
            mHandler = callbackHandler;
        gattCallback = callback;
        if (bluetoothGatt != null && bluetoothGatt.connect()) {
            Log.i(TAG, "re-connected to " + getAddress());
            mConnectionState = State.STATE_CONNECTED;
            return;
        }
        mConnectionState = State.STATE_CONNECTING;
        addOperation(new ConnectOperation(ctx, this));
        addOperation(new DiscoverServicesOperation(this));
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mConnectionState != State.STATE_CONNECTED)
            Log.w(TAG, "disconnect called when state (" + mConnectionState + ") " + getAddress());

        if (bluetoothGatt == null)
            Log.w(TAG, "disconnect called but we're not connected " + getAddress());
        else
            bluetoothGatt.disconnect();

        mCharacteristics = null;
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (mConnectionState != State.STATE_CONNECTED)
            Log.w(TAG, "close called when state " + mConnectionState + " " + getAddress());

        if (bluetoothGatt == null)
            Log.w(TAG, "close called but we're not connected " + getAddress());
        else
            bluetoothGatt.close();

        mCharacteristics = null;
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

    public void addOperation(@NonNull IBleDeviceOperation operation) {
        BleOpManager.enqueueOperation(operation);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic serviceCharacteristic, int newValue, int formatType, @Nullable IStatusCharacteristicCallback callback) {
        var properties = serviceCharacteristic != null ? serviceCharacteristic.getProperties() : 0;
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                && (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            return false;
        }
        addOperation(new BleWriteOperation(this, serviceCharacteristic, (characteristic) -> characteristic.setValue(newValue, formatType, 0), callback));
        return true;
    }

    public boolean writeCharacteristic(UUID service, UUID characteristic, int newValue, int formatType, @Nullable IStatusCharacteristicCallback callback) {
        var serviceCharacteristic = getCharacteristic(service, characteristic);
        return writeCharacteristic(serviceCharacteristic, newValue, formatType, callback);
    }

    public boolean writeCharacteristic(UUID characteristic, int newValue, int formatType, @Nullable IStatusCharacteristicCallback callback) {
        var serviceCharacteristic = getCachedCharacteristic(characteristic);
        return writeCharacteristic(serviceCharacteristic, newValue, formatType, callback);
    }

    public boolean readCharacteristic(@Nullable BluetoothGattCharacteristic serviceCharacteristic, @Nullable IStatusCharacteristicCallback callback) {
        var properties = serviceCharacteristic != null ? serviceCharacteristic.getProperties() : 0;
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            return false;
        }
        addOperation(new BleReadOperation(this, serviceCharacteristic, callback));
        return true;
    }

    public boolean readCharacteristic(UUID service, UUID characteristic) {
        var serviceCharacteristic = getCharacteristic(service, characteristic);
        return readCharacteristic(serviceCharacteristic, null);
    }

    public boolean readCharacteristic(UUID characteristic) {
        var serviceCharacteristic = getCachedCharacteristic(characteristic);
        return readCharacteristic(serviceCharacteristic, null);
    }

    public boolean readCharacteristic(UUID characteristic, IStatusCharacteristicCallback callback) {
        var serviceCharacteristic = getCachedCharacteristic(characteristic);
        return readCharacteristic(serviceCharacteristic, callback);
    }

    @SuppressLint("MissingPermission")
    public boolean enableCharacteristicNotification(UUID characteristicUUID, boolean bEnable) {
        //check mBluetoothGatt is available
        if (bluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        var characteristic = getCachedCharacteristic(characteristicUUID);
        if (characteristic == null) {
            Log.e(TAG, "characteristic not found");
            return false;
        }

        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            Log.e(TAG, "characteristic doesn't have notify property (" + characteristic.getProperties() + ")");
            return false;
        }

        // enable notification callback
        bluetoothGatt.setCharacteristicNotification(characteristic, bEnable);

        // tell device that we're expecting notifications
        var descriptor = characteristic.getDescriptor(GattAttributes.CCCD);
        if (bEnable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return bluetoothGatt.writeDescriptor(descriptor);
    }

    private void generateCharacteristicCache() {
        // cache all characteristics
        if (mCharacteristics != null) {
            return;
        }
        var gattServices = getServices();
        if (!gattServices.isEmpty()) {
            mCharacteristics = new HashMap<>();
            for (var service : gattServices) {
                var characteristics = service.getCharacteristics();
                for (var characteristic : characteristics) {
                    mCharacteristics.put(characteristic.getUuid(), characteristic);
                }
            }
        }
    }

    @Nullable
    public Collection<BluetoothGattCharacteristic> getCachedCharacteristics() {
        //check mBluetoothGatt is available
        if (bluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return null;
        }
        generateCharacteristicCache();
        return mCharacteristics != null ? mCharacteristics.values() : null;
    }

    /**
     * Will return the last characteristic found with the provided UUID
     *
     * @param characteristicUUID the UUID of the desired characteristic
     * @return null if not found
     */
    @Nullable
    public BluetoothGattCharacteristic getCachedCharacteristic(UUID characteristicUUID) {
        //check mBluetoothGatt is available
        if (bluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return null;
        }
        generateCharacteristicCache();
        return mCharacteristics != null ? mCharacteristics.get(characteristicUUID) : null;
    }

    @Nullable
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
        private final BleDeviceWrapper mDeviceWrapper;

        public GattCallback(BleDeviceWrapper deviceWrapper) {
            mDeviceWrapper = deviceWrapper;
            mDeviceWrapper.internalCallback = this;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange");
            BleOpManager.onConnectionStateChange(mDeviceWrapper, status, newState);

            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered");
            BleOpManager.onServicesDiscovered(mDeviceWrapper, status);

            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");
            BleOpManager.onCharacteristicChange(mDeviceWrapper, characteristic, status);

            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite");
            BleOpManager.onCharacteristicChange(mDeviceWrapper, characteristic, status);

            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
            // characteristic notification, no pending operation

            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead");
            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite");
            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onReliableWriteCompleted");
            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "onReadRemoteRssi " + rssi);
            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged " + mtu);
            if (mDeviceWrapper.gattCallback != null)
                mDeviceWrapper.gattCallback.onMtuChanged(gatt, mtu, status);
        }
    }
}
