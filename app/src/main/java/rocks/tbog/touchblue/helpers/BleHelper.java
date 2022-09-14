package rocks.tbog.touchblue.helpers;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.companion.BluetoothDeviceFilter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import rocks.tbog.touchblue.BleEntry;

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

    public static void connect(Context ctx, BleEntry entry) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ctx, "Permission needed", Toast.LENGTH_SHORT).show();
            return;
        }
        var device = entry.getScanResult().getDevice();
        device.connectGatt(ctx, false, entry.getCallback());
    }

}
