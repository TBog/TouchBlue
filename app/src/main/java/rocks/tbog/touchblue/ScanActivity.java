package rocks.tbog.touchblue;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import rocks.tbog.touchblue.databinding.ActivityScanBinding;
import rocks.tbog.touchblue.helpers.BleHelper;
import rocks.tbog.touchblue.helpers.BleScan;

public class ScanActivity extends AppCompatActivity {
    private static final String TAG = ScanActivity.class.getSimpleName();
    public static final String SCAN_RESULT = "bluetooth_search_result";

    private ActivityScanBinding binding;
    private ActivityResultLauncher<Intent> promptEnableBluetooth;
    private ActivityResultLauncher<String[]> requestMultiplePermissions;
    private final MutableLiveData<Boolean> bluetoothPermissionReceived = new MutableLiveData<>();
    private boolean bluetoothPermissionAsked = false;
    private BleScan mScan = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        var actionbar = getSupportActionBar();
        if (actionbar != null)
            actionbar.setTitle(R.string.activity_scan);

        promptEnableBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            bluetoothPermissionReceived.setValue(result.getResultCode() == RESULT_OK);
        });

        requestMultiplePermissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allReceived = true;
            for (var entry : result.entrySet()) {
                Log.i(TAG, entry.getKey() + " " + entry.getValue());
                if (!entry.getValue()) {
                    allReceived = false;
                }
            }
            bluetoothPermissionReceived.setValue(allReceived);
        });

        bluetoothPermissionReceived.observe(this, bltEnabled -> {
            binding.textDebug.setText(bltEnabled ? "BLT enabled" : "Need BLT permission");
        });

        if (savedInstanceState != null) {
            bluetoothPermissionAsked = savedInstanceState.getBoolean("bluetoothPermissionAsked");
        }


        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        var adapter = new RecycleBleAdapter();
        binding.list.setAdapter(adapter);
        binding.list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        adapter.setOnItemClickListener((entry, position) -> {
            var data = new Intent();
            data.putExtra(SCAN_RESULT, entry.getScanResult());
            setResult(RESULT_OK, data);
            finish();
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("bluetoothPermissionAsked", bluetoothPermissionAsked);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean blt_le = getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE);
        binding.textDebug.setText(blt_le ? R.string.app_name : R.string.error_no_blt_le);
        if (blt_le) {
            BluetoothManager bltMgr = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bltMgr.getAdapter().isEnabled()) {
                binding.textDebug.setText("Bluetooth in turned on");
            } else {
                binding.textDebug.setText("Please turn on bluetooth");
            }
        }
        boolean permissionReceived;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionReceived = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            permissionReceived |= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            permissionReceived = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
        bluetoothPermissionReceived.setValue(permissionReceived);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!bluetoothPermissionAsked) {
            askForBluetoothPermission();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scan_options, menu);

        // set menu items actions
        menu.findItem(R.id.action_scan)
                .setOnMenuItemClickListener(item -> {
                    if (!bluetoothPermissionAsked) {
                        askForBluetoothPermission();
                    } else {
                        if (startBleScan(getBaseContext())) {
                            invalidateOptionsMenu();
                            binding.list.postDelayed(() -> {
                                if (mScan != null) {
                                    // stop scanning
                                    mScan.stopScan(getBaseContext());
                                    invalidateOptionsMenu();
                                }
                            }, 10 * 1000);
                        } else {
                            Toast.makeText(ScanActivity.this, "Can't start scanning, check permissions!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true;
                });
        menu.findItem(R.id.action_scan_off)
                .setOnMenuItemClickListener(item -> {
                    if (mScan != null) {
                        // stop scanning
                        mScan.stopScan(getBaseContext());
                        invalidateOptionsMenu();
                    }
                    return true;
                });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isScanning = mScan != null && mScan.isScanning();
        menu.findItem(R.id.action_scan).setVisible(!isScanning);
        menu.findItem(R.id.action_scan_off).setVisible(isScanning);
        return super.onPrepareOptionsMenu(menu);
    }

    private void askForBluetoothPermission() {
        bluetoothPermissionAsked = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT});
        } else {
            promptEnableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
    }

    private boolean startBleScan(Context ctx) {
        if (mScan != null)
            mScan.stopScan(ctx);
        try {
            mScan = BleHelper.startScan2(ctx);
        } catch (Exception e) {
            Log.e(TAG, "start scan", e);
            return false;
        }
        mScan.getScanList().observe(this, bleEntries -> {
            var adapter = binding.list.getAdapter();
            if (adapter instanceof RecycleBleAdapter)
                ((RecycleBleAdapter) adapter).setItems(bleEntries);
        });
        return mScan.isScanning();
    }


}
