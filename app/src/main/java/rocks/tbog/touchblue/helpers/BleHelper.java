package rocks.tbog.touchblue.helpers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.companion.BluetoothDeviceFilter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public class BleHelper {
    private static final String TAG = BleHelper.class.getSimpleName();

    private BleHelper() {
        // don't instantiate me!
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static BleScan startScan() {
        var filter = new ScanFilter.Builder().build();
        var deviceFilter = new BluetoothDeviceFilter.Builder().build();
        return new BleScan();
    }

    public static BleScan startScan2(Context context) throws IllegalStateException {
        var bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = null;
        if (bluetoothManager instanceof BluetoothManager)
            bluetoothAdapter = ((BluetoothManager) bluetoothManager).getAdapter();
        if (bluetoothAdapter == null) {
            throw new IllegalStateException("BluetoothAdapter is null");
        }
        var bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            throw new IllegalStateException("BluetoothLeScanner is null");
        }

        var scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0)
                .build();

        final BleScan bleScan = new BleScan(bleScanner);
        if (!bleScan.startScan(context, scanSettings))
            throw new IllegalStateException("Can't start the scan; missing permission?");

        return bleScan;
    }

    public static void connect(Context ctx, ScanResult result) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ctx, "Permission needed", Toast.LENGTH_SHORT).show();
            return;
        }
        result.getDevice().connectGatt(ctx, false, new GattCallback());
    }

    private static class GattCallback extends BluetoothGattCallback {
        private static final String TAG = GattCallback.class.getSimpleName();

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
        }
    }
}
