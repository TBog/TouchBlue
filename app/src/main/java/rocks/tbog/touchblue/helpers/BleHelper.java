package rocks.tbog.touchblue.helpers;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.companion.BluetoothDeviceFilter;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

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

    @SuppressLint("MissingPermission")
    @NonNull
    public static String getName(@NonNull ScanResult result) {
        final var scanRec = result.getScanRecord();
        final var dev = result.getDevice();
        String name = scanRec.getDeviceName();
        if (name == null || name.isEmpty()) {
            //if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
            name = dev.getName();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (name == null || name.isEmpty()) {
                name = dev.getAlias();
            }
        }
        if (name == null || name.isEmpty()) {
            name = "-";
        }
        return name;
    }
}
