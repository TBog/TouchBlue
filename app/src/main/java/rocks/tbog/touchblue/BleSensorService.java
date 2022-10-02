package rocks.tbog.touchblue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import rocks.tbog.touchblue.helpers.GattAttributes;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleSensorService extends Service {
    private final static String TAG = BleSensorService.class.getSimpleName();
    private static final int ONGOING_NOTIFICATION_ID = 666;        // Notification ID cannot be 0.
    private static final String ONGOING_NOTIFICATION_CHANNEL = "ongoing";

    private BluetoothManager mBluetoothManager;
    private final List<BleDeviceWrapper> mDevices = new ArrayList<>(0);
    private Handler mHandler;

    // broadcast/receiver actions
    public final static String ACTION_GATT_CONNECTED = "rocks.tbog.touchblue.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "rocks.tbog.touchblue.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "rocks.tbog.touchblue.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_CHANGED = "rocks.tbog.touchblue.ACTION_DATA_CHANGED";
    public final static String ACTION_START_SERVICE = "rocks.tbog.touchblue.ACTION_START_SERVICE";
    public final static String ACTION_STOP_SERVICE = "rocks.tbog.touchblue.ACTION_STOP_SERVICE";
    public final static String ACTION_CONNECT_TO = "rocks.tbog.touchblue.ACTION_CONNECT_TO";
    public final static String ACTION_TOGGLE_LED = "rocks.tbog.touchblue.ACTION_TOGGLE_LED";
    public final static String ACTION_REQUEST_DATA = "rocks.tbog.touchblue.ACTION_REQUEST_DATA";
    public final static String ACTION_SET_DATA = "rocks.tbog.touchblue.ACTION_SET_DATA";
    // extra information sent
    public final static String EXTRA_DATA = "rocks.tbog.touchblue.EXTRA_DATA"; // characteristic value
    public final static String EXTRA_DATA_UUID = "rocks.tbog.touchblue.EXTRA_DATA_UUID"; // characteristic uuid
    public final static String EXTRA_SERVICE_UUID = "rocks.tbog.touchblue.EXTRA_SERVICE_UUID";
    public final static String EXTRA_ADDRESS = "rocks.tbog.touchblue.EXTRA_ADDRESS"; // device address
    public final static String EXTRA_DEVICE_NAME = "rocks.tbog.touchblue.EXTRA_DEVICE_NAME";
    public final static String EXTRA_GATT_SERVICES = "rocks.tbog.touchblue.EXTRA_GATT_SERVICES";

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(GattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_LED_SWITCH = UUID.fromString(GattAttributes.LED_SWITCH);
    public final static UUID UUID_LED_BRIGHTNESS = UUID.fromString(GattAttributes.LED_BRIGHTNESS);
    public final static UUID UUID_LED_SATURATION = UUID.fromString(GattAttributes.LED_SATURATION);
    public final static UUID UUID_ACCEL_RANGE = UUID.fromString(GattAttributes.ACCEL_RANGE);
    public final static UUID UUID_ACCEL_BANDWIDTH = UUID.fromString(GattAttributes.ACCEL_BANDWIDTH);
    public final static UUID UUID_ACCEL_SAMPLE_RATE = UUID.fromString(GattAttributes.ACCEL_SAMPLE_RATE);

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction = null;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
            }

            if (intentAction != null)
                broadcastUpdate(intentAction, gatt.getDevice());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, gatt.getDevice());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_CHANGED, gatt.getDevice(), characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_CHANGED, gatt.getDevice(), characteristic);
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final var action = intent.getAction();
            Log.i(TAG, "onReceive action=" + action);
            final var extras = intent.getExtras();
            executeAction(action, extras);
        }
    };

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");

        HandlerThread handlerThread = new HandlerThread("BleService");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), msg -> {
            if (msg.what == -1) {
                mHandler.getLooper().quitSafely();
                return true;
            }
            return false;
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECT_TO);
        filter.addAction(ACTION_REQUEST_DATA);
        filter.addAction(ACTION_STOP_SERVICE);
        registerReceiver(receiver, filter);

        if (!initialize()) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        unregisterReceiver(receiver);
        disconnect();
        close();
        // stop handler thread
        mHandler.sendEmptyMessage(-1);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action;
        final Bundle extras;
        if (intent != null) {
            action = intent.getAction();
            extras = intent.getExtras();
        } else {
            action = null;
            extras = null;
        }
        Log.i(TAG, "onStartCommand action=" + action);

        var contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        var stopIntent = PendingIntent.getService(this,
                1, new Intent(this, BleSensorService.class)
                        .setAction(ACTION_STOP_SERVICE),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ONGOING_NOTIFICATION_CHANNEL,
                    ONGOING_NOTIFICATION_CHANNEL,
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat
                .Builder(this, ONGOING_NOTIFICATION_CHANNEL)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setContentIntent(contentIntent)
                .setTicker(getText(R.string.notification_message))
                .setPriority(Notification.PRIORITY_MIN)
                .addAction(android.R.drawable.ic_delete, "Stop connections", stopIntent)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        executeAction(action, extras);

        return super.onStartCommand(intent, flags, startId);
    }

    private void executeAction(@Nullable String action, @Nullable Bundle extras) {
        if (ACTION_STOP_SERVICE.equals(action)) {
            stopSelf();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        } else if (ACTION_CONNECT_TO.equals(action)) {
            String address = extras != null ? extras.getString(EXTRA_ADDRESS) : null;
            String deviceName = extras != null ? extras.getString(EXTRA_DEVICE_NAME) : null;
            if (!connect(address)) {
                Log.d(TAG, "Can't connect to " + address);
            }
            if (deviceName != null) {
                var dev = findDeviceByAddress(address);
                if (dev != null)
                    dev.setName(deviceName);
            }
        } else if (ACTION_TOGGLE_LED.equals(action)) {
            String address = extras != null ? extras.getString(EXTRA_ADDRESS) : null;
            for (var device : mDevices) {
                if (address != null && !address.equals(device.getAddress()))
                    continue;
                device.readCharacteristic(UUID_LED_SWITCH, (characteristic, status) -> {
                    if (status != BluetoothGatt.GATT_SUCCESS)
                        Log.e(TAG, "read LED switch failed with status " + status);
                    var byteArr = characteristic.getValue();
                    if (byteArr != null && byteArr.length > 0) {
                        var newValue = 1 - byteArr[0];
                        device.writeCharacteristic(characteristic, newValue, BluetoothGattCharacteristic.FORMAT_UINT8);
                    }
                });
            }
        } else if (ACTION_REQUEST_DATA.equals(action)) {
            String address = extras != null ? extras.getString(EXTRA_ADDRESS) : null;
            var uuid = extras != null ? extras.getSerializable(EXTRA_DATA_UUID) : null;
            if (uuid instanceof UUID) {
                var characteristic = (UUID) uuid;
                for (var device : mDevices) {
                    if (address != null && !address.equals(device.getAddress()))
                        continue;
                    var gattCharacteristic = device.getCachedCharacteristic(characteristic);
                    device.readCharacteristic(gattCharacteristic.getService().getUuid(), characteristic);
                }
            } else {
                // UUID is null, send services
                for (var device : mDevices) {
                    if (address != null && !address.equals(device.getAddress()))
                        continue;
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device);
                }
            }
        } else if (ACTION_SET_DATA.equals(action)) {
            String address = extras != null ? extras.getString(EXTRA_ADDRESS) : null;
            var uuid = extras != null ? extras.getSerializable(EXTRA_DATA_UUID) : null;
            var data = extras != null ? extras.get(EXTRA_DATA) : null;
            final int intData;
            if (data instanceof Integer)
                intData = (int) data;
            else {
                intData = Objects.hashCode(data);
                Log.e(TAG, "data class not Integer: " + (data != null ? data.getClass() : null));
            }

            if (uuid instanceof UUID) {
                var characteristicUUID = (UUID) uuid;
                for (var device : mDevices) {
                    if (address != null && !address.equals(device.getAddress()))
                        continue;
                    var characteristic = device.getCachedCharacteristic(characteristicUUID);
                    if (characteristic == null) {
                        Log.e(TAG, "characteristic `" + characteristicUUID + "` not found for device `" + device.getAddress() + "`");
                        continue;
                    }
                    var serviceUUID = characteristic.getService().getUuid();
                    device.writeCharacteristic(serviceUUID, characteristicUUID, intData, getCharacteristicFormat(characteristicUUID));
                    broadcastUpdate(device, characteristic);
                }
            }
        }
    }

    private void broadcastUpdate(@NonNull final String action, @NonNull final BluetoothDevice device) {
        var dev = findDeviceByAddress(device.getAddress());
        if (dev == null) {
            Log.e(TAG, "device `" + device.getAddress() + "` not registered");
            return;
        }
        broadcastUpdate(action, dev);
    }

    private void broadcastUpdate(@NonNull final String action, @NonNull final BleDeviceWrapper device) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, device.getAddress());
        if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            var arrServices = device.services.toArray(new BluetoothGattService[0]);
            intent.putExtra(EXTRA_GATT_SERVICES, arrServices);
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final BleDeviceWrapper device, final BluetoothGattCharacteristic characteristic) {
        broadcastUpdate(ACTION_DATA_CHANGED, device, characteristic);
    }

    private void broadcastUpdate(final String action, final BluetoothDevice device, final BluetoothGattCharacteristic characteristic) {
        var dev = findDeviceByAddress(device.getAddress());
        if (dev == null) {
            Log.e(TAG, "device `" + device.getAddress() + "` not registered");
            return;
        }
        broadcastUpdate(action, dev, characteristic);
    }

    private void broadcastUpdate(final String action, final BleDeviceWrapper device, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, device.getAddress());
        intent.putExtra(EXTRA_DEVICE_NAME, device.getName());

        broadcastUpdate(intent, characteristic);
    }

    private void broadcastUpdate(Intent intent, final BluetoothGattCharacteristic characteristic) {
        intent.putExtra(EXTRA_SERVICE_UUID, characteristic.getService().getUuid());
        intent.putExtra(EXTRA_DATA_UUID, characteristic.getUuid());
        int valueFormatType = getCharacteristicFormat(characteristic.getUuid());
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if (valueFormatType == BluetoothGattCharacteristic.FORMAT_UINT8 ||
                valueFormatType == BluetoothGattCharacteristic.FORMAT_UINT16 ||
                valueFormatType == BluetoothGattCharacteristic.FORMAT_UINT32) {
            var data = characteristic.getIntValue(valueFormatType, 0);
            if (data != null) {
                intent.putExtra(EXTRA_DATA, data);
            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Bluetooth permission not granted. Can't initialize.");
            return false;
        }

        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        for (var device : mDevices) {
            device.connectGatt(this, mGattCallback, mHandler);
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Bluetooth permission not granted.");
            return false;
        }

        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.w(TAG, "Bluetooth invalid address `" + address + "`.");
            return false;
        }

        var dev = findDeviceByAddress(address);
        if (dev != null) {
            boolean isConnected = dev.isConnected(mBluetoothManager);
            Log.i(TAG, address + " isConnected=" + isConnected);
            if (isConnected)
                return true;
            dev.connectGatt(this, mGattCallback, mHandler);
            return true;
        }

        var bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        var bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        var device = new BleDeviceWrapper(bluetoothDevice);
        mDevices.add(device);

        device.connectGatt(this, mGattCallback, mHandler);
        return true;
    }

    private BleDeviceWrapper findDeviceByAddress(String address) {
        for (var dev : mDevices) {
            if (dev.getAddress().equals(address)) {
                return dev;
            }
        }
        return null;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        Log.i(TAG, "disconnect");
        for (var device : mDevices)
            device.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are released properly.
     */
    @SuppressLint("MissingPermission")
    public void close() {
        Log.i(TAG, "close");
        for (var device : mDevices)
            device.close();
    }

    private static int getCharacteristicFormat(UUID characteristic) {
        if (UUID_ACCEL_BANDWIDTH.equals(characteristic)) {
            return BluetoothGattCharacteristic.FORMAT_UINT16;
        } else if (UUID_ACCEL_SAMPLE_RATE.equals(characteristic)) {
            return BluetoothGattCharacteristic.FORMAT_UINT16;
        }
        return BluetoothGattCharacteristic.FORMAT_UINT8;
    }
}
